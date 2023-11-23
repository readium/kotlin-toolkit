/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import android.content.ContentResolver
import android.provider.MediaStore
import java.io.File
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.mediatype.SystemMediaTypeSniffer
import org.readium.r2.shared.util.toUri

@DelicateReadiumApi
public class BlobMediaTypeRetriever(
    private val mediaTypeSniffer: MediaTypeSniffer,
    private val contentResolver: ContentResolver?
) {

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
        SystemMediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return it }

        return hints.mediaTypes.firstOrNull()
    }

    public suspend fun retrieve(hints: MediaTypeHints, blob: Blob): Try<MediaType, MediaTypeSnifferError> {
        mediaTypeSniffer.sniffBlob(blob)
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                when (error) {
                    is MediaTypeSnifferError.NotRecognized -> {}
                    else -> return Try.failure(error)
                }
            }

        // Falls back on the system-wide registered media types using MimeTypeMap.
        // Note: This is done after the default sniffers, because otherwise it will detect
        // JSON, XML or ZIP formats before we have a chance of sniffing their content (for example,
        // for RWPM).
        SystemMediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return Try.success(it) }

        SystemMediaTypeSniffer.sniffBlob(blob)
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                when (error) {
                    is MediaTypeSnifferError.NotRecognized -> {}
                    else -> return Try.failure(error)
                }
            }

        // Falls back on the [contentResolver] in case of content Uri.
        // Note: This is done after the heavy sniffing of the provided [sniffers], because
        // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
        // their content (for example, for RWPM).

        if (contentResolver != null) {
            blob.source
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
