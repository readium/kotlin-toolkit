/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.zip.jvm.ClosedChannelException
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel

internal class ReadableChannelAdapter(
    private val readable: Readable,
    private val wrapError: (ReadError) -> IOException,
) : SeekableByteChannel {

    private val coroutineScope: CoroutineScope =
        MainScope()

    private var isClosed: Boolean =
        false

    private var position: Long =
        0

    override fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        coroutineScope.launch { readable.close() }
    }

    override fun isOpen(): Boolean {
        return !isClosed
    }

    override fun read(dst: ByteBuffer): Int {
        return runBlocking {
            if (isClosed) {
                throw ClosedChannelException()
            }

            withContext(Dispatchers.IO) {
                val size = readable.length()
                    .mapFailure(wrapError)
                    .getOrThrow()

                if (position >= size) {
                    return@withContext -1
                }

                val available = size - position
                val toBeRead = dst.remaining().coerceAtMost(available.toInt())
                check(toBeRead > 0)
                val bytes = readable.read(position until position + toBeRead)
                    .mapFailure(wrapError)
                    .getOrThrow()
                check(bytes.size == toBeRead)
                dst.put(bytes, 0, toBeRead)
                position += toBeRead
                return@withContext toBeRead
            }
        }
    }

    override fun write(buffer: ByteBuffer): Int {
        throw NonWritableChannelException()
    }

    override fun position(): Long {
        return position
    }

    override fun position(newPosition: Long): SeekableByteChannel {
        if (isClosed) {
            throw ClosedChannelException()
        }

        position = newPosition
        return this
    }

    override fun size(): Long {
        if (isClosed) {
            throw ClosedChannelException()
        }

        return runBlocking { readable.length() }
            .mapFailure { wrapError(it) }
            .getOrThrow()
    }

    override fun truncate(size: Long): SeekableByteChannel {
        throw NonWritableChannelException()
    }
}
