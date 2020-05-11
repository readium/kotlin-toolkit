/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readium.r2.lcp.license.container

import android.net.Uri
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.ContainerError
import org.readium.r2.lcp.LCPError
import org.readium.r2.lcp.license.model.LicenseDocument
import org.zeroturnaround.zip.ZipUtil
import timber.log.Timber
import java.io.File
import java.net.URL

/**
 * Access a License Document stored in an LCP License Document file (LCPL).
 */
internal class LCPLLicenseContainer(private val lcpl: String) : LicenseContainer {

    override fun read() : ByteArray =
        try {
            File(lcpl).readBytes()
        } catch (e: Exception) {
            throw LCPError.licenseContainer(ContainerError.openFailed)
        }

    override fun write(license: LicenseDocument) {
        try {
            File(lcpl).writeBytes(license.data)
        } catch (e: Exception) {
            throw LCPError.licenseContainer(ContainerError.writeFailed(lcpl))
        }
    }

}

