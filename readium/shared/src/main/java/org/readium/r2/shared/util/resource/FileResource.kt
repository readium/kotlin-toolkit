/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.channels.Channels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.isLazyInitialized
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.toUrl

/**
 * A [Resource] to access a [file].
 */
public class FileResource private constructor(
    private val file: File,
    private val mediaType: MediaType?,
    private val mediaTypeRetriever: MediaTypeRetriever?
) : Resource {

    public constructor(file: File, mediaType: MediaType) : this(file, mediaType, null)

    public constructor(file: File, mediaTypeRetriever: MediaTypeRetriever) : this(
        file,
        null,
        mediaTypeRetriever
    )

    private val randomAccessFile by lazy {
        ResourceTry.catching {
            RandomAccessFile(file, "r")
        }
    }

    override val source: AbsoluteUrl = file.toUrl()

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        ResourceTry.success(Resource.Properties())

    override suspend fun mediaType(): ResourceTry<MediaType> = Try.success(
        mediaType
            ?: mediaTypeRetriever?.retrieve(
                hints = MediaTypeHints(fileExtension = file.extension),
                content = ResourceMediaTypeSnifferContent(this)
            )
            ?: MediaType.BINARY
    )

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            if (::randomAccessFile.isLazyInitialized) {
                randomAccessFile.onSuccess {
                    tryOrLog { it.close() }
                }
            }
        }
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        withContext(Dispatchers.IO) {
            ResourceTry.catching {
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

    override suspend fun length(): ResourceTry<Long> =
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

    private inline fun <T> Try.Companion.catching(closure: () -> T): ResourceTry<T> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(Resource.Exception.NotFound(file.toUrl(), e))
        } catch (e: SecurityException) {
            failure(Resource.Exception.Forbidden(file.toUrl(), e))
        } catch (e: Exception) {
            failure(Resource.Exception.wrap(file.toUrl(), e))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(Resource.Exception.wrap(file.toUrl(), e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${file.path})"
}

public class FileResourceFactory(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ResourceFactory {

    override suspend fun create(url: AbsoluteUrl): Try<Resource, ResourceFactory.Error> {
        val file = url.toFile()
            ?: return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))

        try {
            if (!file.isFile) {
                return Try.failure(ResourceFactory.Error.NotAResource(url))
            }
        } catch (e: Exception) {
            return Try.failure(ResourceFactory.Error.Forbidden(e))
        }

        return Try.success(FileResource(file, mediaTypeRetriever))
    }
}
