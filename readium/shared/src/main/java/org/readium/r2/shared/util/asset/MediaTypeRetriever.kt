/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import java.io.File
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.SmartArchiveFactory
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.FileResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use

/**
 * Retrieves a canonical [MediaType] for the provided media type and file extension hints and/or
 * asset content.
 *
 * The actual format sniffing is mostly done by the provided [mediaTypeSniffer].
 * The [DefaultMediaTypeSniffer] cover the formats supported with Readium by default.
 */
public class MediaTypeRetriever(
    private val mediaTypeSniffer: MediaTypeSniffer,
    formatRegistry: FormatRegistry,
    archiveFactory: ArchiveFactory,
    contentResolver: ContentResolver?
) {

    private val simpleResourceMediaTypeRetriever: SimpleResourceMediaTypeRetriever =
        SimpleResourceMediaTypeRetriever(mediaTypeSniffer, contentResolver, formatRegistry)

    private val archiveFactory: ArchiveFactory =
        SmartArchiveFactory(archiveFactory, formatRegistry)

    /**
     * Retrieves a canonical [MediaType] for the provided media type and file extension [hints].
     */
    internal fun retrieve(hints: MediaTypeHints): MediaType? =
        simpleResourceMediaTypeRetriever.retrieveUnsafe(hints)
            .getOrNull()

    /**
     * Retrieves a canonical [MediaType] for the provided [mediaType] and [fileExtension] hints.
     */
    internal fun retrieve(mediaType: String? = null, fileExtension: String? = null): MediaType? =
        retrieve(
            MediaTypeHints(
                mediaType = mediaType?.let { MediaType(it) },
                fileExtension = fileExtension
            )
        )

    /**
     * Retrieves a canonical [MediaType] for the provided [mediaType] and [fileExtension] hints.
     */

    internal fun retrieve(mediaType: MediaType, fileExtension: String? = null): MediaType =
        retrieve(MediaTypeHints(mediaType = mediaType, fileExtension = fileExtension)) ?: mediaType

    /**
     * Retrieves a canonical [MediaType] for the provided [mediaTypes] and [fileExtensions] hints.
     */
    internal fun retrieve(
        mediaTypes: List<String> = emptyList(),
        fileExtensions: List<String> = emptyList()
    ): MediaType? =
        retrieve(MediaTypeHints(mediaTypes = mediaTypes, fileExtensions = fileExtensions))

    public suspend fun retrieve(
        container: Container<Readable>,
        hints: MediaTypeHints = MediaTypeHints()
    ): Try<MediaType, MediaTypeSnifferError> {
        simpleResourceMediaTypeRetriever.retrieveSafe(hints)
            .let { Try.success(it) }

        mediaTypeSniffer.sniffContainer(container)
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                when (error) {
                    is MediaTypeSnifferError.NotRecognized -> {}
                    else -> return Try.failure(error)
                }
            }

        return simpleResourceMediaTypeRetriever.retrieveUnsafe(hints)
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
        val resourceMediaType = simpleResourceMediaTypeRetriever.retrieve(resource, hints)
            .getOrElse { return Try.failure(it) }

        val container = archiveFactory.create(resourceMediaType, resource)
            .getOrElse {
                when (it) {
                    is ArchiveFactory.Error.ReadError ->
                        return Try.failure(MediaTypeSnifferError.Read(it.cause))
                    is ArchiveFactory.Error.FormatNotSupported ->
                        return Try.success(resourceMediaType)
                }
            }

        return retrieve(container, hints)
    }
}
