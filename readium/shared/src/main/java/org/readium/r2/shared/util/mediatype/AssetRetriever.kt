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
import java.io.File
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.toUrl

class AssetRetriever(
    protocols: List<Protocol>,
    archiveFactory: ArchiveFactory,
    private val sniffers: List<Sniffer> = MediaType.sniffers
) {

    private val snifferContextFactory: SnifferContextFactory =
        SnifferContextFactory(protocols, archiveFactory)

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): AssetDescription? {
        return ofFile(file, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): AssetDescription? {
        return ofFile(File(path), mediaType = mediaType, fileExtension = fileExtension)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): AssetDescription? {
        return ofFile(File(path), mediaTypes = mediaTypes, fileExtensions = fileExtensions)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): AssetDescription? {
        val context = snifferContextFactory
            .createContext(file.toUrl(), mediaTypes, fileExtensions)
            ?: return null

        return ofClosing(context)
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
    ): AssetDescription? {
        return ofUri(uri, contentResolver, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
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
    ): AssetDescription? {
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

        val url = uri.toUrl()
            ?: return null

        val context = snifferContextFactory
            .createContext(url, allMediaTypes, allFileExtensions)
            ?: return null

        return ofClosing(context)
    }

    suspend fun ofUrl(
        url: Url,
        mediaType: String? = null,
        fileExtension: String? = null
    ): AssetDescription? {
        return ofUrl(url, listOfNotNull(mediaType), listOfNotNull(fileExtension))
    }

    private suspend fun ofUrl(
        url: Url,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): AssetDescription? {
        val context = snifferContextFactory
            .createContext(url, mediaTypes, fileExtensions)
            ?: return null

        return ofClosing(context)
    }

    private suspend fun ofClosing(
        context: SnifferContext,
    ): AssetDescription? =
        try {
            of(context)
        } finally {
            context.release()
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
        context: SnifferContext,
    ): AssetDescription? {

        val type = when {
            (context is PackageSnifferContext) ->
                AssetType.Archive
            (context is ResourceSnifferContext) ->
                AssetType.File
            else ->
                AssetType.Directory
        }

        var mediaType: MediaType? = null

        for (sniffer in sniffers) {
            sniffer(context)?.let { mediaType = it }
        }

        // Falls back on the system-wide registered media types using [MimeTypeMap].
        // Note: This is done after the heavy sniffing of the provided [sniffers], because
        // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
        // their content (for example, for RWPM).
        Sniffers.system(context)?.let { mediaType = it }

        // If nothing else worked, we try to parse the first valid media type hint.
        for (mediaTypeHint in context.mediaTypes) {
            MediaType.parse(mediaTypeHint.toString())?.let { mediaType = it }
        }

        if (mediaType != null) {
            return AssetDescription(mediaType!!, type)
        }

        return null
    }
}
