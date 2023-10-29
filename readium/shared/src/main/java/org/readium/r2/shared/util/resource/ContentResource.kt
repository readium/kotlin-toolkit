/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

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
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toUri
import org.readium.r2.shared.util.toUrl

/**
 * Creates [ContentResource]s.
 */
public class ContentResourceFactory(
    private val contentResolver: ContentResolver
) : ResourceFactory {

    override suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType?
    ): Try<Resource, ResourceFactory.Error> {
        if (!url.isContent) {
            return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))
        }

        val resource = ContentResource(url.toUri(), contentResolver, mediaType)

        return Try.success(resource)
    }
}

/**
 * A [Resource] to access content [uri] thanks to a [ContentResolver].
 */
public class ContentResource internal constructor(
    private val uri: Uri,
    private val contentResolver: ContentResolver,
    private val mediaType: MediaType? = null
) : Resource {

    private lateinit var _length: ResourceTry<Long>

    override val source: AbsoluteUrl? = uri.toUrl() as? AbsoluteUrl

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        ResourceTry.success(Resource.Properties())

    override suspend fun mediaType(): ResourceTry<MediaType> =
        Try.success(
            mediaType
                ?: contentResolver.getType(uri)?.let { MediaType(it) }
                ?: MediaType.BINARY
        )

    override suspend fun close() {
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
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

    private suspend fun readFully(): ResourceTry<ByteArray> =
        withStream { it.readFully() }

    private suspend fun readRange(range: LongRange): ResourceTry<ByteArray> =
        withStream {
            withContext(Dispatchers.IO) {
                val skipped = it.skip(range.first)
                check(skipped == range.first)
                val length = range.last - range.first + 1
                it.read(length)
            }
        }

    override suspend fun length(): ResourceTry<Long> {
        if (!::_length.isInitialized) {
            _length = ResourceTry.catching {
                contentResolver.openFileDescriptor(uri, "r")
                    .use { fd -> checkNotNull(fd?.statSize.takeUnless { it == -1L }) }
            }
        }

        return _length
    }

    private suspend fun <T> withStream(block: suspend (InputStream) -> T): Try<T, ResourceError> {
        return ResourceTry.catching {
            val stream = contentResolver.openInputStream(uri)
                ?: return Try.failure(
                    ResourceError.Other(
                        Exception("Content provider recently crashed.")
                    )
                )
            val result = block(stream)
            stream.close()
            result
        }
    }

    private inline fun <T> Try.Companion.catching(closure: () -> T): ResourceTry<T> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(ResourceError.NotFound(e))
        } catch (e: SecurityException) {
            failure(ResourceError.Forbidden(e))
        } catch (e: IOException) {
            failure(ResourceError.Filesystem(e))
        } catch (e: Exception) {
            failure(ResourceError.Other(e))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(ResourceError.OutOfMemory(e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() } } bytes )"
}
