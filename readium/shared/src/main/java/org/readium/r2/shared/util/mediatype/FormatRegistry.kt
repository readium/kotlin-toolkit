/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

/**
 * Registry of format metadata (e.g. file extension) associated to canonical media types.
 */
public class FormatRegistry(
    fileExtensions: Map<MediaType, String> = mapOf(
        MediaType.ACSM to "acsm",
        MediaType.CBZ to "cbz",
        MediaType.DIVINA to "divina",
        MediaType.DIVINA_MANIFEST to "json",
        MediaType.EPUB to "epub",
        MediaType.LCP_LICENSE_DOCUMENT to "lcpl",
        MediaType.LCP_PROTECTED_AUDIOBOOK to "lcpa",
        MediaType.LCP_PROTECTED_PDF to "lcpdf",
        MediaType.PDF to "pdf",
        MediaType.READIUM_AUDIOBOOK to "audiobook",
        MediaType.READIUM_AUDIOBOOK_MANIFEST to "json",
        MediaType.READIUM_WEBPUB to "webpub",
        MediaType.READIUM_WEBPUB_MANIFEST to "json",
        MediaType.W3C_WPUB_MANIFEST to "json",
        MediaType.ZAB to "zab"
    )
) {

    private val fileExtensions: MutableMap<MediaType, String> = fileExtensions.toMutableMap()

    /**
     * Registers a new [fileExtension] for the given [mediaType].
     */
    public fun register(mediaType: MediaType, fileExtension: String?) {
        if (fileExtension == null) {
            fileExtensions.remove(mediaType)
        } else {
            fileExtensions[mediaType] = fileExtension
        }
    }

    /**
     * Returns the file extension associated to this canonical [mediaType], if any.
     */
    public fun fileExtension(mediaType: MediaType): String? =
        fileExtensions[mediaType]
}
