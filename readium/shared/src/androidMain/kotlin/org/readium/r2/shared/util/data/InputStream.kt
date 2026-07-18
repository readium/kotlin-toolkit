/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.util.Try

/**
 * Wraps a [Readable] into an [InputStream].
 *
 * Ownership of the [Readable] is transferred to the returned [InputStream].
 */
public fun Readable.asInputStream(
    range: LongRange? = null,
    wrapError: (ReadError) -> IOException = { ReadException(it) },
): InputStream =
    ReadableInputStreamAdapter(this, range, wrapError)

/**
 * Input stream reading through a [Readable] and taking ownership of it.
 *
 * If you experience bad performances, consider wrapping the stream in a BufferedInputStream. This
 * is particularly useful when streaming deflated ZIP entries.
 */
private class ReadableInputStreamAdapter(
    private val readable: Readable,
    private val range: LongRange? = null,
    private val wrapError: (ReadError) -> IOException = { ReadException(it) },
) : InputStream() {

    private var isClosed = false

    /**
     * End position (exclusive) of the readable content, or null when the length is unknown —
     * for example an HTTP response streamed with chunked transfer encoding.
     */
    private val end: Long? by lazy {
        val resourceLength =
            runBlocking { readable.length() }
                .getOrNull()

        when {
            range == null -> resourceLength
            resourceLength == null -> range.last + 1
            else -> kotlin.math.min(resourceLength, range.last + 1)
        }
    }

    /** Current position in the resource. */
    private var position: Long = range?.start ?: 0

    /**
     * The currently marked position in the stream. Defaults to 0.
     */
    private var mark: Long = range?.start ?: 0

    /** Number of bytes left until [end], or null when the length is unknown. */
    private fun remaining(): Long? =
        end?.let { (it - position).coerceAtLeast(0) }

    override fun available(): Int {
        checkNotClosed()
        // When the length is unknown, we cannot tell how many bytes are left, so we return 0 as
        // permitted by the InputStream contract.
        return (remaining() ?: 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    override fun skip(n: Long): Long = synchronized(this) {
        checkNotClosed()

        val skipped = remaining()?.let { kotlin.math.min(n, it) } ?: n
        position += skipped
        skipped
    }

    override fun read(): Int = synchronized(this) {
        checkNotClosed()

        if (remaining()?.let { it <= 0 } == true) {
            return -1
        }

        val bytes = runBlocking {
            readable.read(position until (position + 1))
                .recover()
        }
        if (bytes.isEmpty()) {
            return -1
        }
        position += 1
        return bytes.first().toUByte().toInt()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int = synchronized(this) {
        checkNotClosed()

        if (len == 0) {
            return 0
        }

        val remaining = remaining()
        if (remaining != null && remaining <= 0) {
            return -1
        }

        val bytesToRead = remaining?.let { len.toLong().coerceAtMost(it) } ?: len.toLong()
        val bytes = runBlocking {
            readable.read(position until (position + bytesToRead))
                .recover()
        }
        if (bytes.isEmpty()) {
            return -1
        }
        check(bytes.size <= bytesToRead)
        bytes.copyInto(
            destination = b,
            destinationOffset = off,
            startIndex = 0,
            endIndex = bytes.size
        )
        position += bytes.size
        return bytes.size
    }

    override fun markSupported(): Boolean = true

    override fun mark(readlimit: Int) {
        synchronized(this) {
            checkNotClosed()
            mark = position
        }
    }

    override fun reset() {
        synchronized(this) {
            checkNotClosed()
            position = mark
        }
    }

    /**
     * Closes the underlying resource.
     */
    override fun close() {
        synchronized(this) {
            if (isClosed) {
                return
            }

            runBlocking { readable.close() }

            isClosed = true
        }
    }

    private fun checkNotClosed() {
        if (isClosed) {
            throw IllegalStateException("InputStream is closed.")
        }
    }

    private fun <S> Try<S, ReadError>.recover(): S =
        when (this) {
            is Try.Success -> {
                value
            }
            is Try.Failure -> {
                throw wrapError(value)
            }
        }
}
