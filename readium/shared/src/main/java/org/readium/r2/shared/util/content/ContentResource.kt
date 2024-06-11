/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.content

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType
import org.readium.r2.shared.util.toUrl

/**
 * A [Resource] to access content [uri] thanks to a [ContentResolver].
 *
 * @param uri the [Uri] to read.
 * @param contentResolver a ContentResolver.
 */
public class ContentResource(
    private val uri: Uri,
    private val contentResolver: ContentResolver
) : Resource {

    private lateinit var _length: Try<Long, ReadError>

    private lateinit var _properties: Try<Resource.Properties, ReadError>

    override val sourceUrl: AbsoluteUrl? = uri.toUrl() as? AbsoluteUrl

    override fun close() {
    }

    override suspend fun properties(): Try<Resource.Properties, ReadError> {
        if (::_properties.isInitialized) {
            return _properties
        }

        val filename =
            contentResolver.queryProjection(uri, MediaStore.MediaColumns.DISPLAY_NAME)

        val mediaType =
            contentResolver.getType(uri)
                ?.let { MediaType(it) }
                ?.takeUnless { it.matches(MediaType.BINARY) }

        val properties =
            Resource.Properties(
                Resource.Properties.Builder()
                    .also {
                        it.filename = filename
                        it.mediaType = mediaType
                    }
            )

        _properties = Try.success(properties)

        return _properties
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
                var skipped: Long = 0

                while (skipped != range.first) {
                    skipped += it.skip(range.first - skipped)
                    if (skipped == 0L) {
                        throw IOException("Could not skip InputStream to read ranges from $uri.")
                    }
                }

                val length = range.last - range.first + 1
                it.read(length)
            }
        }

    override suspend fun length(): Try<Long, ReadError> {
        if (!::_length.isInitialized) {
            _length = Try.catching {
                contentResolver.openFileDescriptor(uri, "r")
                    ?.use { fd -> fd.statSize.takeUnless { it == -1L } }
            }.flatMap {
                when (it) {
                    null -> Try.failure(
                        ReadError.UnsupportedOperation(
                            DebugError("Content provider does not provide length for uri $uri.")
                        )
                    )
                    else -> Try.success(it)
                }
            }
        }

        return _length
    }

    private suspend fun <T> withStream(block: suspend (InputStream) -> T): Try<T, ReadError> {
        return Try.catching {
            val stream = contentResolver.openInputStream(uri)
                ?: return Try.failure(
                    ReadError.Access(
                        ContentResolverError.NotAvailable()
                    )
                )
            stream.use { block(stream) }
        }
    }

    private inline fun <T> Try.Companion.catching(closure: () -> T): Try<T, ReadError> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(ReadError.Access(ContentResolverError.FileNotFound(e)))
        } catch (e: IOException) {
            failure(ReadError.Access(ContentResolverError.IO(e)))
        } catch (e: SecurityException) {
            failure(ReadError.Access(ContentResolverError.Forbidden(e)))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(ReadError.OutOfMemory(e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() } } bytes)"
}
