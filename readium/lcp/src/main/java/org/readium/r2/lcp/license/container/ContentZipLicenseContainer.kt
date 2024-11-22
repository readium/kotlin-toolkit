/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.container

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipFile
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.toUri

internal class ContentZipLicenseContainer(
    context: Context,
    private val container: Container<Resource>,
    private val pathInZip: Url,
) : LicenseContainer by ContainerLicenseContainer(container, pathInZip), WritableLicenseContainer {

    private val zipUri: Uri =
        requireNotNull(container.sourceUrl).toUri()

    private val contentResolver: ContentResolver =
        context.contentResolver

    private val cache: File =
        context.externalCacheDir ?: context.cacheDir

    override fun write(license: LicenseDocument) {
        try {
            val tmpZip = File(cache, UUID.randomUUID().toString())
            contentResolver.openInputStream(zipUri)
                ?.use { it.copyTo(FileOutputStream(tmpZip)) }
                ?: throw LcpException(LcpError.Container.WriteFailed(pathInZip))
            val tmpZipFile = ZipFile(tmpZip)

            val outStream = contentResolver.openOutputStream(zipUri, "wt")
                ?: throw LcpException(LcpError.Container.WriteFailed(pathInZip))
            tmpZipFile.addOrReplaceEntry(
                pathInZip.toString(),
                ByteArrayInputStream(license.toByteArray()),
                outStream
            )

            outStream.close()
            tmpZipFile.close()
            tmpZip.delete()
        } catch (e: Exception) {
            throw LcpException(LcpError.Container.WriteFailed(pathInZip))
        }
    }
}
