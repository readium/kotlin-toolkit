/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.net.URL
import java.util.*

/**
 * Opened license, used to decipher a protected publication and manage its license.
 */
interface LcpLicense : ContentProtectionService.UserRights {

    /**
     * License Document information.
     * https://readium.org/lcp-specs/releases/lcp/latest.html
     */
    val license: LicenseDocument

    /**
     * License Status Document information.
     * https://readium.org/lcp-specs/releases/lsd/latest.html
     */
    val status: StatusDocument?

    /**
     * Number of remaining characters allowed to be copied by the user. If null, there's no limit.
     */
    val charactersToCopyLeft: Int?

    /**
     * Number of pages allowed to be printed by the user. If null, there's no limit.
     */
    val pagesToPrintLeft: Int?

    /**
     * Can the user renew the loaned publication?
     */
    val canRenewLoan: Boolean

    /**
     * The maximum potential date to renew to.
     * If null, then the renew date might not be customizable.
     */
    val maxRenewDate: Date?

    /**
     * Renews the loan by starting a renew LSD interaction.
     *
     * @param prefersWebPage Indicates whether the loan should be renewed through a web page if
     *        available, instead of programmatically.
     */
    suspend fun renewLoan(listener: RenewListener, prefersWebPage: Boolean = false): Try<Date?, LcpException>

    /**
     * Can the user return the loaned publication?
     */
    val canReturnPublication: Boolean

    /**
     * Returns the publication to its provider.
     */
    suspend fun returnPublication(): Try<Unit, LcpException>

    /**
     * Decrypts the given [data] encrypted with the license's content key.
     */
    suspend fun decrypt(data: ByteArray): Try<ByteArray, LcpException>


    /**
     * UX delegate for the loan renew LSD interaction.
     *
     * If your application fits Material Design guidelines, take a look at [MaterialRenewListener]
     * for a default implementation.
     */
    interface RenewListener {

        /**
         * Called when the renew interaction allows to customize the end date programmatically.
         * You can prompt the user for the number of days to renew, for example.
         *
         * The returned date can't exceed [maximumDate].
         */
        suspend fun preferredEndDate(maximumDate: Date?): Date?

        /**
         * Called when the renew interaction uses an HTML web page.
         *
         * You should present the URL in a Chrome Custom Tab and terminate the function when the
         * web page is dismissed by the user.
         */
        suspend fun openWebPage(url: URL)

    }

    @Deprecated("Use `license.encryption.profile` instead", ReplaceWith("license.encryption.profile"))
    val encryptionProfile: String? get() =
        license.encryption.profile

    @Deprecated("Use `decrypt()` with coroutines instead", ReplaceWith("decrypt(data)"))
    fun decipher(data: ByteArray): ByteArray? =
        runBlocking { decrypt(data) }
            .onFailure { Timber.e(it) }
            .getOrNull()

    @Deprecated("Use `renewLoan` with `RenewListener` instead", ReplaceWith("renewLoan(LcpLicense.RenewListener)"), level = DeprecationLevel.ERROR)
    suspend fun renewLoan(end: DateTime?, urlPresenter: suspend (URL) -> Unit): Try<Unit, LcpException> = Try.success(Unit)

    @Deprecated("Use `renewLoan` with `RenewListener` instead", ReplaceWith("renewLoan(LcpLicense.RenewListener)"), level = DeprecationLevel.ERROR)
    fun renewLoan(end: DateTime?, present: (URL, dismissed: () -> Unit) -> Unit, completion: (LcpException?) -> Unit) {}

    @Deprecated("Use `returnPublication()` with coroutines instead", ReplaceWith("returnPublication"))
    @DelicateCoroutinesApi
    fun returnPublication(completion: (LcpException?) -> Unit) {
        GlobalScope.launch {
            completion(returnPublication().exceptionOrNull())
        }
    }

}

@Deprecated("Renamed to `LcpService`", replaceWith = ReplaceWith("LcpService"))
typealias LCPService = LcpService

@Deprecated("Renamed to `LcpLicense`", replaceWith = ReplaceWith("LcpLicense"))
typealias LCPLicense = LcpLicense
