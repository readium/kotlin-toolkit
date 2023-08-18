/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.format

import org.readium.r2.shared.util.mediatype.DefaultMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.HintMediaTypeSnifferContext
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferContext

public class FormatRegistry(
    formats: List<Format> = listOf(
        // The known formats are not declared as constants to discourage comparing the format
        // instance instead of the media type for equality.
        Format(
            MediaType.ACSM,
            name = "Adobe Content Server Message",
            fileExtension = "acsm"
        ),
        Format(
            MediaType.CBZ,
            name = "Comic Book Archive",
            fileExtension = "cbz"
        ),
        Format(
            MediaType.DIVINA,
            name = "Digital Visual Narratives",
            fileExtension = "divina"
        ),
        Format(
            MediaType.DIVINA_MANIFEST,
            name = "Digital Visual Narratives",
            fileExtension = "json"
        ),
        Format(
            MediaType.EPUB,
            name = "EPUB",
            fileExtension = "epub"
        ),
        Format(
            MediaType.LCP_LICENSE_DOCUMENT,
            name = "LCP License",
            fileExtension = "lcpl"
        ),
        Format(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            name = "LCP Protected Audiobook",
            fileExtension = "lcpa"
        ),
        Format(
            MediaType.LCP_PROTECTED_PDF,
            name = "LCP Protected PDF",
            fileExtension = "lcpdf"
        ),
        Format(
            MediaType.PDF,
            name = "PDF",
            fileExtension = "pdf"
        ),
        Format(
            MediaType.READIUM_AUDIOBOOK,
            name = "Readium Audiobook",
            fileExtension = "audiobook"
        ),
        Format(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            name = "Readium Audiobook",
            fileExtension = "json"
        ),
        Format(
            MediaType.READIUM_WEBPUB,
            name = "Readium Web Publication",
            fileExtension = "webpub"
        ),
        Format(
            MediaType.READIUM_WEBPUB_MANIFEST,
            name = "Readium Web Publication",
            fileExtension = "json"
        ),
        Format(
            MediaType.W3C_WPUB_MANIFEST,
            name = "Web Publication",
            fileExtension = "json"
        ),
        Format(
            MediaType.ZAB,
            name = "Zipped Audio Book",
            fileExtension = "zab"
        )
    ),
    private val sniffer: MediaTypeSniffer = DefaultMediaTypeSniffer()
) {
    private val formats: MutableMap<MediaType, Format> =
        formats.associateBy { it.mediaType }.toMutableMap()

    public fun register(format: Format) {
        formats[format.mediaType] = format
    }

    public suspend fun canonicalize(mediaType: MediaType): MediaType =
        retrieve(mediaType).mediaType

    public suspend fun retrieve(mediaType: MediaType): Format =
        retrieve(HintMediaTypeSnifferContext(hints = FormatHints(mediaType)))
            ?: Format(mediaType)

    public suspend fun retrieve(context: MediaTypeSnifferContext): Format? =
        sniffer.sniff(context)?.let {
            formats[it] ?: Format(it)
        }
}

/**
 * Represents a media format, identified by a unique RFC 6838 media type.
 *
 * @param mediaType Canonical media type for this format.
 * @param name A human readable name identifying the format, which may be presented to the user.
 * @param fileExtension The default file extension to use for this format.
 */
public data class Format(
    public val mediaType: MediaType,
    public val name: String? = null,
    public val fileExtension: String? = null
) {

    override fun toString(): String =
        name ?: mediaType.toString()
}

public data class FormatHints(
    val mediaTypes: List<MediaType> = emptyList(),
    val fileExtensions: List<String> = emptyList()
) {
    public companion object {
        public operator fun invoke(mediaType: MediaType? = null, fileExtension: String? = null): FormatHints =
            FormatHints(
                mediaTypes = listOfNotNull(mediaType),
                fileExtensions = listOfNotNull(fileExtension)
            )

        public operator fun invoke(
            mediaTypes: List<String> = emptyList(),
            fileExtensions: List<String> = emptyList()
        ): FormatHints =
            FormatHints(mediaTypes.mapNotNull { MediaType(it) }, fileExtensions = fileExtensions)
    }

    public operator fun plus(other: FormatHints): FormatHints =
        FormatHints(
            mediaTypes = mediaTypes + other.mediaTypes,
            fileExtensions = fileExtensions + other.fileExtensions
        )
}
