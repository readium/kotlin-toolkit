/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive.channel

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.ArchiveProperties
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.FailureResource
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.ZipContainer
import org.readium.r2.shared.resource.archive
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.channel.compress.archivers.zip.ZipArchiveEntry
import org.readium.r2.shared.util.archive.channel.compress.archivers.zip.ZipFile
import org.readium.r2.shared.util.archive.channel.jvm.SeekableByteChannel
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.mediatype.MediaType

internal class ChannelZipContainer(
    private val archive: ZipFile
) : ZipContainer {

    private inner class FailureEntry(
        override val path: String
    ) : ZipContainer.Entry, Resource by FailureResource(Resource.Exception.NotFound()) {

        override val compressedLength: Long? = null
    }

    private inner class Entry(private val entry: ZipArchiveEntry) : ZipContainer.Entry {

        override val path: String = entry.name.addPrefix("/")

        override val source: Url? get() = null

        override suspend fun properties(): ResourceTry<Resource.Properties> =
            ResourceTry.success(Resource.Properties {
                archive = ArchiveProperties(
                    entryLength = compressedLength
                        ?: length().getOrElse { return ResourceTry.failure(it) },
                    isEntryCompressed = compressedLength != null
                )
            })

        // FIXME: Implement with a sniffer.
        override suspend fun mediaType(): ResourceTry<MediaType?> =
            ResourceTry.success(null)

        override suspend fun length(): ResourceTry<Long> =
            entry.size.takeUnless { it == -1L }
                ?.let { Try.success(it) }
                ?: Try.failure(Resource.Exception.Other(UnsupportedOperationException()))

        override val compressedLength: Long?
            get() =
                if (entry.method == ZipArchiveEntry.STORED || entry.method == -1)
                    null
                else
                    entry.compressedSize.takeUnless { it == -1L }

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            withContext(Dispatchers.IO) {
                try {
                    val bytes =
                        if (range == null)
                            readFully()
                        else
                            readRange(range)
                    Try.success(bytes)
                } catch (e: Exception) {
                    Try.failure(Resource.Exception.wrap(e))
                }
            }

        private suspend fun readFully(): ByteArray =
            archive.getInputStream(entry).use {
                it.readFully()
            }

        private fun readRange(range: LongRange): ByteArray =
            stream(range.first).readRange(range)

        /**
         * Reading an entry in chunks (e.g. from the HTTP server) can be really slow if the entry
         * is deflated in the archive, because we can't jump to an arbitrary offset in a deflated
         * stream. This means that we need to read from the start of the entry for each chunk.
         *
         * To alleviate this issue, we cache a stream which will be reused as long as the chunks are
         * requested in order.
         *
         * See this issue for more info: https://github.com/readium/r2-shared-kotlin/issues/129
         *
         * In case of a stored entry, we create a new stream starting at the desired index in order
         * to prevent downloading of data until [fromIndex].
         *
         */
        private fun stream(fromIndex: Long): CountingInputStream {
            if (entry.method == ZipArchiveEntry.STORED && fromIndex < entry.size) {
                return CountingInputStream(archive.getRawInputStream(entry, fromIndex), fromIndex)
            }

            // Reuse the current stream if it didn't exceed the requested index.
            stream
                ?.takeIf { it.count <= fromIndex }
                ?.let { return it }

            stream?.close()

            return CountingInputStream(archive.getInputStream(entry))
                .also { stream = it }
        }

        private var stream: CountingInputStream? = null

        override suspend fun close() {
            tryOrLog {
                withContext(Dispatchers.IO) {
                    stream?.close()
                }
            }
        }
    }

    override suspend fun entries(): Set<Container.Entry> =
        archive.entries.toList()
            .filterNot { it.isDirectory }
            .mapNotNull { Entry(it) }
            .toSet()

    override fun get(path: String): Container.Entry =
        archive.getEntry(path.removePrefix("/"))
            ?.takeUnless { it.isDirectory }
            ?.let { Entry(it) }
            ?: FailureEntry(path)

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            tryOrLog { archive.close() }
        }
    }
}

/**
 * An [ArchiveFactory] able to open a ZIP archive served through an HTTP server.
 */
public class ChannelZipArchiveFactory : ArchiveFactory {

    override suspend fun create(
        resource: Resource,
        password: String?
    ): Try<Container, ArchiveFactory.Error> {
        if (password != null) {
            return Try.failure(ArchiveFactory.Error.PasswordsNotSupported())
        }

        return try {
            val resourceChannel = ResourceChannel(resource)
            val channel = wrapBaseChannel(resourceChannel)
            val zipFile = ZipFile(channel, true)
            val channelZip = ChannelZipContainer(zipFile)
            Try.success(channelZip)
        } catch (e: Resource.Exception) {
            Try.failure(ArchiveFactory.Error.ResourceReading(e))
        } catch (e: Exception) {
            Try.failure(ArchiveFactory.Error.FormatNotSupported(e))
        }
    }

    internal fun openFile(file: File): Container {
        val fileChannel = FileChannelAdapter(file, "r")
        val channel = wrapBaseChannel(fileChannel)
        return ChannelZipContainer(ZipFile(channel))
    }

    private fun wrapBaseChannel(channel: SeekableByteChannel): SeekableByteChannel {
        val size = channel.size()
        return if (size < CACHE_ALL_MAX_SIZE) {
            CachingReadableChannel(channel, 0)
        } else {
            val cacheStart = size - CACHED_TAIL_SIZE
            val cachingChannel = CachingReadableChannel(channel, cacheStart)
            cachingChannel.cache()
            BufferedReadableChannel(cachingChannel, DEFAULT_BUFFER_SIZE)
        }
    }

    public companion object {

        private const val CACHE_ALL_MAX_SIZE = 5242880

        private const val CACHED_TAIL_SIZE = 65557
    }
}
