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
    ),
    parentMediaTypes: Map<MediaType, MediaType> = mapOf(
        MediaType.EPUB to MediaType.ZIP,
        MediaType.READIUM_AUDIOBOOK to MediaType.READIUM_WEBPUB,
        MediaType.READIUM_WEBPUB to MediaType.ZIP
    ),
    archiveMediaTypes: List<MediaType> = listOf(MediaType.ZIP)
) {

    private val fileExtensions: MutableMap<MediaType, String> = fileExtensions.toMutableMap()

    private val parentMediaTypes: MutableMap<MediaType, MediaType> = parentMediaTypes.toMutableMap()

    private val archiveMediaTypes = archiveMediaTypes.toMutableList()

    /**
     * Registers a new [fileExtension] for the given [mediaType].
     */
    public fun register(
        mediaType: MediaType,
        fileExtension: String?,
        isArchive: Boolean,
        parent: MediaType?
    ) {
        if (fileExtension == null) {
            fileExtensions.remove(mediaType)
        } else {
            fileExtensions[mediaType] = fileExtension
        }

        if (parent == null) {
            parentMediaTypes.remove(mediaType)
        } else {
            parentMediaTypes[mediaType] = parent
        }

        if (isArchive) {
            archiveMediaTypes.add(mediaType)
        } else {
            archiveMediaTypes.remove(mediaType)
        }
    }

    /**
     * Returns the file extension associated to this canonical [mediaType], if any.
     */
    public fun fileExtension(mediaType: MediaType): String? =
        fileExtensions[mediaType]

    public fun parentMediaType(mediaType: MediaType): MediaType? =
        parentMediaTypes[mediaType]

    public fun MediaType.isAlso(mediaType: MediaType): Boolean {
        if (this == mediaType) {
            return true
        }

        return parentMediaTypes[this]
            ?.isAlso(mediaType)
            ?: false
    }

    public val MediaType.isArchive: Boolean get() {
        if (this in archiveMediaTypes) {
            return true
        }

        return parentMediaTypes[this]
            ?.isArchive
            ?: false
    }
}
