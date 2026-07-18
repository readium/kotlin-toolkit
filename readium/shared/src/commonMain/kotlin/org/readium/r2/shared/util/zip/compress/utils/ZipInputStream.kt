/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.utils

import org.readium.r2.shared.util.Closeable

/**
 * The minimal suspending replacement for `java.io.InputStream` used by the ported zip stack.
 *
 * This is deliberately *not* a general-purpose stream hierarchy: it implements exactly the subset
 * of the `InputStream` contract consumed by the vendored Commons Compress read path
 * ([read], [skip], [close]), with suspending reads as mandated by the KMP plan's concurrency
 * ground rules (the underlying channels bridge to the suspending Readium `Readable`).
 *
 * Like the channel adapters (see phase 05a), streams are not thread-safe: callers must serialize
 * accesses externally.
 */
internal abstract class ZipInputStream : Closeable {

    private var singleByteBuffer: ByteArray? = null

    /**
     * Reads up to [len] bytes into [b] starting at [off].
     *
     * @return the number of bytes actually read, or -1 on end of stream.
     */
    abstract suspend fun read(b: ByteArray, off: Int, len: Int): Int

    /** Reads up to [b].size bytes into [b]. */
    suspend fun read(b: ByteArray): Int =
        read(b, 0, b.size)

    /**
     * Reads a single byte.
     *
     * @return the byte as an unsigned int, or -1 on end of stream.
     */
    open suspend fun read(): Int {
        val buffer = singleByteBuffer ?: ByteArray(1).also { singleByteBuffer = it }
        val read = read(buffer, 0, 1)
        return if (read < 1) -1 else buffer[0].toInt() and 0xff
    }

    /**
     * Skips up to [n] bytes.
     *
     * @return the number of bytes actually skipped.
     */
    open suspend fun skip(n: Long): Long {
        if (n <= 0) {
            return 0
        }
        val buffer = ByteArray(SKIP_BUFFER_SIZE)
        var remaining = n
        while (remaining > 0) {
            val read = read(buffer, 0, minOf(remaining, SKIP_BUFFER_SIZE.toLong()).toInt())
            if (read < 1) {
                break
            }
            remaining -= read
        }
        return n - remaining
    }

    override fun close() {
    }

    private companion object {
        private const val SKIP_BUFFER_SIZE = 4096
    }
}

/**
 * Stream that tracks the number of bytes read.
 *
 * Ported from `util/zip/compress/utils/CountingInputStream.java`, adapted to [ZipInputStream].
 */
internal open class CountingInputStream(
    private val input: ZipInputStream,
) : ZipInputStream() {

    /** The current number of bytes read from this stream. */
    var bytesRead: Long = 0
        private set

    protected fun count(read: Long) {
        if (read != -1L) {
            bytesRead += read
        }
    }

    override suspend fun read(): Int {
        val r = input.read()
        if (r >= 0) {
            count(1)
        }
        return r
    }

    override suspend fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) {
            return 0
        }
        val r = input.read(b, off, len)
        if (r >= 0) {
            count(r.toLong())
        }
        return r
    }

    override fun close() {
        input.close()
    }
}

/**
 * A minimal buffering wrapper replacing `java.io.BufferedInputStream` for the zip read path.
 */
internal class BufferedZipInputStream(
    private val input: ZipInputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
) : ZipInputStream() {

    init {
        require(bufferSize > 0) { "bufferSize must be positive" }
    }

    private val buffer: ByteArray = ByteArray(bufferSize)

    private var position: Int = 0

    private var end: Int = 0

    private suspend fun fill(): Int {
        position = 0
        end = 0
        val read = input.read(buffer, 0, buffer.size)
        if (read > 0) {
            end = read
        }
        return read
    }

    override suspend fun read(): Int {
        if (position >= end && fill() < 1) {
            return -1
        }
        return buffer[position++].toInt() and 0xff
    }

    override suspend fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) {
            return 0
        }
        if (position >= end) {
            // Large reads bypass the buffer once it is empty.
            if (len >= buffer.size) {
                return input.read(b, off, len)
            }
            if (fill() < 1) {
                return -1
            }
        }
        val toCopy = minOf(len, end - position)
        buffer.copyInto(b, off, position, position + toCopy)
        position += toCopy
        return toCopy
    }

    override fun close() {
        input.close()
    }

    private companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
