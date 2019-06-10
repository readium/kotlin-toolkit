//TODO double check this, there is no write function needed here

/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readium.r2.lcp.license.container

import android.net.Uri
import org.readium.r2.lcp.license.model.LicenseDocument
import java.net.URL

class LCPLLicenseContainer(private val lcpl: String? = null, private val byteArray: ByteArray? = null) : LicenseContainer {

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
//        val file = File(lcpl)
//        file.writeBytes(license.data)
    }
}

