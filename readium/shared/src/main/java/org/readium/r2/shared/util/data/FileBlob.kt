/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.Channels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.isLazyInitialized
import org.readium.r2.shared.util.toUrl

/**
 * A [Resource] to access a [file].
 */
public class FileBlob(
    private val file: File
) : Blob {

    private val randomAccessFile by lazy {
        try {
            Try.success(RandomAccessFile(file, "r"))
        } catch (e: FileNotFoundException) {
            Try.failure(e)
        }
    }

    override val source: AbsoluteUrl = file.toUrl()

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            if (::randomAccessFile.isLazyInitialized) {
                randomAccessFile.onSuccess {
                    tryOrLog { it.close() }
                }
            }
        }
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
        withContext(Dispatchers.IO) {
            Try.catching {
                readSync(range)
            }
        }

    private fun readSync(range: LongRange?): ByteArray {
        if (range == null) {
            return file.readBytes()
        }

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceFirstNonNegative()
            .requireLengthFitInt()

        if (range.isEmpty()) {
            return ByteArray(0)
        }

        return randomAccessFile.getOrThrow().run {
            channel.position(range.first)

            // The stream must not be closed here because it would close the underlying
            // [FileChannel] too. Instead, [close] is responsible for that.
            Channels.newInputStream(channel).run {
                val length = range.last - range.first + 1
                read(length)
            }
        }
    }

    override suspend fun length(): Try<Long, ReadError> =
        metadataLength?.let { Try.success(it) }
            ?: read().map { it.size.toLong() }

    private val metadataLength: Long? =
        tryOrNull {
            if (file.isFile) {
                file.length()
            } else {
                null
            }
        }

    private inline fun <T> Try.Companion.catching(closure: () -> T): Try<T, ReadError> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(ReadError.Filesystem(FilesystemError.NotFound(e)))
        } catch (e: SecurityException) {
            failure(ReadError.Filesystem(FilesystemError.Forbidden(e)))
        } catch (e: IOException) {
            failure(ReadError.Filesystem(FilesystemError.Unknown(e)))
        } catch (e: Exception) {
            failure(ReadError.Filesystem(FilesystemError.Unknown(e)))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(ReadError.OutOfMemory(e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${file.path})"
}
