/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.datasource

import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.getOrThrow

/**
 * Input stream reading through a [DataSource].
 *
 * If you experience bad performances, consider wrapping the stream in a BufferedInputStream. This
 * is particularly useful when streaming deflated ZIP entries.
 */
internal class DataSourceInputStream<E: Error>(
    private val dataSource: DataSource<E>,
    private val wrapError: (E) -> IOException,
    private val range: LongRange? = null
) : InputStream() {

    private var isClosed = false

    private val end: Long by lazy {
        val resourceLength =
            runBlocking { dataSource.length() }
                .mapFailure { wrapError(it) }
                .getOrThrow()

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

        val bytes = runBlocking { dataSource.read(position until (position + 1))
            .mapFailure { wrapError(it) }
            .getOrThrow() }
        position += 1
        return bytes.first().toUByte().toInt()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int = synchronized(this) {
        checkNotClosed()

        if (available() <= 0) {
            return -1
        }

        val bytesToRead = len.coerceAtMost(available())
        val bytes = runBlocking { dataSource.read(position until (position + bytesToRead))
            .mapFailure { wrapError(it) }
            .getOrThrow() }
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
}
