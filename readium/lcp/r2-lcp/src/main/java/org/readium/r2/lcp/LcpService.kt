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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.readium.r2.lcp.persistence.Database
import org.readium.r2.lcp.service.*
import org.readium.r2.shared.util.Try
import java.io.File

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
     * Returns if the publication is protected by LCP.
     */
    suspend fun isLcpProtected(file: File): Boolean

    /**
     *  Imports a protected publication from a standalone LCPL file.
     */
    suspend fun importPublication(lcpl: ByteArray): Try<ImportedPublication, LcpException>

    /**
     * Opens the LCP license of a protected publication, to access its DRM metadata and decipher
     * its content.
     *
     * @param allowUserInteraction Indicates whether the user can be prompted for their passphrase.
     * @param sender Free object that can be used by reading apps to give some UX context when
     *        presenting dialogs.
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


    @Deprecated("Use `importPublication()` with coroutines instead", ReplaceWith("importPublication(lcpl)"))
    fun importPublication(lcpl: ByteArray, authentication: LcpAuthenticating?, completion: (ImportedPublication?, LcpException?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            importPublication(lcpl)
                .onSuccess { completion(it, null) }
                .onFailure { completion(null, it) }
        }
    }

    @Deprecated("Use `retrieveLicense()` with coroutines instead", ReplaceWith("retrieveLicense(File(publication), authentication, allowUserInteraction = true)"))
    fun retrieveLicense(publication: String, authentication: LcpAuthenticating?, completion: (LcpLicense?, LcpException?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val result = retrieveLicense(File(publication), authentication, allowUserInteraction = true)
            if (result == null) {
                completion(null, null)
            } else {
                result
                    .onSuccess { completion(it, null) }
                    .onFailure { completion(null, it) }
            }
        }

    }

}


@Deprecated("Renamed to `LcpService.create()`", replaceWith = ReplaceWith("LcpService.create"))
fun R2MakeLCPService(context: Context): LcpService =
    LcpService.create(context)

@Deprecated("Renamed to `LcpService.ImportedPublication`", replaceWith = ReplaceWith("LcpService.ImportedPublication"))
typealias LCPImportedPublication = LcpService.ImportedPublication
