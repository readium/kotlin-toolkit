/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import org.readium.r2.shared.BuildConfig
import org.readium.r2.shared.resource.*
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.toUrl

class MediaTypeRetriever(
    resourceFactory: ResourceFactory = FileResourceFactory(),
    containerFactory: ContainerFactory = DirectoryContainerFactory(),
    archiveFactory: ArchiveFactory = DefaultArchiveFactory(),
    contentResolver: ContentResolver? = null,
    private val sniffers: List<Sniffer> = Sniffers.all,
) {
    private val urlSnifferContextFactory: UrlSnifferContextFactory =
        UrlSnifferContextFactory(resourceFactory, containerFactory, archiveFactory, contentResolver)

    private val bytesSnifferContextFactory: BytesSnifferContextFactory =
        BytesSnifferContextFactory(archiveFactory)

    suspend fun canonicalMediaType(mediaType: MediaType): MediaType =
        retrieve(mediaType = mediaType.toString()) ?: mediaType

    /**
     * Resolves a media type from a single file extension and media type hint, without checking the actual
     * content.
     */
    suspend fun retrieve(
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        if (BuildConfig.DEBUG && mediaType?.startsWith("/") == true) {
            throw IllegalArgumentException("The provided media type is incorrect: $mediaType. To pass a file path, you must wrap it in a File().")
        }
        return retrieve(mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a media type from file extension and media type hints without checking the actual
     * content.
     */
    suspend fun retrieve(
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): MediaType? {
        return doRetrieve(null, mediaTypes, fileExtensions)
    }

    /**
     * Resolves a media type from a local file.
     */
    suspend fun retrieve(
        file: File,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        return retrieve(file, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a media type from a local file.
     */
    suspend fun retrieve(
        file: File,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        return retrieve(content = Either.Right(file.toUrl()), mediaTypes = mediaTypes, fileExtensions = listOf(file.extension) + fileExtensions)
    }

    /**
     * Resolves a media type from bytes, e.g. from an HTTP response.
     */
    suspend fun retrieve(
        bytes: () -> ByteArray,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        return retrieve(bytes, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a media type from bytes, e.g. from an HTTP response.
     */
    suspend fun retrieve(
        bytes: () -> ByteArray,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        return retrieve(content = Either.Left(bytes), mediaTypes = mediaTypes, fileExtensions = fileExtensions)
    }

    /**
     * Resolves a media type from a Uri.
     */
    suspend fun retrieve(
        uri: Uri,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        return retrieve(uri, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a media type from a Uri.
     */
    suspend fun retrieve(
        uri: Uri,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        val url = uri.toUrl() ?: return null
        return retrieve(content = Either.Right(url), mediaTypes = mediaTypes, fileExtensions = fileExtensions)
    }

    /**
     * Resolves a media type from a sniffer context.
     *
     * Sniffing a media type is done in two rounds, because we want to give an opportunity to all
     * sniffers to return a [MediaType] quickly before inspecting the content itself:
     *  - Light Sniffing checks only the provided file extension or media type hints.
     *  - Heavy Sniffing reads the bytes to perform more advanced sniffing.
     */
    private suspend fun retrieve(
        content: Either<() -> ByteArray, Url>?,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): MediaType? {
        val fullContext = suspend {
            when (content) {
                is Either.Left ->
                    bytesSnifferContextFactory.createContext(
                        content.value.invoke(),
                        mediaTypes,
                        fileExtensions
                    )
                is Either.Right ->
                    urlSnifferContextFactory.createContext(
                        content.value,
                        mediaTypes,
                        fileExtensions
                    )
                null -> null
            }
        }

        return doRetrieve(fullContext, mediaTypes, fileExtensions)
    }

    /**
     * Resolves a media type from a sniffer context.
     *
     * Sniffing a media type is done in two rounds, because we want to give an opportunity to all
     * sniffers to return a [MediaType] quickly before inspecting the content itself:
     *  - Light Sniffing checks only the provided file extension or media type hints.
     *  - Heavy Sniffing reads the bytes to perform more advanced sniffing.
     */
    internal suspend fun doRetrieve(
        fullContext: (suspend () -> SnifferContext?)?,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): MediaType? {
        // Light sniffing with only media type hints
        if (mediaTypes.isNotEmpty()) {
            val context = HintSnifferContext(mediaTypes = mediaTypes)
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Light sniffing with both media type hints and file extensions
        if (fileExtensions.isNotEmpty()) {
            val context = HintSnifferContext(mediaTypes = mediaTypes, fileExtensions = fileExtensions)
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Heavy sniffing
        val context = fullContext?.invoke()

        if (context != null) {
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Falls back on the system-wide registered media types using [MimeTypeMap].
        // Note: This is done after the heavy sniffing of the provided [sniffers], because
        // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
        // their content (for example, for RWPM).
        val systemContext = context ?: HintSnifferContext(mediaTypes, fileExtensions)
        Sniffers.system(systemContext)?.let { return it }

        // If nothing else worked, we try to parse the first valid media type hint.
        for (mediaType in mediaTypes) {
            MediaType.parse(mediaType)?.let { return it }
        }

        return null
    }
}
