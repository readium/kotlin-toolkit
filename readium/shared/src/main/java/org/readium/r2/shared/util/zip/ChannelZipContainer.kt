/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.ArchiveProperties
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.FailureResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceMediaTypeSnifferContent
import org.readium.r2.shared.util.resource.ResourceTry
import org.readium.r2.shared.util.resource.archive
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipArchiveEntry
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipFile

internal class ChannelZipContainer(
    private val archive: ZipFile,
    override val source: AbsoluteUrl?,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Container {

    private inner class FailureEntry(
        override val url: Url
    ) : Container.Entry, Resource by FailureResource(Resource.Exception.NotFound())

    private inner class Entry(
        override val url: Url,
        private val entry: ZipArchiveEntry
    ) : Container.Entry {

        override val source: AbsoluteUrl? get() = null

        override suspend fun properties(): ResourceTry<Resource.Properties> =
            ResourceTry.success(
                Resource.Properties {
                    archive = ArchiveProperties(
                        entryLength = compressedLength
                            ?: length().getOrElse { return ResourceTry.failure(it) },
                        isEntryCompressed = compressedLength != null
                    )
                }
            )

        override suspend fun mediaType(): ResourceTry<MediaType> =
            Try.success(
                mediaTypeRetriever.retrieve(
                    hints = MediaTypeHints(fileExtension = url.extension),
                    content = ResourceMediaTypeSnifferContent(this)
                ) ?: MediaType.BINARY
            )

        override suspend fun length(): ResourceTry<Long> =
            entry.size.takeUnless { it == -1L }
                ?.let { Try.success(it) }
                ?: Try.failure(Resource.Exception.Other(UnsupportedOperationException()))

        private val compressedLength: Long?
            get() =
                if (entry.method == ZipArchiveEntry.STORED || entry.method == -1) {
                    null
                } else {
                    entry.compressedSize.takeUnless { it == -1L }
                }

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            withContext(Dispatchers.IO) {
                try {
                    val bytes =
                        if (range == null) {
                            readFully()
                        } else {
                            readRange(range)
                        }
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
            .mapNotNull { entry ->
                Url.fromDecodedPath(entry.name)
                    ?.let { url -> Entry(url, entry) }
            }
            .toSet()

    override fun get(url: Url): Container.Entry =
        (url as? RelativeUrl)?.path
            ?.let { archive.getEntry(it) }
            ?.takeUnless { it.isDirectory }
            ?.let { Entry(url, it) }
            ?: FailureEntry(url)

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            tryOrLog { archive.close() }
        }
    }
}
