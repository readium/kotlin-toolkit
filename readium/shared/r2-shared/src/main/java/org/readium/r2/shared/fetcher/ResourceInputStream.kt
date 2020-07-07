/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream

/** Input stream reading a [Resource]'s content. */
class ResourceInputStream(
    private val resource: Resource
) : InputStream() {

    private val length: Long by lazy {
        try {
            runBlocking { resource.length().getOrThrow() }
        } catch (e: Exception) {
            throw IOException("Can't get resource length")
        }
    }

    /** Current position in the resource. */
    private var position: Long = 0

    /**
     * The currently marked position in the stream. Defaults to 0.
     */
    private var mark: Long = 0

    override fun available(): Int = (length - position).toInt()

    override fun skip(n: Long): Long = synchronized(this) {
        val newPosition = (position + n).coerceAtMost(length)
        val skipped = position - newPosition
        position = newPosition
        skipped
    }

    override fun read(): Int {
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
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (available() <= 0) {
            return -1
        }

        try {
            val bytes = runBlocking { resource.read(position until (position + len)).getOrThrow() }
            bytes.copyInto(
                destination = b,
                destinationOffset = off,
                startIndex = 0,
                endIndex = len.coerceAtMost(bytes.size)
            )
            position += bytes.size
            return bytes.size

        } catch (e: Exception) {
            throw IOException("Can't read ResourceInputStream", e)
        }
    }

    override fun markSupported(): Boolean = true

    override fun mark(readlimit: Int) = synchronized(this) {
        mark = position
    }

    override fun reset() = synchronized(this) {
        position = mark
    }

}
