/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import android.provider.MediaStore
import java.io.File
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.invoke
import org.readium.r2.shared.util.toUri
import org.readium.r2.shared.util.tryRecover

/**
 * A [MediaTypeRetriever] which does not open archive resources.
 */
internal class SimpleResourceMediaTypeRetriever(
    private val mediaTypeSniffer: MediaTypeSniffer,
    private val contentResolver: ContentResolver?,
    private val formatRegistry: FormatRegistry
) {

    /**
     * Retrieves a canonical [MediaType] for the provided media type [hints].
     *
     * Does not recognize media types and file extensions for too generic types.
     */
    fun retrieveSafe(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> =
        retrieveUnsafe(hints)
            .flatMap {
                if (formatRegistry.isSuperType(it)) {
                    Try.failure(MediaTypeSnifferError.NotRecognized)
                } else {
                    Try.success(it)
                }
            }

    /**
     * Retrieves a [MediaType] as much canonical as possible without accessing the content.
     */
    fun retrieveUnsafe(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> =
        mediaTypeSniffer.sniffHints(hints)
            .tryRecover {
                hints.mediaTypes.firstOrNull()
                    ?.let { Try.success(it) }
                    ?: Try.failure(MediaTypeSnifferError.NotRecognized)
            }

    /**
     * Retrieves a [MediaType] for [resource] using [hints] added to those embedded in [resource]
     * and reading content if necessary.
     */
    suspend fun retrieve(resource: Resource, hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError> {
        val properties = resource.properties()
            .getOrElse { return Try.failure(MediaTypeSnifferError.Read(it)) }

        retrieveSafe(MediaTypeHints(properties) + hints)
            .onSuccess { return Try.success(it) }

        if (contentResolver != null) {
            resource.source
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

                    retrieveSafe(contentHints)
                        .onSuccess { return Try.success(it) }
                }
        }

        mediaTypeSniffer.sniffBlob(resource)
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                when (error) {
                    is MediaTypeSnifferError.NotRecognized -> {}
                    else -> return Try.failure(error)
                }
            }

        return retrieveUnsafe(hints)
    }
}
