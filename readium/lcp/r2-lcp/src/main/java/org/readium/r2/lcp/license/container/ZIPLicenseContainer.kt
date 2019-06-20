/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.public.LCPError
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.util.zip.ZipFile


open class ZIPLicenseContainer(private val zip: String, private val pathInZIP: String) : LicenseContainer {

    override fun read(): ByteArray {

        val archive = try {
            ZipFile(zip)
        } catch (e: Exception) {
            throw LCPError.licenseContainer
        }
        val entry = try {
            archive.getEntry(pathInZIP)
        } catch (e: Exception) {
            throw LCPError.licenseContainer
        }

        return archive.getInputStream(entry).readBytes()

    }

    override fun write(license: LicenseDocument) {
        try {
            val source = File(zip)
            val tmpZip = File("$zip.tmp")
            tmpZip.delete()
            source.copyTo(tmpZip)
            source.delete()
            if (ZipUtil.containsEntry(tmpZip, pathInZIP)) {
                ZipUtil.removeEntry(tmpZip, pathInZIP)
            }
            ZipUtil.addEntry(tmpZip, pathInZIP, license.data, source)
            tmpZip.delete()
        } catch (e: Exception) {
            throw LCPError.licenseContainer
        }
    }
}
