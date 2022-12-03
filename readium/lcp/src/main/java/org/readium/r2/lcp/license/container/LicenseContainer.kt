/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Encapsulates the read/write access to the packaged License Document (eg. in an EPUB container,
 * or a standalone LCPL file).
 */
internal interface LicenseContainer {
    fun read(): ByteArray
    fun write(license: LicenseDocument)
}

internal suspend fun createLicenseContainer(
    filepath: String,
    mediaTypes: List<String> = emptyList()
): LicenseContainer {
    val mediaType = MediaType.ofFile(filepath, mediaTypes = mediaTypes, fileExtensions = emptyList())
        ?: throw LcpException.Container.OpenFailed
    return createLicenseContainer(filepath, mediaType)
}

internal fun createLicenseContainer(filepath: String, mediaType: MediaType): LicenseContainer =
    when (mediaType) {
        MediaType.EPUB -> EPUBLicenseContainer(filepath)
        MediaType.LCP_LICENSE_DOCUMENT -> LCPLLicenseContainer(filepath)
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> WebPubLicenseContainer(filepath)
    }
