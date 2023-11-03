/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import org.readium.r2.shared.util.Try

/**
 * Retrieves a canonical [MediaType] for the provided media type and file extension hints and/or
 * asset content.
 *
 * The actual format sniffing is done by the provided [sniffers]. The [defaultSniffers] cover the
 * formats supported with Readium by default.
 */
public class MediaTypeRetriever(
    private val sniffers: List<MediaTypeSniffer> = defaultSniffers
) {

    public companion object {
        /**
         * The default sniffers provided by Readium 2 for all known formats.
         * The sniffers order is important, because some formats are subsets of other formats.
         */
        public val defaultSniffers: List<MediaTypeSniffer> = listOf(
            XhtmlMediaTypeSniffer,
            HtmlMediaTypeSniffer,
            OpdsMediaTypeSniffer,
            LcpLicenseMediaTypeSniffer,
            BitmapMediaTypeSniffer,
            WebPubManifestMediaTypeSniffer,
            WebPubMediaTypeSniffer,
            W3cWpubMediaTypeSniffer,
            EpubMediaTypeSniffer,
            LpfMediaTypeSniffer,
            ArchiveMediaTypeSniffer,
            PdfMediaTypeSniffer,
            JsonMediaTypeSniffer
        )
    }

    /**
     * Retrieves a canonical [MediaType] for the provided media type and file extension [hints].
     */
    public fun retrieve(hints: MediaTypeHints): MediaType? {
        for (sniffer in sniffers) {
            sniffer.sniffHints(hints)
                .getOrNull()
                ?.let { return it }
        }

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

    /**
     * Retrieves a canonical [MediaType] for the provided media type and file extensions [hints] and
     * asset [content].
     */
    public suspend fun retrieve(
        hints: MediaTypeHints = MediaTypeHints(),
        content: MediaTypeSnifferContent? = null
    ): Try<MediaType, MediaTypeSnifferError> {
        for (sniffer in sniffers) {
            sniffer.sniffHints(hints)
                .getOrNull()
                ?.let { return Try.success(it) }
        }

        if (content != null) {
            for (sniffer in sniffers) {
                sniffer.sniffContent(content)
                    .onSuccess { return Try.success(it) }
                    .onFailure { error ->
                        if (error is MediaTypeSnifferError.SourceError) {
                            return Try.failure(error)
                        }
                    }
            }
        }

        // Falls back on the system-wide registered media types using MimeTypeMap.
        // Note: This is done after the default sniffers, because otherwise it will detect
        // JSON, XML or ZIP formats before we have a chance of sniffing their content (for example,
        // for RWPM).
        SystemMediaTypeSniffer.sniffHints(hints)
            .getOrNull()
            ?.let { return Try.success(it) }

        if (content != null) {
            SystemMediaTypeSniffer.sniffContent(content)
                .onSuccess { return Try.success(it) }
                .onFailure { error ->
                    if (error is MediaTypeSnifferError.SourceError) {
                        return Try.failure(error)
                    }
                }
        }

        return hints.mediaTypes.firstOrNull()
            ?.let { Try.success(it) }
            ?: Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}
