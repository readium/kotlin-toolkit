/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import android.content.Context
import org.joda.time.DateTime
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.persistence.Database
import org.readium.r2.lcp.service.*
import org.readium.r2.shared.util.Try
import java.io.File
import java.io.Serializable
import java.net.URL

/**
 * Service used to fulfill and access protected publications.
 *
 * If an LCPAuthenticating instance is not given when expected, the request is cancelled if no
 * passphrase is found in the local database. This can be the desired behavior when trying to
 * import a license in the background, without prompting the user for its passphrase.
 */
interface LcpService {

    companion object {

        /**
         * LCP service factory.
         */
        fun create(context: Context): LcpService {
            val db = Database(context)
            val network = NetworkService()
            val device = DeviceService(repository = db.licenses, network = network, context = context)
            val crl = CRLService(network = network, context = context)
            val passphrases = PassphrasesService(repository = db.transactions)
            return LicensesService(licenses = db.licenses, crl = crl, device = device, network = network, passphrases = passphrases, context = context)
        }

    }

    /**
     * Returns if the publication is protected by Lcp.
     */
    suspend fun isLcpProtected(file: File): Boolean

    /**
     *  Imports a protected publication from a standalone LCPL file.
     */
    suspend fun importPublication(lcpl: ByteArray): Try<ImportedPublication, LcpException>

    /**
     * Opens the LCP license of a protected publication, to access its DRM metadata and decipher
     * its content.
     */
    suspend fun retrieveLicense(file: File, authentication: LcpAuthenticating?, allowUserInteraction: Boolean, sender: Any? = null): Try<LcpLicense, LcpException>?

    /**
     * Informations about a downloaded publication.
     *
     * @param localURL Path to the downloaded publication. You must move this file to the user
     *        library's folder.
     * @param suggestedFilename Filename that should be used for the publication when importing it in
     *        the user library.
     */
    data class ImportedPublication(
        val localURL: String,
        val suggestedFilename: String
    )
}

/**
 * Opened license, used to decipher a protected publication and manage its license.
 */
interface LcpLicense : Serializable {

    val license: LicenseDocument
    val status: StatusDocument?

    /**
     * Number of remaining characters allowed to be copied by the user. If [null], there's no limit.
     */
    val charactersToCopyLeft: Int?

    /**
     * Number of pages allowed to be printed by the user. If [null], there's no limit.
     */
    val pagesToPrintLeft: Int?

    /**
     * Returns whether the user is allowed to print pages of the publication.
     */
    val canPrint: Boolean

    /**
     * Requests to print the given number of pages.
     *
     * The caller is responsible to perform the actual print. This method is only used to know if
     * the action is allowed.
     *
     * @return Whether the user is allowed to print that many pages.
     */
    fun print(pagesCount: Int): Boolean

    val canCopy: Boolean

    fun copy(text: String): String?

    /**
     * Can the user renew the loaned publication?
     */
    val canRenewLoan: Boolean

    /**
     * The maximum potential date to renew to.
     * If [null], then the renew date might not be customizable.
     */
    val maxRenewDate: DateTime?

    /**
     * Renews the loan up to a certain date (if possible).
     *
     * @param urlPresenter: Used when the renew requires to present an HTML page to the user.
     */
    suspend fun renewLoan(end: DateTime?, urlPresenter: suspend (URL) -> Unit): Try<Unit, LcpException>

    /**
     * Can the user return the loaned publication?
     */
    val canReturnPublication: Boolean

    /**
     * Returns the publication to its provider.
     */
    suspend fun returnPublication(): Try<Unit, LcpException>

    suspend fun decrypt(data: ByteArray): Try<ByteArray, LcpException>
}

