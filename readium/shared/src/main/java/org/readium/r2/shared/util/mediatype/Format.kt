/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

public class FormatRegistry(
    private val sniffer: MediaTypeSniffer,
    formats: Map<MediaType, Format> = mapOf(
        MediaType.ACSM to Format(
            name = "Adobe Content Server Message",
            fileExtension = "acsm"
        ),
        MediaType.CBZ to Format(
            name = "Comic Book Archive",
            fileExtension = "cbz"
        ),
        MediaType.DIVINA to Format(
            name = "Digital Visual Narratives",
            fileExtension = "divina"
        ),
        MediaType.DIVINA_MANIFEST to Format(
            name = "Digital Visual Narratives",
            fileExtension = "json"
        ),
        MediaType.EPUB to Format(
            name = "EPUB",
            fileExtension = "epub"
        ),
        MediaType.LCP_LICENSE_DOCUMENT to Format(
            name = "LCP License",
            fileExtension = "lcpl"
        ),
        MediaType.LCP_PROTECTED_AUDIOBOOK to Format(
            name = "LCP Protected Audiobook",
            fileExtension = "lcpa"
        ),
        MediaType.LCP_PROTECTED_PDF to Format(
            name = "LCP Protected PDF",
            fileExtension = "lcpdf"
        ),
        MediaType.PDF to Format(
            name = "PDF",
            fileExtension = "pdf"
        ),
        MediaType.READIUM_AUDIOBOOK to Format(
            name = "Readium Audiobook",
            fileExtension = "audiobook"
        ),
        MediaType.READIUM_AUDIOBOOK_MANIFEST to Format(
            name = "Readium Audiobook",
            fileExtension = "json"
        ),
        MediaType.READIUM_WEBPUB to Format(
            name = "Readium Web Publication",
            fileExtension = "webpub"
        ),
        MediaType.READIUM_WEBPUB_MANIFEST to Format(
            name = "Readium Web Publication",
            fileExtension = "json"
        ),
        MediaType.W3C_WPUB_MANIFEST to Format(
            name = "Web Publication",
            fileExtension = "json"
        ),
        MediaType.ZAB to Format(
            name = "Zipped Audio Book",
            fileExtension = "zab"
        )
    )
) {

    private val formats: MutableMap<MediaType, Format> = formats.toMutableMap()

    public fun register(mediaType: MediaType, format: Format) {
        formats[mediaType] = format
    }

    public suspend fun retrieve(mediaType: MediaType): Format? =
        formats[canonicalize(mediaType)]

    public suspend fun canonicalize(mediaType: MediaType): MediaType =
        sniffer.sniff(HintMediaTypeSnifferContext(hints = MediaTypeHints(mediaType)))
            ?: mediaType
}

/**
 * Represents a media format, identified by a unique RFC 6838 media type.
 *
 * @param name A human readable name identifying the format, which may be presented to the user.
 * @param fileExtension The default file extension to use for this format.
 */
public data class Format(
    public val name: String,
    public val fileExtension: String? = null
) {

    override fun toString(): String = name
}
