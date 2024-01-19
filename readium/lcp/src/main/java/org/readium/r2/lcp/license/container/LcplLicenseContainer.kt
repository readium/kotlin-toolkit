/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import java.io.File
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.util.toUrl

/**
 * Access a License Document stored in an LCP License Document file (LCPL).
 */
internal class LcplLicenseContainer(private val licenseFile: File) : WritableLicenseContainer {

    override fun read(): ByteArray =
        try {
            licenseFile.readBytes()
        } catch (e: Exception) {
            throw LcpException(LcpError.Container.OpenFailed)
        }

    override fun write(license: LicenseDocument) {
        try {
            licenseFile.writeBytes(license.toByteArray())
        } catch (e: Exception) {
            throw LcpException(LcpError.Container.WriteFailed(licenseFile.toUrl()))
        }
    }
}
