/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import kotlinx.coroutines.withContext
import okio.FileHandle
import org.readium.r2.shared.util.io.IoDispatcher
import org.readium.r2.shared.util.zip.jvm.ClosedChannelException
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * Adapts an Okio [FileHandle] to a read-only [SeekableByteChannel].
 *
 * Takes ownership of the given [fileHandle]: closing the channel closes it.
 *
 * This class is not thread-safe: callers must serialize accesses externally, as the wrapping
 * [CachingReadableChannel]/[BufferedReadableChannel] and the zip containers do.
 */
internal class FileChannelAdapter(
    private val fileHandle: FileHandle,
) : SeekableByteChannel {

    private var isClosed: Boolean =
        false

    private var position: Long =
        0

    override fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        fileHandle.close()
    }

    override val isOpen: Boolean
        get() = !isClosed

    override suspend fun read(buffer: ZipBuffer): Int {
        if (isClosed) {
            throw ClosedChannelException()
        }

        val sizeToRead = buffer.remaining()
        if (sizeToRead == 0) {
            return 0
        }

        return withContext(IoDispatcher) {
            // Reads directly into the array backing the buffer ([ZipBuffer.arrayOffset] is
            // always 0), avoiding a temporary allocation and copy on the hot read path.
            val read = fileHandle.read(position, buffer.array(), buffer.position(), sizeToRead)
            if (read == -1) {
                return@withContext -1
            }

            buffer.position(buffer.position() + read)
            position += read
            read
        }
    }

    override suspend fun write(buffer: ZipBuffer): Int {
        throw NonWritableChannelException()
    }

    override suspend fun position(): Long {
        return position
    }

    override suspend fun position(newPosition: Long): SeekableByteChannel {
        if (isClosed) {
            throw ClosedChannelException()
        }

        require(newPosition >= 0)
        position = newPosition
        return this
    }

    override suspend fun size(): Long {
        if (isClosed) {
            throw ClosedChannelException()
        }

        return withContext(IoDispatcher) {
            fileHandle.size()
        }
    }

    override suspend fun truncate(size: Long): SeekableByteChannel {
        throw NonWritableChannelException()
    }
}
