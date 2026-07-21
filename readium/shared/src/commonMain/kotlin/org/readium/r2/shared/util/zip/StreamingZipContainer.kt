/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.zip

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.findInstance
import org.readium.r2.shared.extensions.requireLengthFitInt
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
import org.readium.r2.shared.util.data.STREAM_CHUNK_SIZE
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.io.IoDispatcher
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipArchiveEntry
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipFile
import org.readium.r2.shared.util.zip.compress.utils.CountingInputStream
import org.readium.r2.shared.util.zip.compress.utils.ZipInputStream
import org.readium.r2.shared.util.zip.compress.utils.IOUtils

internal class StreamingZipContainer(
    private val zipFile: ZipFile,
    override val sourceUrl: AbsoluteUrl?,
    private val cacheEntryMaxSize: Int = 0,
) : Container<Resource> {

    private inner class Entry(
        private val url: Url,
        private val entry: ZipArchiveEntry,
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
            withContext(IoDispatcher) {
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
                        exception.findInstance<ReadException>()
                            ?.let { Try.failure(it.error) }
                            ?: Try.failure(ReadError.Decoding(exception))
                    }
                }
            }

        override suspend fun stream(
            range: LongRange?,
            consume: (ByteArray) -> Unit,
        ): Try<Unit, ReadError> =
            withContext(IoDispatcher) {
                // The mutex is container-wide because all entries share `zipFile`'s single
                // underlying channel. Holding it for a whole drain serialises the other entries of
                // the archive; that is accepted rather than risk interleaved reads on the channel.
                //
                // Consequently `consume` runs on IoDispatcher while the mutex is held, and must not
                // re-enter this container -- see the `Readable.stream` contract.
                mutex.withLock {
                    try {
                        streamLocked(range, consume)
                        Try.success(Unit)
                    } catch (exception: Exception) {
                        exception.findInstance<ReadException>()
                            ?.let { Try.failure(it.error) }
                            ?: Try.failure(ReadError.Decoding(exception))
                    }
                }
            }

        private suspend fun streamLocked(range: LongRange?, consume: (ByteArray) -> Unit) {
            cache?.let { cache ->
                consume(if (range == null) cache else cache.sliceRange(range))
                return
            }

            if (range == null) {
                checkNotNull(zipFile.getInputStream(entry)).use { input ->
                    input.drain(consume)
                }
                return
            }

            if (entry.size in 0 until cacheEntryMaxSize) {
                cache = readFully()
                consume(cache!!.sliceRange(range))
                return
            }

            // Reuses the forward-seek optimisation: a deflated entry cannot be seeked, so a cached
            // stream is kept as long as chunks are requested in order.
            inputStream(range.first).drainRange(range, consume)
        }

        private suspend fun readFully(): ByteArray =
            checkNotNull(zipFile.getInputStream(entry)).use {
                IOUtils.toByteArray(it)
            }

        private suspend fun readRange(range: LongRange): ByteArray =
            when {
                cache != null -> cache!!.sliceRange(range)

                entry.size in 0 until cacheEntryMaxSize -> {
                    cache = readFully()
                    readRange(range)
                }
                else ->
                    inputStream(range.first).readRange(range)
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
        private suspend fun inputStream(fromIndex: Long): CountingInputStream {
            if (entry.method == ZipArchiveEntry.STORED && fromIndex < entry.size) {
                return CountingInputStream(
                    checkNotNull(zipFile.getRawInputStream(entry, fromIndex)),
                    initialBytesRead = fromIndex
                )
            }

            // Reuse the current stream if it didn't exceed the requested index.
            stream
                ?.takeIf { it.bytesRead <= fromIndex }
                ?.let { return it }

            stream?.close()

            return CountingInputStream(checkNotNull(zipFile.getInputStream(entry)))
                .also { stream = it }
        }

        private var stream: CountingInputStream? = null

        override fun close() {
            tryOrLog {
                stream?.close()
            }
        }
    }

    private val mutex: Mutex =
        Mutex()

    override val entries: Set<Url> =
        zipFile.entries
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
        GlobalScope.launch(IoDispatcher) {
            tryOrLog { zipFile.close() }
        }
    }
}

/**
 * Reads the given [range] of bytes from the stream, assuming the stream was not read past the
 * start of the range yet.
 *
 * Replicates the `readRange` helper of the androidMain `util/io/CountingInputStream` on the
 * suspending zip stream.
 */
private suspend fun CountingInputStream.readRange(range: LongRange): ByteArray {
    @Suppress("NAME_SHADOWING")
    val range = range
        .coerceFirstNonNegative()
        .requireLengthFitInt()

    require(range.first >= bytesRead)

    if (range.isEmpty()) {
        return ByteArray(0)
    }

    if (!skipTo(range.first)) {
        // End reached, range.first was greater or equal to content length
        return ByteArray(0)
    }

    val length = (range.last - range.first + 1).toInt()
    val buffer = ByteArray(length)
    var read = 0

    while (read < length) {
        val count = read(buffer, read, length - read)
        if (count == -1) {
            break
        }
        read += count
    }

    return if (read == length) buffer else buffer.copyOf(read)
}

/** Returns the [range] slice of this array, clamped to its bounds. */
private fun ByteArray.sliceRange(range: LongRange): ByteArray {
    // If the entry is cached, its size fits into an Int.
    val start = range.first.coerceIn(0, size.toLong()).toInt()
    val end = (range.last + 1).coerceIn(start.toLong(), size.toLong()).toInt()
    return copyOfRange(start, end)
}

/** Drains the whole stream, emitting fixed-size chunks. */
private suspend fun ZipInputStream.drain(consume: (ByteArray) -> Unit) {
    val buffer = ByteArray(STREAM_CHUNK_SIZE)
    while (true) {
        val count = read(buffer, 0, buffer.size)
        if (count == -1) {
            return
        }
        if (count > 0) {
            consume(buffer.copyOf(count))
        }
    }
}

/**
 * Emits the given [range] of bytes in chunks, assuming the stream was not read past the start of
 * the range yet.
 */
private suspend fun CountingInputStream.drainRange(
    range: LongRange,
    consume: (ByteArray) -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val range = range
        .coerceFirstNonNegative()
        .requireLengthFitInt()

    require(range.first >= bytesRead)

    if (range.isEmpty()) {
        return
    }

    if (!skipTo(range.first)) {
        // End reached, range.first was greater or equal to content length.
        return
    }

    var remaining = (range.last - range.first + 1).toInt()
    val buffer = ByteArray(minOf(remaining, STREAM_CHUNK_SIZE))

    while (remaining > 0) {
        val count = read(buffer, 0, minOf(remaining, buffer.size))
        if (count == -1) {
            return
        }
        if (count > 0) {
            consume(buffer.copyOf(count))
            remaining -= count
        }
    }
}

/**
 * Skips forward until [offset], returning false if the end of the stream was reached first.
 */
private suspend fun CountingInputStream.skipTo(offset: Long): Boolean {
    val toSkip = offset - bytesRead
    var skipped = 0L

    while (skipped != toSkip) {
        val progress = skip(toSkip - skipped)
        skipped += progress
        if (progress == 0L) {
            // Guard against spinning forever when skip() stops making progress: fall back to
            // reading (and discarding) one byte, or bail out at the end of the stream.
            if (read() == -1) {
                return false
            }
            skipped += 1
        }
    }

    return true
}
