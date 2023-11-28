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
        MediaType.CBR to "cbr",
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
    ),
    superTypes: Map<MediaType, MediaType> = mapOf(
        MediaType.CBR to MediaType.RAR,
        MediaType.CBZ to MediaType.ZIP,
        MediaType.DIVINA to MediaType.READIUM_WEBPUB,
        MediaType.DIVINA_MANIFEST to MediaType.READIUM_WEBPUB_MANIFEST,
        MediaType.EPUB to MediaType.ZIP,
        MediaType.XHTML to MediaType.XML,
        MediaType.JSON_PROBLEM_DETAILS to MediaType.JSON,
        MediaType.LCP_LICENSE_DOCUMENT to MediaType.JSON,
        MediaType.LCP_PROTECTED_AUDIOBOOK to MediaType.READIUM_AUDIOBOOK,
        MediaType.LCP_PROTECTED_PDF to MediaType.READIUM_WEBPUB,
        MediaType.OPDS1 to MediaType.XML,
        MediaType.OPDS1_ENTRY to MediaType.XML,
        MediaType.OPDS2 to MediaType.JSON,
        MediaType.OPDS2_PUBLICATION to MediaType.JSON,
        MediaType.READIUM_AUDIOBOOK to MediaType.READIUM_WEBPUB,
        MediaType.READIUM_AUDIOBOOK_MANIFEST to MediaType.READIUM_WEBPUB_MANIFEST,
        MediaType.READIUM_WEBPUB to MediaType.ZIP,
        MediaType.READIUM_WEBPUB_MANIFEST to MediaType.JSON,
        MediaType.W3C_WPUB_MANIFEST to MediaType.JSON,
        MediaType.ZAB to MediaType.ZIP
    )
) {

    private val fileExtensions: MutableMap<MediaType, String> = fileExtensions.toMutableMap()

    private val superTypes: MutableMap<MediaType, MediaType> = superTypes.toMutableMap()

    /**
     * Registers a new [fileExtension] for the given [mediaType].
     */
    public fun register(
        mediaType: MediaType,
        fileExtension: String?,
        superType: MediaType?
    ) {
        if (fileExtension == null) {
            fileExtensions.remove(mediaType)
        } else {
            fileExtensions[mediaType] = fileExtension
        }

        if (superType == null) {
            superTypes.remove(mediaType)
        } else {
            superTypes[mediaType] = superType
        }
    }

    /**
     * Returns the file extension associated to this canonical [mediaType], if any.
     */
    public fun fileExtension(mediaType: MediaType): String? =
        fileExtensions[mediaType]

    public fun superType(mediaType: MediaType): MediaType? =
        superTypes[mediaType]

    public fun isSuperType(mediaType: MediaType): Boolean =
        superTypes.values.any { it.matches(mediaType) }
}
