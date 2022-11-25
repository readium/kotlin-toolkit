/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.runBlocking

/**
 * Input stream reading a [Resource]'s content.
 *
 * If you experience bad performances, consider wrapping the stream in a BufferedInputStream. This
 * is particularly useful when streaming deflated ZIP entries.
 */
class ResourceInputStream(
    private val resource: Resource,
    val range: LongRange? = null
) : InputStream() {

    private var isClosed = false

    private val end: Long by lazy {
        val resourceLength = try {
            runBlocking { resource.length().getOrThrow() }
        } catch (e: Exception) {
            throw IOException("Can't get resource length", e)
        }

        if (range == null)
            resourceLength
        else {
            kotlin.math.min(resourceLength, range.last + 1)
        }
    }

    /** Current position in the resource. */
    private var position: Long = range?.start ?: 0

    /**
     * The currently marked position in the stream. Defaults to 0.
     */
    private var mark: Long = range?.start ?: 0

    @Throws(IOException::class)
    override fun available(): Int {
        checkNotClosed()
        return (end - position).toInt()
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long = synchronized(this) {
        checkNotClosed()

        val newPosition = (position + n).coerceAtMost(end)
        val skipped = newPosition - position
        position = newPosition
        skipped
    }

    @Throws(IOException::class)
    override fun read(): Int = synchronized(this) {
        checkNotClosed()

        if (available() <= 0) {
            return -1
        }

        try {
            val bytes = runBlocking { resource.read(position until (position + 1)).getOrThrow() }
            position += 1
            return bytes.first().toInt()
        } catch (e: Exception) {
            throw IOException("Can't read ResourceInputStream", e)
        }
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int = synchronized(this) {
        checkNotClosed()

        if (available() <= 0) {
            return -1
        }

        try {
            val bytesToRead = len.coerceAtMost(available())
            val bytes = runBlocking { resource.read(position until (position + bytesToRead)).getOrThrow() }
            check(bytes.size <= bytesToRead)
            bytes.copyInto(
                destination = b,
                destinationOffset = off,
                startIndex = 0,
                endIndex = bytes.size
            )
            position += bytes.size
            return bytes.size
        } catch (e: Exception) {
            throw IOException("Can't read ResourceInputStream", e)
        }
    }

    override fun markSupported(): Boolean = true

    @Throws(IOException::class)
    override fun mark(readlimit: Int) = synchronized(this) {
        checkNotClosed()
        mark = position
    }

    @Throws(IOException::class)
    override fun reset() = synchronized(this) {
        checkNotClosed()
        position = mark
    }

    /**
     * Closes the underlying resource.
     */
    override fun close() = synchronized(this) {
        if (isClosed)
            return

        isClosed = true
        runBlocking { resource.close() }
    }

    private fun checkNotClosed() {
        if (isClosed)
            throw IOException("InputStream is closed.")
    }
}
