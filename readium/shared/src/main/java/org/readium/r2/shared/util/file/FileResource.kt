/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.file

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.Channels
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.isLazyInitialized
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.toUrl

/**
 * A [Resource] to access a [File].
 *
 * @param file the file to read.
 */
public class FileResource(
    private val file: File,
) : Resource {

    private val randomAccessFile by lazy {
        try {
            Try.success(RandomAccessFile(file, "r"))
        } catch (e: FileNotFoundException) {
            Try.failure(e)
        }
    }

    private val properties =
        Resource.Properties(
            Resource.Properties.Builder()
                .also {
                    it.filename = file.name
                }
        )

    override val sourceUrl: AbsoluteUrl = file.toUrl()

    public override suspend fun properties(): Try<Resource.Properties, ReadError> {
        return Try.success(properties)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        if (::randomAccessFile.isLazyInitialized) {
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    randomAccessFile.onSuccess {
                        tryOrLog { it.close() }
                    }
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
        withContext(Dispatchers.IO) {
            metadataLength?.let { Try.success(it) }
                ?: Try.failure(
                    ReadError.UnsupportedOperation(
                        DebugError("Length not available for file at ${file.path}.")
                    )
                )
        }

    private val metadataLength: Long? by lazy {
        tryOrNull {
            if (file.isFile) {
                file.length()
            } else {
                null
            }
        }
    }

    private inline fun <T> Try.Companion.catching(closure: () -> T): Try<T, ReadError> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(ReadError.Access(FileSystemError.FileNotFound(e)))
        } catch (e: SecurityException) {
            failure(ReadError.Access(FileSystemError.Forbidden(e)))
        } catch (e: IOException) {
            failure(ReadError.Access(FileSystemError.IO(e)))
        } catch (e: Exception) {
            failure(ReadError.Access(FileSystemError.IO(e)))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(ReadError.OutOfMemory(e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${file.path})"
}
