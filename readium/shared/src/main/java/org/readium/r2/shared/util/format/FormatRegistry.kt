/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import org.readium.r2.shared.util.mediatype.MediaType

@JvmInline
public value class FileExtension(
    public val value: String
)
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
        Format.RPF_IMAGE to FormatInfo(MediaType.DIVINA, FileExtension("divina")),
        Format.RWPM_IMAGE to FormatInfo(MediaType.DIVINA_MANIFEST, FileExtension("json")),
        Format.EPUB to FormatInfo(MediaType.EPUB, FileExtension("epub")),
        Format.LCP_LICENSE_DOCUMENT to FormatInfo(
            MediaType.LCP_LICENSE_DOCUMENT,
            FileExtension("lcpl")
        ),
        Format.RPF_AUDIO_LCP to FormatInfo(MediaType.LCP_PROTECTED_AUDIOBOOK, FileExtension("lcpa")),
        Format.RPF_PDF_LCP to FormatInfo(MediaType.LCP_PROTECTED_PDF, FileExtension("lcpdf")),
        Format.PDF to FormatInfo(MediaType.PDF, FileExtension("pdf")),
        Format.RPF_AUDIO to FormatInfo(MediaType.READIUM_AUDIOBOOK, FileExtension("audiobook")),
        Format.RWPM_AUDIO to FormatInfo(MediaType.READIUM_AUDIOBOOK_MANIFEST, FileExtension("json")),
        Format.RPF to FormatInfo(MediaType.READIUM_WEBPUB, FileExtension("webpub")),
        Format.RWPM to FormatInfo(MediaType.READIUM_WEBPUB, FileExtension("json")),
        Format.JPEG to FormatInfo(MediaType.JPEG, FileExtension("jpg"))
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
