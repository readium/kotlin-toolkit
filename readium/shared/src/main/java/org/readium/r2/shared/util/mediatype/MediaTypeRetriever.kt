/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import android.content.ContentResolver
import android.provider.MediaStore
import java.io.File
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.FileBlob
import org.readium.r2.shared.util.toUri

/**
 * Retrieves a canonical [MediaType] for the provided media type and file extension hints and/or
 * asset content.
 *
 * The actual format sniffing is mostly done by the provided [mediaTypeSniffer].
 * The [DefaultMediaTypeSniffer] cover the formats supported with Readium by default.
 */
public class MediaTypeRetriever(
    private val contentResolver: ContentResolver? = null,
    private val mediaTypeSniffer: MediaTypeSniffer = DefaultMediaTypeSniffer()
) : MediaTypeSniffer {

    private val systemMediaTypeSniffer: MediaTypeSniffer =
        SystemMediaTypeSniffer()

    /**
     * Retrieves a canonical [MediaType] for the provided media type and file extension [hints].
     */
    public fun retrieve(hints: MediaTypeHints): MediaType? {
        mediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return it }

        // Falls back on the system-wide registered media types using MimeTypeMap.
        // Note: This is done after the default sniffers, because otherwise it will detect
        // JSON, XML or ZIP formats before we have a chance of sniffing their content (for example,
        // for RWPM).
        systemMediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return it }

        return hints.mediaTypes.firstOrNull()
    }

    /**
     * Retrieves a canonical [MediaType] for the provided [mediaType] and [fileExtension] hints.
     */
    public fun retrieve(mediaType: String? = null, fileExtension: String? = null): MediaType? =
        retrieve(
            MediaTypeHints(
                mediaType = mediaType?.let { MediaType(it) },
                fileExtension = fileExtension
            )
        )

    /**
     * Retrieves a canonical [MediaType] for the provided [mediaType] and [fileExtension] hints.
     */
    public fun retrieve(mediaType: MediaType, fileExtension: String? = null): MediaType =
        retrieve(MediaTypeHints(mediaType = mediaType, fileExtension = fileExtension)) ?: mediaType

    /**
     * Retrieves a canonical [MediaType] for the provided [mediaTypes] and [fileExtensions] hints.
     */
    public fun retrieve(
        mediaTypes: List<String> = emptyList(),
        fileExtensions: List<String> = emptyList()
    ): MediaType? =
        retrieve(MediaTypeHints(mediaTypes = mediaTypes, fileExtensions = fileExtensions))

    public suspend fun retrieve(
        hints: MediaTypeHints = MediaTypeHints(),
        container: Container<*>? = null
    ): Try<MediaType, MediaTypeSnifferError> {
        mediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return Try.success(it) }

        if (container != null) {
            mediaTypeSniffer.sniffContainer(container)
                .onSuccess { return Try.success(it) }
                .onFailure { error ->
                    when (error) {
                        is MediaTypeSnifferError.NotRecognized -> {}
                        else -> return Try.failure(error)
                    }
                }
        }

        return hints.mediaTypes.firstOrNull()
            ?.let { Try.success(it) }
            ?: Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    public suspend fun retrieve(file: File): Try<MediaType, MediaTypeSnifferError> =
        retrieve(
            hints = MediaTypeHints(fileExtension = file.extension),
            blob = FileBlob(file)
        )

    /**
     * Retrieves a canonical [MediaType] for the provided media type and file extensions [hints] and
     * asset [blob].
     */
    public suspend fun retrieve(
        hints: MediaTypeHints = MediaTypeHints(),
        blob: Blob? = null
    ): Try<MediaType, MediaTypeSnifferError> {
        mediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return Try.success(it) }

        if (blob != null) {
            mediaTypeSniffer.sniffBlob(blob)
                .onSuccess { return Try.success(it) }
                .onFailure { error ->
                    when (error) {
                        is MediaTypeSnifferError.NotRecognized -> {}
                        else -> return Try.failure(error)
                    }
                }
        }

        // Falls back on the system-wide registered media types using MimeTypeMap.
        // Note: This is done after the default sniffers, because otherwise it will detect
        // JSON, XML or ZIP formats before we have a chance of sniffing their content (for example,
        // for RWPM).
        systemMediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return Try.success(it) }

        if (blob != null) {
            systemMediaTypeSniffer.sniffBlob(blob)
                .onSuccess { return Try.success(it) }
                .onFailure { error ->
                    when (error) {
                        is MediaTypeSnifferError.NotRecognized -> {}
                        else -> return Try.failure(error)
                    }
                }
        }

        // Falls back on the [contentResolver] in case of content Uri.
        // Note: This is done after the heavy sniffing of the provided [sniffers], because
        // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
        // their content (for example, for RWPM).

        if (contentResolver != null) {
            blob?.source
                ?.takeIf { it.isContent }
                ?.let { url ->
                    val contentHints = MediaTypeHints(
                        mediaType = contentResolver.getType(url.toUri())
                            ?.let { MediaType(it) }
                            ?.takeUnless { it.matches(MediaType.BINARY) },
                        fileExtension = contentResolver
                            .queryProjection(url.uri, MediaStore.MediaColumns.DISPLAY_NAME)
                            ?.let { filename -> File(filename).extension }
                    )

                    retrieve(contentHints)
                        ?.let { return Try.success(it) }
                }
        }

        return hints.mediaTypes.firstOrNull()
            ?.let { Try.success(it) }
            ?: Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}
