/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import org.readium.r2.shared.extensions.queryProjection
import java.io.File
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory

class AssetRetriever(
    private val protocols: (Url) -> Resource?,
    private val archiveFactory: ArchiveFactory
) {

    /**
     * Resolves a format from file extension and media type hints, without checking the actual
     * content.
     */
    suspend fun of(
        mediaTypes: List<String>,
        fileExtensions: List<String>,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return of(content = null, mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaType: String? = null,
        fileExtension: String? = null,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return ofFile(file, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return of(content = SnifferFileContent(file), mediaTypes = mediaTypes, fileExtensions = listOf(file.extension) + fileExtensions, sniffers = sniffers)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaType: String? = null,
        fileExtension: String? = null,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return ofFile(File(path), mediaType = mediaType, fileExtension = fileExtension, sniffers = sniffers)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return ofFile(File(path), mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
    }

    /**
     * Resolves a format from bytes, e.g. from an HTTP response.
     */
    suspend fun ofBytes(
        bytes: () -> ByteArray,
        mediaType: String? = null,
        fileExtension: String? = null,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return ofBytes(bytes, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
    }

    /**
     * Resolves a format from bytes, e.g. from an HTTP response.
     */
    suspend fun ofBytes(
        bytes: () -> ByteArray,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return of(content = SnifferBytesContent(bytes), mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
    }

    /**
     * Resolves a format from a content URI and a [ContentResolver].
     * Accepts the following URI schemes: content, android.resource, file.
     */
    suspend fun ofUri(
        uri: Uri,
        contentResolver: ContentResolver,
        mediaType: String? = null,
        fileExtension: String? = null,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        return ofUri(uri, contentResolver, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
    }

    /**
     * Resolves a format from a content URI and a [ContentResolver].
     * Accepts the following URI schemes: content, android.resource, file.
     */
    suspend fun ofUri(
        uri: Uri,
        contentResolver: ContentResolver,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
        sniffers: List<Sniffer> = MediaType.sniffers
    ): MediaType? {
        val allMediaTypes = mediaTypes.toMutableList()
        val allFileExtensions = fileExtensions.toMutableList()

        MimeTypeMap.getFileExtensionFromUrl(uri.toString()).ifEmpty { null }?.let {
            allFileExtensions.add(0, it)
        }

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.getType(uri)
                ?.takeUnless { MediaType.BINARY.matches(it) }
                ?.let { allMediaTypes.add(0, it) }

            contentResolver.queryProjection(uri, MediaStore.MediaColumns.DISPLAY_NAME)?.let { filename ->
                allFileExtensions.add(0, File(filename).extension)
            }
        }

        val content = SnifferUriContent(uri = uri, contentResolver = contentResolver)
        return of(content = content, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
    }

    /**
     * Resolves a media type from a sniffer context.
     *
     * Sniffing a media type is done in two rounds, because we want to give an opportunity to all
     * sniffers to return a [MediaType] quickly before inspecting the content itself:
     *  - Light Sniffing checks only the provided file extension or media type hints.
     *  - Heavy Sniffing reads the bytes to perform more advanced sniffing.
     */
    private suspend fun of(
        content: SnifferContent?,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
        sniffers: List<Sniffer>
    ): MediaType? {
        // Light sniffing with only media type hints
        if (mediaTypes.isNotEmpty()) {
            val context = SnifferContext(
                archiveFactory = archiveFactory,
                mediaTypes = mediaTypes
            )
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Light sniffing with both media type hints and file extensions
        if (fileExtensions.isNotEmpty()) {
            val context = SnifferContext(
                archiveFactory = archiveFactory,
                mediaTypes = mediaTypes,
                fileExtensions = fileExtensions
            )
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Heavy sniffing
        if (content != null) {
            val context = SnifferContext(
                archiveFactory = archiveFactory,
                content = content,
                mediaTypes = mediaTypes,
                fileExtensions = fileExtensions
            )
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
        val context = SnifferContext(
            archiveFactory = archiveFactory,
            content = content,
            mediaTypes = mediaTypes,
            fileExtensions = fileExtensions
        )
        Sniffers.system(context)?.let { return it }

        // If nothing else worked, we try to parse the first valid media type hint.
        for (mediaType in mediaTypes) {
            MediaType.parse(mediaType)?.let { return it }
        }

        return null
    }
}