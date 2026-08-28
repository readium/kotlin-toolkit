/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.util.Url

/**
 * Access to a License Document stored in a ZIP archive.
 */
internal class FileZipLicenseContainer(
    private val zip: String,
    private val pathInZIP: Url,
) : WritableLicenseContainer {

    override fun read(): ByteArray {
        val archive = try {
            ZipFile(zip)
        } catch (e: Exception) {
            throw LcpException(LcpError.Container.OpenFailed)
        }

        val entry = try {
            archive.getEntry(pathInZIP.toString())!!
        } catch (e: Exception) {
            archive.close()
            throw LcpException(LcpError.Container.FileNotFound(pathInZIP))
        }

        return try {
            archive.getInputStream(entry).readBytes()
        } catch (e: Exception) {
            throw LcpException(LcpError.Container.ReadFailed(pathInZIP))
        } finally {
            archive.close()
        }
    }

    override fun write(license: LicenseDocument) {
        try {
            val source = File(zip)
            val tmpZip = File("$zip.tmp")
            val zipFile = ZipFile(source)
            try {
                zipFile.addOrReplaceEntry(
                    pathInZIP.toString(),
                    ByteArrayInputStream(license.toByteArray()),
                    tmpZip
                )
            } finally {
                zipFile.close()
            }
            tmpZip.moveTo(source)
        } catch (e: Exception) {
            throw LcpException(LcpError.Container.WriteFailed(pathInZIP))
        }
    }
}
