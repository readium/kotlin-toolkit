/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import java.nio.ByteBuffer
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel

internal class CachingReadableChannel(
    private val innerChannel: SeekableByteChannel,
    private val cacheFrom: Long = 0,
) : SeekableByteChannel {

    init {
        require(cacheFrom < innerChannel.size())
    }

    private val tail: ByteBuffer =
        ByteBuffer.allocate((innerChannel.size() - cacheFrom).toInt())
            .apply { limit(0) }

    private val lock: Any =
        Any()

    override fun close() {
        synchronized(lock) {
            innerChannel.close()
        }
    }

    override fun isOpen(): Boolean {
        synchronized(lock) {
            return innerChannel.isOpen
        }
    }

    fun cache() {
        synchronized(lock) {
            cacheTail()
        }
    }

    override fun read(buffer: ByteBuffer): Int {
        synchronized(lock) {
            val channelPosition = innerChannel.position()
            if (channelPosition in cacheFrom until innerChannel.size()) {
                if (tail.limit() == 0) {
                    cacheTail()
                }

                return readFromTail(buffer, channelPosition - cacheFrom)
            }

            return innerChannel.read(buffer)
        }
    }

    private fun readFromTail(buffer: ByteBuffer, start: Long): Int {
        tail.position(start.toInt())
        val sizeToRead = buffer.remaining().coerceAtMost(tail.remaining())
        val temp = ByteArray(sizeToRead)
        tail.get(temp)
        buffer.put(temp)
        innerChannel.position(innerChannel.position() + sizeToRead)
        return sizeToRead
    }

    private fun cacheTail() {
        tail.clear()
        innerChannel.position(cacheFrom)
        innerChannel.read(tail)
        tail.flip()
    }

    override fun write(buffer: ByteBuffer): Int {
        throw NonWritableChannelException()
    }

    override fun position(): Long {
        synchronized(lock) {
            return innerChannel.position()
        }
    }

    override fun position(newPosition: Long): CachingReadableChannel {
        synchronized(lock) {
            innerChannel.position(newPosition)
            return this
        }
    }

    override fun size(): Long {
        synchronized(lock) {
            return innerChannel.size()
        }
    }

    override fun truncate(size: Long): CachingReadableChannel {
        throw NonWritableChannelException()
    }
}
