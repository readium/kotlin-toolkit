/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * A read-only [SeekableByteChannel] caching the tail of [innerChannel], starting at [cacheFrom].
 *
 * Zip reading accesses the central directory (located at the end of the archive) very often;
 * caching it avoids repeated random accesses to the underlying data source.
 */
internal class CachingReadableChannel(
    private val innerChannel: SeekableByteChannel,
    private val cacheFrom: Long = 0,
) : SeekableByteChannel {

    /**
     * Caches the bytes of [innerChannel] from [cacheFrom] to the end. Allocated and filled on
     * first use, as the channel size can only be queried from a suspending context.
     */
    private var tail: ZipBuffer? =
        null

    private val mutex: Mutex =
        Mutex()

    override fun close() {
        innerChannel.close()
    }

    override val isOpen: Boolean
        get() = innerChannel.isOpen

    suspend fun cache() {
        mutex.withLock {
            cacheTail()
        }
    }

    override suspend fun read(buffer: ZipBuffer): Int {
        mutex.withLock {
            val channelPosition = innerChannel.position()
            if (channelPosition in cacheFrom until innerChannel.size()) {
                val tail = tail ?: cacheTail()
                return readFromTail(tail, buffer, channelPosition - cacheFrom)
            }

            return innerChannel.read(buffer)
        }
    }

    private suspend fun readFromTail(tail: ZipBuffer, buffer: ZipBuffer, start: Long): Int {
        tail.position(start.toInt())
        val sizeToRead = buffer.remaining().coerceAtMost(tail.remaining())
        val temp = ByteArray(sizeToRead)
        tail.get(temp)
        buffer.put(temp)
        innerChannel.position(innerChannel.position() + sizeToRead)
        return sizeToRead
    }

    private suspend fun cacheTail(): ZipBuffer {
        val size = innerChannel.size()
        require(cacheFrom < size)
        val tail = this.tail
            ?: ZipBuffer.allocate((size - cacheFrom).toInt())
        tail.clear()
        innerChannel.position(cacheFrom)
        // Note: this leaves the inner channel position at the end of the data, not at
        // `cacheFrom + read bytes`. This mirrors the legacy implementation; it is harmless because
        // consumers (ZipFile) always seek before reading.
        innerChannel.read(tail)
        tail.flip()
        // Only mark the tail as cached after a successful fill, so that a failed read (e.g. a
        // network error on zip-over-HTTP) doesn't leave a garbage buffer served as cached data.
        this.tail = tail
        return tail
    }

    override suspend fun write(buffer: ZipBuffer): Int {
        throw NonWritableChannelException()
    }

    override suspend fun position(): Long {
        mutex.withLock {
            return innerChannel.position()
        }
    }

    override suspend fun position(newPosition: Long): CachingReadableChannel {
        mutex.withLock {
            innerChannel.position(newPosition)
            return this
        }
    }

    override suspend fun size(): Long {
        mutex.withLock {
            return innerChannel.size()
        }
    }

    override suspend fun truncate(size: Long): CachingReadableChannel {
        throw NonWritableChannelException()
    }
}
