/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import java.nio.ByteBuffer
import org.readium.r2.shared.util.zip.legacyjvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.legacyjvm.SeekableByteChannel

/**
 * Legacy copy of [BufferedReadableChannel] working with the blocking channels of
 * `:readium:readium-shared-zip-legacy`, kept while the vendored Java zip stack is still in use.
 *
 * Deleted with the legacy project at the end of phase 05c of the KMP migration.
 */
internal class LegacyBufferedReadableChannel(
    private val innerChannel: SeekableByteChannel,
    bufferSize: Int,
) : SeekableByteChannel {

    private val dataBuffer: ByteBuffer =
        ByteBuffer.allocate(bufferSize)
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

    override fun read(buffer: ByteBuffer): Int {
        synchronized(lock) {
            val sizeToRead = buffer.remaining()
            val sizeToReadFromBuffer = sizeToRead.coerceAtMost(dataBuffer.remaining())

            val temp = ByteArray(sizeToReadFromBuffer)
            dataBuffer.get(temp, 0, sizeToReadFromBuffer)
            buffer.put(temp)

            if (sizeToReadFromBuffer == sizeToRead) {
                return sizeToReadFromBuffer
            }

            dataBuffer.clear()
            innerChannel.read(dataBuffer)
            dataBuffer.flip()

            if (!dataBuffer.hasRemaining()) {
                return sizeToReadFromBuffer
            }

            return sizeToReadFromBuffer + read(buffer)
        }
    }

    override fun write(buffer: ByteBuffer): Int {
        throw NonWritableChannelException()
    }

    override fun position(): Long {
        synchronized(lock) {
            return innerChannel.position() - dataBuffer.remaining()
        }
    }

    override fun position(newPosition: Long): LegacyBufferedReadableChannel {
        synchronized(lock) {
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

    override fun size(): Long {
        synchronized(lock) {
            return innerChannel.size()
        }
    }

    override fun truncate(size: Long): LegacyBufferedReadableChannel {
        throw NonWritableChannelException()
    }
}
