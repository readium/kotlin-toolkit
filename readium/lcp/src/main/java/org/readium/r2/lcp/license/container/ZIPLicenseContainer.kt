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
import java.util.zip.ZipFile
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.util.Url
import org.zeroturnaround.zip.ZipUtil

/**
 * Access to a License Document stored in a ZIP archive.
 */
internal class ZIPLicenseContainer(
    private val zip: File,
    private val pathInZIP: Url
) : LicenseContainer {

    override fun read(): ByteArray {
        val archive = try {
            ZipFile(zip)
        } catch (e: Exception) {
            throw LcpException.Container.OpenFailed
        }
        val entry = try {
            archive.getEntry(pathInZIP.toString())
        } catch (e: Exception) {
            throw LcpException.Container.FileNotFound(pathInZIP)
        }

        return try {
            archive.getInputStream(entry).readBytes()
        } catch (e: Exception) {
            throw LcpException.Container.ReadFailed(pathInZIP)
        }
    }

    override fun write(license: LicenseDocument) {
        try {
            val path = pathInZIP.toString()
            val tmpZip = File("${zip.path}.tmp")
            tmpZip.delete()
            zip.copyTo(tmpZip)
            zip.delete()
            if (ZipUtil.containsEntry(tmpZip, path)) {
                ZipUtil.removeEntry(tmpZip, path)
            }
            ZipUtil.addEntry(tmpZip, path, license.data, zip)
            tmpZip.delete()
        } catch (e: Exception) {
            throw LcpException.Container.WriteFailed(pathInZIP)
        }
    }
}
