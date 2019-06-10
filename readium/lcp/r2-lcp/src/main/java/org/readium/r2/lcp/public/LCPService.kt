// TODO download
/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.public

import android.content.Context
import org.joda.time.DateTime
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.persistence.Database
import org.readium.r2.lcp.service.*
import org.readium.r2.shared.drm.DRMLicense
import java.net.URL


interface LCPService {
    fun importPublication(lcpl: ByteArray, authentication: LCPAuthenticating?, completion: (LCPImportedPublication?, LCPError?) -> Unit)
    fun retrieveLicense(publication: String, authentication: LCPAuthenticating?, completion: (LCPLicense?, LCPError?) -> Unit)
}

data class LCPImportedPublication(
        val localURL: String,
        val suggestedFilename: String) {}


typealias URLPresenter = (URL, dismissed: () -> Unit) -> Unit

interface LCPLicense : DRMLicense {
    val license: LicenseDocument
    val status: StatusDocument?
    val charactersToCopyLeft: Int?
    val pagesToPrintLeft: Int?
    val canPrint: Boolean
    fun print(pagesCount: Int): Boolean
    val canRenewLoan: Boolean
    val maxRenewDate: DateTime?
    fun renewLoan(end: DateTime?, present: URLPresenter, completion: (LCPError?) -> Unit)
    val canReturnPublication: Boolean
    fun returnPublication(completion: (LCPError?) -> Unit)
}

fun R2MakeLCPService(context: Context): LCPService {
    val db = Database(context)
    val network = NetworkService()
    val device = DeviceService(repository = db.licenses, network = network, context = context)
    val crl = CRLService(network = network, context = context)
    val passphrases = PassphrasesService(repository = db.transactions)
    val licenses = LicensesService(licenses = db.licenses, crl = crl, device = device, network = network, passphrases = passphrases, context = context)
    return licenses
}
