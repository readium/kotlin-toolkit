/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.mediatype.MediaType

public data class FormatInfo(
    public val mediaType: MediaType,
    public val fileExtension: FileExtension
)

/**
 * Registry of format metadata (e.g. file extension) associated to canonical media types.
 */
public class FormatRegistry(
    formatInfo: Map<Format, FormatInfo> = mapOf(
        Format.CBR to FormatInfo(MediaType.CBR, FileExtension("cbr")),
        Format.CBZ to FormatInfo(MediaType.CBZ, FileExtension("cbz")),
        Format(setOf(Trait.ZIP, Trait.RPF, Trait.COMICS)) to FormatInfo(
            MediaType.DIVINA,
            FileExtension("divina")
        ),
        Format.READIUM_COMICS_MANIFEST to FormatInfo(
            MediaType.DIVINA_MANIFEST,
            FileExtension("json")
        ),
        Format.EPUB to FormatInfo(MediaType.EPUB, FileExtension("epub")),
        Format.LCP_LICENSE_DOCUMENT to FormatInfo(
            MediaType.LCP_LICENSE_DOCUMENT,
            FileExtension("lcpl")
        ),
        (Format.READIUM_AUDIOBOOK + Trait.LCP_PROTECTED) to FormatInfo(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            FileExtension("lcpa")
        ),
        (Format.READIUM_PDF + Trait.LCP_PROTECTED)to FormatInfo(
            MediaType.LCP_PROTECTED_PDF,
            FileExtension("lcpdf")
        ),
        Format(setOf(Trait.PDF)) to FormatInfo(MediaType.PDF, FileExtension("pdf")),
        Format.READIUM_AUDIOBOOK to FormatInfo(
            MediaType.READIUM_AUDIOBOOK,
            FileExtension("audiobook")
        ),
        Format.READIUM_AUDIOBOOK_MANIFEST to FormatInfo(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            FileExtension("json")
        ),
        Format.READIUM_WEBPUB to FormatInfo(MediaType.READIUM_WEBPUB, FileExtension("webpub")),
        Format.READIUM_WEBPUB_MANIFEST to FormatInfo(
            MediaType.READIUM_WEBPUB,
            FileExtension("json")
        ),
        Format(setOf(Trait.BITMAP, Trait.JPEG)) to FormatInfo(MediaType.JPEG, FileExtension("jpg"))
    )
) {

    private val formatInfo: MutableMap<Format, FormatInfo> = formatInfo.toMutableMap()

    /**
     * Registers format info for the given [Format].
     */
    public fun register(
        format: Format,
        formatInfo: FormatInfo
    ) {
        this.formatInfo[format] = formatInfo
    }

    public operator fun get(format: Format): FormatInfo? =
        formatInfo[format]
}
