/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import android.content.ContentResolver
import android.net.Uri
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.FilesystemError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.toUrl

/**
 * A [Resource] to access content [uri] thanks to a [ContentResolver].
 */
public class ContentBlob(
    private val uri: Uri,
    private val contentResolver: ContentResolver
) : Blob<ReadError> {

    private lateinit var _length: Try<Long, ReadError>

    override val source: AbsoluteUrl? = uri.toUrl() as? AbsoluteUrl

    override suspend fun close() {
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
        if (range == null) {
            return readFully()
        }

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceFirstNonNegative()
            .requireLengthFitInt()

        if (range.isEmpty()) {
            return Try.success(ByteArray(0))
        }

        return readRange(range)
    }

    private suspend fun readFully(): Try<ByteArray, ReadError> =
        withStream { it.readFully() }

    private suspend fun readRange(range: LongRange): Try<ByteArray, ReadError> =
        withStream {
            withContext(Dispatchers.IO) {
                val skipped = it.skip(range.first)
                check(skipped == range.first)
                val length = range.last - range.first + 1
                it.read(length)
            }
        }

    override suspend fun length(): Try<Long, ReadError> {
        if (!::_length.isInitialized) {
            _length = Try.catching {
                contentResolver.openFileDescriptor(uri, "r")
                    .use { fd -> checkNotNull(fd?.statSize.takeUnless { it == -1L }) }
            }
        }

        return _length
    }

    private suspend fun <T> withStream(block: suspend (InputStream) -> T): Try<T, ReadError> {
        return Try.catching {
            val stream = contentResolver.openInputStream(uri)
                ?: return Try.failure(
                    ReadError.Other(
                        Exception("Content provider recently crashed.")
                    )
                )
            val result = block(stream)
            stream.close()
            result
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
        "${javaClass.simpleName}(${runBlocking { length() } } bytes )"
}
