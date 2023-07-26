/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.toUri

/**
 * Creates [ContentResource]s.
 */
public class ContentResourceFactory(
    private val contentResolver: ContentResolver
) : ResourceFactory {

    override suspend fun create(url: Url): Try<Resource, ResourceFactory.Error> {
        if (url.scheme != ContentResolver.SCHEME_CONTENT) {
            return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))
        }

        val resource = ContentResource(url.toUri(), contentResolver)

        return Try.success(resource)
    }
}

/**
 * A [Resource] to access content [uri] thanks to a [ContentResolver].
 */
public class ContentResource(
    private val uri: Uri,
    private val contentResolver: ContentResolver,
) : Resource {

    private lateinit var _length: ResourceTry<Long>

    override suspend fun name(): ResourceTry<String?> {
        val cursor = contentResolver
            .query(uri, null, null, null, null)
            ?: return ResourceTry.failure(Resource.Exception.NotFound())

        @Suppress("Name_shadowing")
        cursor.use { cursor ->
            if (!cursor.moveToFirst()) {
                return ResourceTry.failure(Resource.Exception.NotFound())
            }
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                .takeUnless { it == -1 }
                ?: return Try.success(uri.lastPathSegment)

            tryOrNull { cursor.getString(columnIndex) }
                ?.let { return Try.success(it) }
                ?: return Try.success(uri.lastPathSegment)
        }
    }

    override suspend fun mediaType(): ResourceTry<String?> =
        Try.success(contentResolver.getType(uri))

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

    private suspend fun <T> withStream(block: suspend (InputStream) -> T): Try<T, Resource.Exception> =
        ResourceTry.catching {
            val stream = contentResolver.openInputStream(uri)
                ?: throw Resource.Exception.Unavailable(
                    Exception("Content provider recently crashed.")
                )
            val result = block(stream)
            stream.close()
            result
        }

    private inline fun <T> Try.Companion.catching(closure: () -> T): ResourceTry<T> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(Resource.Exception.NotFound(e))
        } catch (e: SecurityException) {
            failure(Resource.Exception.Forbidden(e))
        } catch (e: IOException) {
            failure(Resource.Exception.Unavailable(e))
        } catch (e: Exception) {
            failure(Resource.Exception.wrap(e))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(Resource.Exception.wrap(e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() } } bytes )"
}
