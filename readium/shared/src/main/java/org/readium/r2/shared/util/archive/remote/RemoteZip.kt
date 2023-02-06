/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive.remote

import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.util.archive.Archive
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.remote.compress.archivers.zip.ZipArchiveEntry
import org.readium.r2.shared.util.archive.remote.compress.archivers.zip.ZipFile
import org.readium.r2.shared.util.archive.remote.jvm.SeekableByteChannel
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.io.CountingInputStream

internal class RemoteZip(private val archive: ZipFile) : Archive {

    private inner class Entry(private val entry: ZipArchiveEntry) : Archive.Entry {
        override val path: String get() = entry.name

        override val length: Long? get() = entry.size.takeUnless { it == -1L }

        override val compressedLength: Long?
            get() =
                if (entry.method == ZipArchiveEntry.STORED || entry.method == -1)
                    null
                else
                    entry.compressedSize.takeUnless { it == -1L }

        override suspend fun read(range: LongRange?): ByteArray =
            withContext(Dispatchers.IO) {
                if (range == null)
                    readFully()
                else
                    readRange(range)
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
         */
        private fun stream(fromIndex: Long): CountingInputStream {
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
            withContext(Dispatchers.IO) {
                stream?.close()
            }
        }
    }

    override suspend fun entries(): List<Archive.Entry> =
        archive.entries.toList().filterNot { it.isDirectory }.mapNotNull { Entry(it) }

    override suspend fun entry(path: String): Archive.Entry {
        val entry = archive.getEntry(path)
            ?: throw Exception("No file entry at path $path.")

        return Entry(entry)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        archive.close()
    }
}

class RemoteZipArchiveFactory(
    private val httpClient: HttpClient
) : ArchiveFactory {

    override suspend fun open(file: File, password: String?): Archive = withContext(Dispatchers.IO) {
        val channel = BufferedReadableChannel(FileChannelAdapter(file, "r"), DEFAULT_BUFFER_SIZE)
        RemoteZip(ZipFile(channel))
    }

    override suspend fun open(url: URL, password: String?): Archive = withContext(Dispatchers.IO) {
        var channel: SeekableByteChannel = HttpChannel(url.toString(), httpClient)
        val size = channel.size()
        channel = if (size < CACHE_ALL_MAX_SIZE) {
            CachingReadableChannel(channel, 0)
        } else {
            val cacheStart = size - CACHED_TAIL_SIZE
            val cachingChannel = CachingReadableChannel(channel, cacheStart)
            cachingChannel.cache()
            BufferedReadableChannel(cachingChannel, REMOTE_BUFFER_SIZE)
        }
        RemoteZip(ZipFile(channel))
    }

    companion object {

        private const val CACHE_ALL_MAX_SIZE = 5242880

        private const val CACHED_TAIL_SIZE = 65557

        private const val REMOTE_BUFFER_SIZE = 8192
    }
}
