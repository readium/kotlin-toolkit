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
import java.io.File

/**
 * Access a License Document stored in an LCP License Document file (LCPL).
 */
internal class LCPLLicenseContainer(private val lcpl: String) : LicenseContainer {

    override fun read() : ByteArray =
        try {
            File(lcpl).readBytes()
        } catch (e: Exception) {
            throw LcpException.Container.OpenFailed
        }

    override fun write(license: LicenseDocument) {
        try {
            File(lcpl).writeBytes(license.data)
        } catch (e: Exception) {
            throw LcpException.Container.WriteFailed(lcpl)
        }
    }

}

