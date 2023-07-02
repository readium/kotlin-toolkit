/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive.channel

import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.*
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.error.getOrThrow
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.archive.channel.jvm.ClosedChannelException
import org.readium.r2.shared.util.archive.channel.jvm.NonWritableChannelException
import org.readium.r2.shared.util.archive.channel.jvm.SeekableByteChannel

class ResourceChannel(
    private val resource: Resource
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
        coroutineScope.launch { resource.close() }
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
                val size = resource.length()
                    .getOrElse { throw IOException("Server didn't provide content length.", it) }

                if (position >= size) {
                    return@withContext - 1
                }

                val available = size - position
                val toBeRead = dst.remaining().coerceAtMost(available.toInt())
                check(toBeRead > 0)
                val bytes = resource.read(position until position + toBeRead)
                    .mapFailure { IOException(it) }
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

        return runBlocking { resource.length() }
            .mapFailure { IOException(it) }
            .getOrThrow()
    }

    override fun truncate(size: Long): SeekableByteChannel {
        throw NonWritableChannelException()
    }
}
