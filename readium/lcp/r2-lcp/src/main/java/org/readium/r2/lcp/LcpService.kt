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
import kotlinx.coroutines.*
import org.readium.r2.lcp.auth.LcpDialogAuthentication
import org.readium.r2.lcp.persistence.Database
import org.readium.r2.lcp.service.*
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.util.Try
import java.io.File
import java.lang.Exception

/**
 * Service used to acquire and open publications protected with LCP.
 */
interface LcpService {

    /**
     * Returns if the publication is protected by LCP.
     */
    suspend fun isLcpProtected(file: File): Boolean

    /**
     * Acquires a protected publication from a standalone LCPL's bytes.
     *
     * You can cancel the on-going acquisition by cancelling its parent coroutine context.
     *
     * @param onProgress Callback to follow the acquisition progress from 0.0 to 1.0.
     */
    suspend fun acquirePublication(lcpl: ByteArray, onProgress: (Double) -> Unit = {}): Try<AcquiredPublication, LcpException>

    /**
     * Acquires a protected publication from a standalone LCPL file.
     *
     * You can cancel the on-going acquisition by cancelling its parent coroutine context.
     *
     * @param onProgress Callback to follow the acquisition progress from 0.0 to 1.0.
     */
    suspend fun acquirePublication(lcpl: File, onProgress: (Double) -> Unit = {}): Try<AcquiredPublication, LcpException> = withContext(Dispatchers.IO) {
        try {
            acquirePublication(lcpl.readBytes(), onProgress)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }
    }

    /**
     * Opens the LCP license of a protected publication, to access its DRM metadata and decipher
     * its content.
     *
     * @param authentication Used to retrieve the user passphrase if it is not already known.
     *        The request will be cancelled if no passphrase is found in the LCP passphrase storage
     *        and the provided [authentication].
     * @param allowUserInteraction Indicates whether the user can be prompted for their passphrase.
     * @param sender Free object that can be used by reading apps to give some UX context when
     *        presenting dialogs with [LcpAuthenticating].
     */
    suspend fun retrieveLicense(
        file: File,
        authentication: LcpAuthenticating = LcpDialogAuthentication(),
        allowUserInteraction: Boolean,
        sender: Any? = null
    ): Try<LcpLicense, LcpException>?

    /**
     * Creates a [ContentProtection] instance which can be used with a Streamer to unlock
     * LCP protected publications.
     *
     * The provided [authentication] will be used to retrieve the user passphrase when opening an
     * LCP license. The default implementation [LcpDialogAuthentication] presents a dialog to the
     * user to enter their passphrase.
     */
    fun contentProtection(authentication: LcpAuthenticating = LcpDialogAuthentication()): ContentProtection =
        LcpContentProtection(this, authentication)

    /**
     * Information about an acquired publication protected with LCP.
     *
     * @param localFile Path to the downloaded publication. You must move this file to the user
     *        library's folder.
     * @param suggestedFilename Filename that should be used for the publication when importing it in
     *        the user library.
     */
    data class AcquiredPublication(
        val localFile: File,
        val suggestedFilename: String
    ) {
        @Deprecated("Use `localFile` instead", replaceWith = ReplaceWith("localFile"))
        val localURL: String get() = localFile.path
    }

    companion object {

        /**
         * LCP service factory.
         */
        operator fun invoke(context: Context): LcpService? {
            if (!LcpClient.isAvailable())
                return null

            val db = Database(context)
            val network = NetworkService()
            val device = DeviceService(repository = db.licenses, network = network, context = context)
            val crl = CRLService(network = network, context = context)
            val passphrases = PassphrasesService(repository = db.transactions)
            return LicensesService(licenses = db.licenses, crl = crl, device = device, network = network, passphrases = passphrases, context = context)
        }

        @Deprecated("Use `LcpService()` instead", ReplaceWith("LcpService(context)"), level = DeprecationLevel.ERROR)
        fun create(context: Context): LcpService? = invoke(context)

    }


    @Deprecated("Use `acquirePublication()` with coroutines instead", ReplaceWith("acquirePublication(lcpl)"))
    @DelicateCoroutinesApi
    fun importPublication(lcpl: ByteArray, authentication: LcpAuthenticating?, completion: (AcquiredPublication?, LcpException?) -> Unit) {
        GlobalScope.launch {
            acquirePublication(lcpl)
                .onSuccess { completion(it, null) }
                .onFailure { completion(null, it) }
        }
    }

    @Deprecated("Use `retrieveLicense()` with coroutines instead", ReplaceWith("retrieveLicense(File(publication), authentication, allowUserInteraction = true)"))
    @DelicateCoroutinesApi
    fun retrieveLicense(publication: String, authentication: LcpAuthenticating?, completion: (LcpLicense?, LcpException?) -> Unit) {
        GlobalScope.launch {
            val result = retrieveLicense(File(publication), authentication ?: LcpDialogAuthentication(), allowUserInteraction = true)
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


@Deprecated("Renamed to `LcpService()`", replaceWith = ReplaceWith("LcpService(context)"))
fun R2MakeLCPService(context: Context): LcpService =
    LcpService(context) ?: throw Exception("liblcp is missing on the classpath")

@Deprecated("Renamed to `LcpService.AcquiredPublication`", replaceWith = ReplaceWith("LcpService.AcquiredPublication"))
typealias LCPImportedPublication = LcpService.AcquiredPublication
