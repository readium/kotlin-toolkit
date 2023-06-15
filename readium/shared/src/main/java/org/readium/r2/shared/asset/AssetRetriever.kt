/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.ContainerFactory
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.ContainerSnifferContext
import org.readium.r2.shared.util.mediatype.ContentAwareSnifferContext
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.ResourceSnifferContext
import org.readium.r2.shared.util.mediatype.Sniffer
import org.readium.r2.shared.util.mediatype.Sniffers
import org.readium.r2.shared.util.mediatype.UrlSnifferContextFactory
import org.readium.r2.shared.util.toUrl

class AssetRetriever(
    resourceFactory: ResourceFactory,
    containerFactory: ContainerFactory,
    archiveFactory: ArchiveFactory,
    private val sniffers: List<Sniffer> = MediaType.sniffers
) {

    private val snifferContextFactory: UrlSnifferContextFactory =
        UrlSnifferContextFactory(resourceFactory, containerFactory, archiveFactory)

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): Asset? {
        return ofFile(file, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): Asset? {
        return ofFile(File(path), mediaType = mediaType, fileExtension = fileExtension)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): Asset? {
        return ofFile(File(path), mediaTypes = mediaTypes, fileExtensions = fileExtensions)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): Asset? {
        val context = snifferContextFactory
            .createContext(file.toUrl(), mediaTypes, listOf(file.extension) + fileExtensions)
            ?: return null

        val fallbackName = file.name

        return this.of(context, fallbackName)
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
    ): Asset? {
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
    ): Asset? {
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

        val fallbackName = url.filename

        return this.of(context, fallbackName)
    }

    suspend fun ofUrl(
        url: Url,
        mediaType: String? = null,
        fileExtension: String? = null
    ): Asset? {
        return ofUrl(url, listOfNotNull(mediaType), listOfNotNull(fileExtension))
    }

    private suspend fun ofUrl(
        url: Url,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): Asset? {
        val context = snifferContextFactory
            .createContext(url, mediaTypes, fileExtensions)
            ?: return null

        val fallbackName = url.filename

        return of(context, fallbackName)
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
        context: ContentAwareSnifferContext,
        fallbackName: String
    ): Asset? {

        suspend fun asset(mediaType: MediaType): Asset {
            return when (context) {
                is ContainerSnifferContext ->
                    Asset.Container(
                        context.container.name().getOrNull() ?: fallbackName,
                        mediaType,
                        if (context.isExploded) AssetType.Directory else AssetType.Archive,
                        context.container
                    )
                is ResourceSnifferContext ->
                    Asset.Resource(
                        context.resource.name().getOrNull() ?: fallbackName,
                        mediaType,
                        context.resource
                    )
            }
        }

        for (sniffer in sniffers) {
            sniffer(context)?.let { return asset(it) }
        }

        // Falls back on the system-wide registered media types using [MimeTypeMap].
        // Note: This is done after the heavy sniffing of the provided [sniffers], because
        // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
        // their content (for example, for RWPM).
        Sniffers.system(context)?.let { return asset(it) }

        // If nothing else worked, we try to parse the first valid media type hint.
        for (mediaTypeHint in context.mediaTypes) {
            MediaType.parse(mediaTypeHint.toString())?.let { return asset(it) }
        }

        return null
    }
}
