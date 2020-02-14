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

internal interface LicensesRepository {
    fun addLicense(license: LicenseDocument)
    fun copiesLeft(licenseId: String) : Int?
    fun setCopiesLeft(quantity: Int, licenseId: String)
    fun printsLeft(licenseId: String) : Int?
    fun setPrintsLeft(quantity: Int, licenseId: String)
}
