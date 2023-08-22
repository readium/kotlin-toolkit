/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

/**
 * The default sniffer provided by Readium 2 to resolve a [MediaType].
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

    public fun retrieve(hints: MediaTypeHints): MediaType? {
        sniffers.firstNotNullOfOrNull { it.sniffHints(hints) }
            ?.let { return it }

        // Falls back on the system-wide registered media types using MimeTypeMap.
        // Note: This is done after the default sniffers, because otherwise it will detect
        // JSON, XML or ZIP formats before we have a chance of sniffing their content (for example,
        // for RWPM).
        SystemMediaTypeSniffer.sniffHints(hints)
            ?.let { return it }

        return hints.mediaTypes.firstOrNull()
    }

    public fun retrieve(mediaType: String? = null, fileExtension: String? = null): MediaType? =
        retrieve(
            MediaTypeHints(
                mediaType = mediaType?.let { MediaType(it) },
                fileExtension = fileExtension
            )
        )

    public fun retrieve(mediaType: MediaType, fileExtension: String? = null): MediaType =
        retrieve(MediaTypeHints(mediaType = mediaType, fileExtension = fileExtension)) ?: mediaType

    public fun retrieve(
        mediaTypes: List<String> = emptyList(),
        fileExtensions: List<String> = emptyList()
    ): MediaType? =
        retrieve(MediaTypeHints(mediaTypes = mediaTypes, fileExtensions = fileExtensions))

    public suspend fun retrieve(
        hints: MediaTypeHints = MediaTypeHints(),
        content: MediaTypeSnifferContent? = null
    ): MediaType? {
        sniffers.run {
            firstNotNullOfOrNull { it.sniffHints(hints) }
                ?: content?.let { firstNotNullOfOrNull { it.sniffContent(content) } }
        }?.let { return it }

        // Falls back on the system-wide registered media types using MimeTypeMap.
        // Note: This is done after the default sniffers, because otherwise it will detect
        // JSON, XML or ZIP formats before we have a chance of sniffing their content (for example,
        // for RWPM).
        SystemMediaTypeSniffer.run {
            sniffHints(hints) ?: content?.let { sniffContent(it) }
        }?.let { return it }

        return hints.mediaTypes.firstOrNull()
    }
}
