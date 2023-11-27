/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import android.content.ContentResolver
import java.io.File
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.DefaultMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.mediatype.SystemMediaTypeSniffer
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.zip.ZipArchiveFactory

/**
 * Retrieves a canonical [MediaType] for the provided media type and file extension hints and/or
 * asset content.
 *
 * The actual format sniffing is mostly done by the provided [mediaTypeSniffer].
 * The [DefaultMediaTypeSniffer] cover the formats supported with Readium by default.
 */
@OptIn(DelicateReadiumApi::class)
public class MediaTypeRetriever(
    private val mediaTypeSniffer: MediaTypeSniffer,
    formatRegistry: FormatRegistry,
    archiveFactory: ArchiveFactory,
    contentResolver: ContentResolver?
) {

    public companion object {

        @Deprecated("This overload will be removed without notice as soon as possible.")
        public operator fun invoke(
            contentResolver: ContentResolver? = null
        ): MediaTypeRetriever {
            val mediaTypeSniffer =
                DefaultMediaTypeSniffer()

            val archiveFactory =
                ZipArchiveFactory()

            val formatRegistry =
                FormatRegistry()

            return MediaTypeRetriever(
                mediaTypeSniffer,
                formatRegistry,
                archiveFactory,
                contentResolver
            )
        }
    }

    private val blobMediaTypeRetriever: BlobMediaTypeRetriever =
        BlobMediaTypeRetriever(mediaTypeSniffer, contentResolver)

    private val archiveFactory: ArchiveFactory =
        SmartArchiveFactory(archiveFactory, formatRegistry)

    /**
     * Retrieves a canonical [MediaType] for the provided media type and file extension [hints].
     */
    public fun retrieve(hints: MediaTypeHints): MediaType? {
        blobMediaTypeRetriever.retrieve(hints)
            ?.let { return it }

        // Falls back on the system-wide registered media types using MimeTypeMap.
        // Note: This is done after the default sniffers, because otherwise it will detect
        // JSON, XML or ZIP formats before we have a chance of sniffing their content (for example,
        // for RWPM).
        SystemMediaTypeSniffer.sniffHints(hints)
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
        container: Container<Readable>,
        hints: MediaTypeHints = MediaTypeHints()
    ): Try<MediaType, MediaTypeSnifferError> {
        mediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return Try.success(it) }

        mediaTypeSniffer.sniffContainer(container)
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                when (error) {
                    is MediaTypeSnifferError.NotRecognized -> {}
                    else -> return Try.failure(error)
                }
            }

        return hints.mediaTypes.firstOrNull()
            ?.let { Try.success(it) }
            ?: Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    public suspend fun retrieve(
        file: File,
        hints: MediaTypeHints = MediaTypeHints()
    ): Try<MediaType, MediaTypeSnifferError> =
        FileResource(file).use { retrieve(it, hints) }

    /**
     * Retrieves a canonical [MediaType] for [resource].
     */
    public suspend fun retrieve(
        resource: Resource,
        hints: MediaTypeHints = MediaTypeHints()
    ): Try<MediaType, MediaTypeSnifferError> {
        val properties = resource.properties()
            .getOrElse { return Try.failure(MediaTypeSnifferError.Read(it)) }

        mediaTypeSniffer.sniffHints(MediaTypeHints(properties) + hints)
            .getOrNull()
            ?.let { return Try.success(it) }

        val blobMediaType = blobMediaTypeRetriever.retrieve(hints, resource)
            .getOrElse { return Try.failure(it) }

        val container = archiveFactory.create(blobMediaType, resource)
            .getOrElse {
                when (it) {
                    is ArchiveFactory.Error.ReadError ->
                        return Try.failure(MediaTypeSnifferError.Read(it.cause))
                    is ArchiveFactory.Error.FormatNotSupported ->
                        return Try.success(blobMediaType)
                }
            }

        return retrieve(container, hints)
    }
}
