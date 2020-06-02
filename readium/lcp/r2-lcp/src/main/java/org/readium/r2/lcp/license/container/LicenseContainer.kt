/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import org.readium.r2.lcp.ContainerError
import org.readium.r2.lcp.LCPError
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.format.Format
import java.io.File

/**
 * Encapsulates the read/write access to the packaged License Document (eg. in an EPUB container,
 * or a standalone LCPL file).
 */
internal interface LicenseContainer {
    fun read() : ByteArray
    fun write(license: LicenseDocument)
}

internal fun createLicenseContainer(filepath: String, mediaTypes: List<String> = emptyList()): LicenseContainer {
    val format = Format.of(File(filepath), mediaTypes = mediaTypes, fileExtensions = emptyList())
        ?: throw LCPError.licenseContainer(ContainerError.openFailed)
    return createLicenseContainer(filepath, format)
}

internal fun createLicenseContainer(filepath: String, format: Format): LicenseContainer =
    when (format) {
        Format.EPUB -> EPUBLicenseContainer(filepath)
        Format.LCP_LICENSE -> LCPLLicenseContainer(filepath)
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> WebPubLicenseContainer(filepath)
    }
