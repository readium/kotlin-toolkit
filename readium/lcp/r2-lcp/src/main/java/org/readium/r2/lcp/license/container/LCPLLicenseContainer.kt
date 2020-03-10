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
import org.readium.r2.lcp.license.model.LicenseDocument
import org.zeroturnaround.zip.ZipUtil
import timber.log.Timber
import java.io.File
import java.net.URL

/**
 * Access a License Document stored in an LCPDF archive.
 */
internal class LCPLLicenseContainer(private val lcpl: String? = null, private val byteArray: ByteArray? = null) : LicenseContainer {

    var publication: String? = null

    override fun read() : ByteArray {
        return lcpl?.let {
            URL(Uri.parse(it).toString()).openStream().readBytes()
        } ?: run {
            byteArray?.let {
                it
            } ?:run {
                ByteArray(0)
            }
        }
    }

    override fun write(license: LicenseDocument) {
        publication?.let {
            val pathInZip = "META-INF/license.lcpl"
            if (DEBUG) Timber.i("LCP moveLicense")
            val source = File(publication)
            val tmpZip = File("$publication.tmp")
            tmpZip.delete()
            source.copyTo(tmpZip)
            source.delete()
            if (ZipUtil.containsEntry(tmpZip, pathInZip)) {
                ZipUtil.removeEntry(tmpZip, pathInZip)
            }
            ZipUtil.addEntry(tmpZip, pathInZip, license.data, source)
            tmpZip.delete()
        }
    }

}

