/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.zip

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.findInstance
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveProperties
import org.readium.r2.shared.util.archive.archive
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.data.ReadTry
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipArchiveEntry
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipFile

internal class StreamingZipContainer(
    private val zipFile: ZipFile,
    override val sourceUrl: AbsoluteUrl?,
    private val cacheEntryMaxSize: Int = 0
) : Container<Resource> {

    private inner class Entry(
        private val url: Url,
        private val entry: ZipArchiveEntry
    ) : Resource {

        private var cache: ByteArray? =
            null

        override val sourceUrl: AbsoluteUrl? get() = null

        override suspend fun properties(): ReadTry<Resource.Properties> =
            Try.success(
                Resource.Properties {
                    filename = url.filename
                    archive = ArchiveProperties(
                        entryLength = compressedLength
                            ?: length().getOrElse { return Try.failure(it) },
                        isEntryCompressed = compressedLength != null
                    )
                }
            )

        override suspend fun length(): ReadTry<Long> =
            entry.size.takeUnless { it == -1L }
                ?.let { Try.success(it) }
                ?: Try.failure(
                    ReadError.UnsupportedOperation(
                        DebugError("ZIP entry doesn't provide length for entry $url.")
                    )
                )

        private val compressedLength: Long?
            get() =
                if (entry.method == ZipArchiveEntry.STORED || entry.method == -1) {
                    null
                } else {
                    entry.compressedSize.takeUnless { it == -1L }
                }

        override suspend fun read(range: LongRange?): ReadTry<ByteArray> =
            withContext(Dispatchers.IO) {
                mutex.withLock {
                    try {
                        val bytes =
                            if (range == null) {
                                readFully()
                            } else {
                                readRange(range)
                            }
                        Try.success(bytes)
                    } catch (exception: Exception) {
                        exception.findInstance(ReadException::class.java)
                            ?.let { Try.failure(it.error) }
                            ?: Try.failure(ReadError.Decoding(exception))
                    }
                }
            }

        private suspend fun readFully(): ByteArray =
            zipFile.getInputStream(entry).use {
                it.readFully()
            }

        private suspend fun readRange(range: LongRange): ByteArray =
            when {
                cache != null -> {
                    // If the entry is cached, its size fit into an Int.
                    val rangeSize = (range.last - range.first + 1).toInt()
                    cache!!.copyInto(
                        ByteArray(rangeSize),
                        startIndex = range.first.toInt(),
                        endIndex = range.last.toInt() + 1
                    )
                }

                entry.size in 0 until cacheEntryMaxSize -> {
                    cache = readFully()
                    readRange(range)
                }
                else ->
                    stream(range.first).readRange(range)
            }

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
                return CountingInputStream(zipFile.getRawInputStream(entry, fromIndex), fromIndex)
            }

            // Reuse the current stream if it didn't exceed the requested index.
            stream
                ?.takeIf { it.count <= fromIndex }
                ?.let { return it }

            stream?.close()

            return CountingInputStream(zipFile.getInputStream(entry))
                .also { stream = it }
        }

        private var stream: CountingInputStream? = null

        @OptIn(DelicateCoroutinesApi::class)
        override fun close() {
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    tryOrLog {
                        stream?.close()
                    }
                }
            }
        }
    }

    private val mutex: Mutex =
        Mutex()

    override val entries: Set<Url> =
        zipFile.entries.toList()
            .filterNot { it.isDirectory }
            .mapNotNull { entry -> Url.fromDecodedPath(entry.name) }
            .toSet()

    override fun get(url: Url): Resource? =
        (url as? RelativeUrl)?.path
            ?.let { zipFile.getEntry(it) }
            ?.takeUnless { it.isDirectory }
            ?.let { Entry(url, it) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                tryOrLog { zipFile.close() }
            }
        }
    }
}
