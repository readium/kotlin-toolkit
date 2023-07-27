/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

internal class FileChannelResource(
    override val url: Url?,
    private val file: File?,
    private val channel: FileChannel
) : Resource {

    private lateinit var _length: ResourceTry<Long>

    override suspend fun mediaType(): ResourceTry<MediaType?> =
        ResourceTry.success(null)

    override suspend fun name(): ResourceTry<String?> =
        ResourceTry.success(null)

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        ResourceTry.success(Resource.Properties())

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            tryOrLog { channel.close() }
        }
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        ResourceTry.catching {
            check(channel.isOpen)
            if (range == null) {
                return@catching readFullyThrowing()
            }

            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceFirstNonNegative()
                .requireLengthFitInt()

            if (range.isEmpty()) {
                return@catching ByteArray(0)
            }

            readRangeThrowing(range)
        }

    private suspend fun readFullyThrowing(): ByteArray =
        withContext(Dispatchers.IO) {
            channel.position(0)
            val stream = Channels.newInputStream(channel)
            stream.readFully()
        }

    private suspend fun readRangeThrowing(range: LongRange): ByteArray =
        withContext(Dispatchers.IO) {
            channel.position(range.first)

            // The stream must not be closed here because it would close the underlying
            // [FileChannel] too. Instead, [close] is responsible for that.
            val stream = Channels.newInputStream(channel)
            val length = range.last - range.first + 1
            stream.read(length)
        }

    override suspend fun length(): ResourceTry<Long> {
        if (!::_length.isInitialized) {
            _length = withContext(Dispatchers.IO) {
                try {
                    check(channel.isOpen)
                    Try.success(channel.size())
                } catch (e: IOException) {
                    Try.failure(Resource.Exception.Unavailable(e))
                }
            }
        }

        return _length
    }

    private inline fun <T> Try.Companion.catching(closure: () -> T): ResourceTry<T> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(Resource.Exception.NotFound(e))
        } catch (e: SecurityException) {
            failure(Resource.Exception.Forbidden(e))
        } catch (e: Exception) {
            failure(Resource.Exception.wrap(e))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(Resource.Exception.wrap(e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${channel.size()} bytes)"
}
