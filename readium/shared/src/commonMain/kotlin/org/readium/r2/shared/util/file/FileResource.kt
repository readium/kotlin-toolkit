/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.file

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileHandle
import okio.FileNotFoundException
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.OutOfMemoryError
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.STREAM_CHUNK_SIZE
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.io.IoDispatcher
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename

/**
 * A [Resource] to access a file stored on the file system.
 *
 * @param url the `file://` URL of the file to read.
 */
public class FileResource(
    url: AbsoluteUrl,
) : Resource {

    init {
        require(url.isFile) { "FileResource requires a file:// URL, was: $url" }
    }

    override val sourceUrl: AbsoluteUrl = url

    private val path: Path = checkNotNull(url.path).toPath()

    private val lazyHandle: Lazy<Try<FileHandle, Exception>> = lazy {
        try {
            Try.success(fileSystem.openReadOnly(path))
        } catch (e: FileNotFoundException) {
            Try.failure(e)
        } catch (e: IOException) {
            Try.failure(e)
        }
    }

    private val handle: Try<FileHandle, Exception> by lazyHandle

    private val properties =
        Resource.Properties(
            Resource.Properties.Builder()
                .also {
                    it.filename = sourceUrl.filename
                }
        )

    public override suspend fun properties(): Try<Resource.Properties, ReadError> {
        return Try.success(properties)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        if (lazyHandle.isInitialized()) {
            GlobalScope.launch(IoDispatcher) {
                handle.onSuccess {
                    tryOrLog { it.close() }
                }
            }
        }
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
        withContext(IoDispatcher) {
            Try.catching {
                readSync(range)
            }
        }

    override suspend fun stream(
        range: LongRange?,
        consume: (ByteArray) -> Unit,
    ): Try<Unit, ReadError> =
        withContext(IoDispatcher) {
            Try.catching {
                streamSync(range, consume)
            }
        }

    private fun streamSync(range: LongRange?, consume: (ByteArray) -> Unit) {
        val handle = handle.getOrThrow()

        @Suppress("NAME_SHADOWING")
        val range = (range ?: 0 until Long.MAX_VALUE)
            .coerceFirstNonNegative()

        var offset = range.first
        while (offset <= range.last) {
            val wanted = minOf(
                STREAM_CHUNK_SIZE.toLong(),
                range.last - offset + 1
            ).toInt()

            val buffer = ByteArray(wanted)
            var read = 0
            while (read < wanted) {
                val count = handle.read(
                    fileOffset = offset + read,
                    array = buffer,
                    arrayOffset = read,
                    byteCount = wanted - read
                )
                if (count == -1) {
                    break
                }
                read += count
            }

            if (read == 0) {
                return
            }
            consume(if (read == wanted) buffer else buffer.copyOf(read))
            offset += read
        }
    }

    private fun readSync(range: LongRange?): ByteArray {
        if (range == null) {
            return handle.getOrThrow().source(fileOffset = 0)
                .buffer()
                .use { it.readByteArray() }
        }

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceFirstNonNegative()
            .requireLengthFitInt()

        if (range.isEmpty()) {
            return ByteArray(0)
        }

        val length = (range.last - range.first + 1).toInt()
        val bytes = ByteArray(length)
        var read = 0

        val handle = handle.getOrThrow()
        while (read < length) {
            val count = handle.read(
                fileOffset = range.first + read,
                array = bytes,
                arrayOffset = read,
                byteCount = length - read
            )
            if (count == -1) {
                break
            }
            read += count
        }

        return if (read == length) bytes else bytes.copyOf(read)
    }

    override suspend fun length(): Try<Long, ReadError> =
        withContext(IoDispatcher) {
            metadataLength?.let { Try.success(it) }
                ?: Try.failure(
                    ReadError.UnsupportedOperation(
                        DebugError("Length not available for file at $path.")
                    )
                )
        }

    private val metadataLength: Long? by lazy {
        tryOrNull {
            fileSystem.metadataOrNull(path)
                ?.takeIf { it.isRegularFile }
                ?.size
        }
    }

    private inline fun <T> Try.Companion.catching(closure: () -> T): Try<T, ReadError> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(ReadError.Access(FileSystemError.FileNotFound(e)))
        } catch (e: IOException) {
            failure(ReadError.Access(FileSystemError.IO(e)))
        } catch (e: Exception) {
            failure(ReadError.Access(FileSystemError.IO(e)))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(ReadError.OutOfMemory(e))
        }

    override fun toString(): String =
        "${this::class.simpleName}($path)"
}
