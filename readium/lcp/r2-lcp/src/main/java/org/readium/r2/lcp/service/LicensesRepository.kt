/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.persistence.LcpDao
import org.readium.r2.lcp.persistence.License

internal class LicensesRepository(private val lcpDao: LcpDao) {

    suspend fun addLicense(licenseDocument: LicenseDocument) {
        if (lcpDao.exists(licenseDocument.id) != null) {
            return
        }
        val license = License(
            licenseId = licenseDocument.id,
            rightPrint = licenseDocument.rights.print,
            rightCopy = licenseDocument.rights.copy
        )
        lcpDao.addLicense(license)
    }

    fun copiesLeft(licenseId: String): Int? {
        return lcpDao.getCopiesLeft(licenseId)
    }

    fun setCopiesLeft(quantity: Int, licenseId: String) {
        lcpDao.setCopiesLeft(quantity, licenseId)
    }

    fun printsLeft(licenseId: String) : Int? {
        return lcpDao.getPrintsLeft(licenseId)
    }

    fun setPrintsLeft(quantity: Int, licenseId: String) {
        lcpDao.setPrintsLeft(quantity, licenseId)
    }
}
