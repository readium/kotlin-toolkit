/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import java.io.File
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.RecursiveArchiveFactory
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType
import org.readium.r2.shared.util.tryRecover
import org.readium.r2.shared.util.use

/**
 * Retrieves a canonical [MediaType] for the provided media type and file extension hints and
 * asset content.
 *
 * The actual format sniffing is done by the provided [mediaTypeSniffer].
 * The [DefaultMediaTypeSniffer] covers the formats supported with Readium by default.
 */
public class MediaTypeRetriever(
    private val mediaTypeSniffer: MediaTypeSniffer,
    private val formatRegistry: FormatRegistry,
    archiveFactory: ArchiveFactory
) {

    private val archiveFactory: ArchiveFactory =
        RecursiveArchiveFactory(archiveFactory, formatRegistry)

    /**
     * Retrieves a canonical [MediaType] for the provided media type and file extension [hints].
     *
     * Useful for testing purpose.
     */
    internal fun retrieve(hints: MediaTypeHints): MediaType? =
        retrieveUnsafe(hints)
            .getOrNull()

    /**
     * Retrieves a canonical [MediaType] for the provided [mediaType] and [fileExtension] hints.
     *
     * Useful for testing purpose.
     */
    internal fun retrieve(mediaType: String? = null, fileExtension: String? = null): MediaType? =
        retrieve(
            MediaTypeHints(
                mediaType = mediaType?.let { MediaType(it) },
                fileExtension = fileExtension
            )
        )

    /**
     * Retrieves a canonical [MediaType] for [resource].
     *
     * @param resource the resource to retrieve the media type of
     * @param hints additional hints which will be added to those provided by the resource
     */
    public suspend fun retrieve(
        resource: Resource,
        hints: MediaTypeHints = MediaTypeHints()
    ): Try<MediaType, MediaTypeSnifferError> {
        val resourceMediaType = retrieveUnsafe(resource, hints)
            .getOrElse { return Try.failure(it) }

        val container = archiveFactory.create(resourceMediaType, resource)
            .getOrElse {
                when (it) {
                    is ArchiveFactory.CreateError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))
                    is ArchiveFactory.CreateError.FormatNotSupported ->
                        return Try.success(resourceMediaType)
                }
            }

        return retrieve(container, hints)
    }

    /**
     * Retrieves a canonical [MediaType] for [file].
     *
     * @param file the file to retrieve the media type of
     * @param hints additional hints which will be added to those provided by the resource
     */
    public suspend fun retrieve(
        file: File,
        hints: MediaTypeHints = MediaTypeHints()
    ): Try<MediaType, MediaTypeSnifferError> =
        FileResource(file).use { retrieve(it, hints) }

    /**
     * Retrieves a canonical [MediaType] for [container].
     *
     * @param container the resource to retrieve the media type of
     * @param hints media type hints
     */
    public suspend fun retrieve(
        container: Container<Readable>,
        hints: MediaTypeHints = MediaTypeHints()
    ): Try<MediaType, MediaTypeSnifferError> {
        val unsafeMediaType = retrieveUnsafe(hints)
            .getOrNull()

        if (unsafeMediaType != null && !formatRegistry.isSuperType(unsafeMediaType)) {
            return Try.success(unsafeMediaType)
        }

        mediaTypeSniffer.sniffContainer(container)
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                when (error) {
                    is MediaTypeSnifferError.NotRecognized -> {}
                    else -> return Try.failure(error)
                }
            }

        return (unsafeMediaType ?: hints.mediaTypes.firstOrNull())
            ?.let { Try.success(it) }
            ?: Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    /**
     * Retrieves a [MediaType] as much canonical as possible without accessing the content.
     *
     * Does not refuse too generic types.
     */
    private fun retrieveUnsafe(
        hints: MediaTypeHints
    ): Try<MediaType, MediaTypeSnifferError.NotRecognized> =
        mediaTypeSniffer.sniffHints(hints)
            .tryRecover {
                hints.mediaTypes.firstOrNull()
                    ?.let { Try.success(it) }
                    ?: Try.failure(MediaTypeSnifferError.NotRecognized)
            }

    /**
     * Retrieves a [MediaType] for [resource] using [hints] added to those embedded in [resource]
     * and reading content if necessary.
     *
     * Does not open archive resources.
     */
    private suspend fun retrieveUnsafe(
        resource: Resource,
        hints: MediaTypeHints
    ): Try<MediaType, MediaTypeSnifferError> {
        val properties = resource.properties()
            .getOrElse { return Try.failure(MediaTypeSnifferError.Reading(it)) }

        val embeddedHints = MediaTypeHints(
            mediaType = properties.mediaType,
            fileExtension = properties.filename
                ?.substringAfterLast(".", "")
        )

        val unsafeMediaType = retrieveUnsafe(embeddedHints + hints)
            .getOrNull()

        if (unsafeMediaType != null && !formatRegistry.isSuperType(unsafeMediaType)) {
            return Try.success(unsafeMediaType)
        }

        mediaTypeSniffer.sniffBlob(resource)
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                when (error) {
                    is MediaTypeSnifferError.NotRecognized -> {}
                    else -> return Try.failure(error)
                }
            }

        return (unsafeMediaType ?: hints.mediaTypes.firstOrNull())
            ?.let { Try.success(it) }
            ?: Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}
