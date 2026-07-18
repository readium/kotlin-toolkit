/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import okio.IOException
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.zip.jvm.ClosedChannelException
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * Adapts a Readium [Readable] to a read-only [SeekableByteChannel].
 *
 * This is the seam making zip-over-HTTP work: the underlying [Readable] can serve ranged reads
 * from any source (file, HTTP server, content provider…).
 *
 * This class is not thread-safe: callers must serialize accesses externally, as the wrapping
 * [CachingReadableChannel]/[BufferedReadableChannel] and the zip containers do.
 */
internal class ReadableChannelAdapter(
    private val readable: Readable,
    private val wrapError: (ReadError) -> IOException,
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
        readable.close()
    }

    override val isOpen: Boolean
        get() = !isClosed

    override suspend fun read(buffer: ZipBuffer): Int {
        if (isClosed) {
            throw ClosedChannelException()
        }

        val size = readable.length()
            .mapFailure(wrapError)
            .getOrThrow()

        if (position >= size) {
            return -1
        }

        val available = size - position
        val toBeRead = buffer.remaining().coerceAtMost(available.toInt())
        check(toBeRead > 0)
        val bytes = readable.read(position until position + toBeRead)
            .mapFailure(wrapError)
            .getOrThrow()
        check(bytes.size == toBeRead)
        buffer.put(bytes, 0, toBeRead)
        position += toBeRead
        return toBeRead
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

        return readable.length()
            .mapFailure { wrapError(it) }
            .getOrThrow()
    }

    override suspend fun truncate(size: Long): SeekableByteChannel {
        throw NonWritableChannelException()
    }
}
