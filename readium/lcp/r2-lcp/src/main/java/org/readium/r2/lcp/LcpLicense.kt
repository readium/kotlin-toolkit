/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import kotlinx.coroutines.*
import org.joda.time.DateTime
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.net.URL
import kotlin.coroutines.resume

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
    val maxRenewDate: DateTime?

    /**
     * Renews the loan up to a certain date (if possible).
     *
     * @param urlPresenter: Used when the renew requires to present an HTML page to the user.
     *        A reading app should implement it with a `suspendCoroutine` statement to return the
     *        control once the user dismissed the browser view.
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

    /**
     * Decrypts the given [data] encrypted with the license's content key.
     */
    suspend fun decrypt(data: ByteArray): Try<ByteArray, LcpException>


    @Deprecated("Use `license.encryption.profile` instead", ReplaceWith("license.encryption.profile"))
    val encryptionProfile: String? get() =
        license.encryption.profile

    @Deprecated("Use `decrypt()` with coroutines instead", ReplaceWith("decrypt(data)"))
    fun decipher(data: ByteArray): ByteArray? =
        runBlocking { decrypt(data) }
            .onFailure { Timber.e(it) }
            .getOrNull()

    @Deprecated("Use `renewLoan()` with coroutines instead", ReplaceWith("renewLoan)"))
    fun renewLoan(end: DateTime?, present: (URL, dismissed: () -> Unit) -> Unit, completion: (LcpException?) -> Unit) {
        GlobalScope.launch {
            suspend fun presentUrl(url: URL) {
                suspendCancellableCoroutine<Unit> { cont ->
                    present(url) {
                        if (cont.isActive) {
                            cont.resume(Unit)
                        }
                    }
                }
            }

            val result = renewLoan(end, ::presentUrl)
            completion(result.exceptionOrNull())
        }
    }

    @Deprecated("Use `returnPublication()` with coroutines instead", ReplaceWith("returnPublication"))
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
