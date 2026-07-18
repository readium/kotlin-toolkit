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
 * A read-only [SeekableByteChannel] reading [innerChannel] ahead by chunks of [bufferSize] bytes,
 * to reduce the number of accesses to the underlying data source when reading small amounts of
 * data sequentially.
 */
internal class BufferedReadableChannel(
    private val innerChannel: SeekableByteChannel,
    bufferSize: Int,
) : SeekableByteChannel {

    private val dataBuffer: ZipBuffer =
        ZipBuffer.allocate(bufferSize)
            .apply { limit(0) }

    private val mutex: Mutex =
        Mutex()

    override fun close() {
        innerChannel.close()
    }

    override val isOpen: Boolean
        get() = innerChannel.isOpen

    override suspend fun read(buffer: ZipBuffer): Int {
        mutex.withLock {
            return readLocked(buffer)
        }
    }

    private suspend fun readLocked(buffer: ZipBuffer): Int {
        val sizeToRead = buffer.remaining()
        val sizeToReadFromBuffer = sizeToRead.coerceAtMost(dataBuffer.remaining())

        // Copies directly between the backing arrays, avoiding a temporary allocation and copy
        // on the hot read path.
        buffer.put(dataBuffer.array(), dataBuffer.position(), sizeToReadFromBuffer)
        dataBuffer.position(dataBuffer.position() + sizeToReadFromBuffer)

        if (sizeToReadFromBuffer == sizeToRead) {
            return sizeToReadFromBuffer
        }

        dataBuffer.clear()
        innerChannel.read(dataBuffer)
        dataBuffer.flip()

        if (!dataBuffer.hasRemaining()) {
            return sizeToReadFromBuffer
        }

        return sizeToReadFromBuffer + readLocked(buffer)
    }

    override suspend fun write(buffer: ZipBuffer): Int {
        throw NonWritableChannelException()
    }

    override suspend fun position(): Long {
        mutex.withLock {
            return innerChannel.position() - dataBuffer.remaining()
        }
    }

    override suspend fun position(newPosition: Long): BufferedReadableChannel {
        mutex.withLock {
            val innerPosition = innerChannel.position()
            if (newPosition in innerPosition - dataBuffer.limit() until innerPosition) {
                val newBufferPosition = (dataBuffer.limit() - (innerPosition - newPosition)).toInt()
                dataBuffer.position(newBufferPosition)
            } else {
                dataBuffer.limit(0)
                innerChannel.position(newPosition)
            }
            return this
        }
    }

    override suspend fun size(): Long {
        mutex.withLock {
            return innerChannel.size()
        }
    }

    override suspend fun truncate(size: Long): BufferedReadableChannel {
        throw NonWritableChannelException()
    }
}
