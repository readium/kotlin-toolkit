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
 * Input stream reading through a [Blob].
 *
 * If you experience bad performances, consider wrapping the stream in a BufferedInputStream. This
 * is particularly useful when streaming deflated ZIP entries.
 */
public class BlobInputStream(
    private val blob: Blob,
    private val wrapError: (ReadError) -> IOException,
    private val range: LongRange? = null
) : InputStream() {

    private var isClosed = false

    private val end: Long by lazy {
        val resourceLength =
            runBlocking { blob.length() }
                .recover()

        if (range == null) {
            resourceLength
        } else {
            kotlin.math.min(resourceLength, range.last + 1)
        }
    }

    /** Current position in the resource. */
    private var position: Long = range?.start ?: 0

    /**
     * The currently marked position in the stream. Defaults to 0.
     */
    private var mark: Long = range?.start ?: 0

    private var error: ReadError? = null

    internal fun consumeError(): ReadError? {
        val errorNow = error
        error = null
        return errorNow
    }

    override fun available(): Int {
        checkNotClosed()
        return (end - position).toInt()
    }

    override fun skip(n: Long): Long = synchronized(this) {
        checkNotClosed()

        val newPosition = (position + n).coerceAtMost(end)
        val skipped = newPosition - position
        position = newPosition
        skipped
    }

    override fun read(): Int = synchronized(this) {
        checkNotClosed()

        if (available() <= 0) {
            return -1
        }

        val bytes = runBlocking {
            blob.read(position until (position + 1))
                .recover()
        }
        position += 1
        return bytes.first().toUByte().toInt()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int = synchronized(this) {
        checkNotClosed()

        if (available() <= 0) {
            return -1
        }

        val bytesToRead = len.coerceAtMost(available())
        val bytes = runBlocking {
            blob.read(position until (position + bytesToRead))
                .recover()
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

            isClosed = true
        }
    }

    private fun checkNotClosed() {
        if (isClosed) {
            throw IllegalStateException("InputStream is closed.")
        }
    }

    private fun<S> Try<S, ReadError>.recover(): S =
        when (this) {
            is Try.Success -> {
                value
            }
            is Try.Failure -> {
                error = value
                throw wrapError(value)
            }
        }
}
