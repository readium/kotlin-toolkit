/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import android.content.Context
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.LcpContentProtection
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpPublicationRetriever
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.license.License
import org.readium.r2.lcp.license.LicenseValidation
import org.readium.r2.lcp.license.container.LicenseContainer
import org.readium.r2.lcp.license.container.WritableLicenseContainer
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetOpener
import org.readium.r2.shared.util.downloads.DownloadManager
import timber.log.Timber

internal class LicensesService(
    private val licenses: LicensesRepository,
    private val crl: CRLService,
    private val device: DeviceService,
    private val network: NetworkService,
    private val passphrases: PassphrasesService,
    private val context: Context,
    private val assetOpener: AssetOpener,
    private val downloadManager: DownloadManager
) : LcpService, CoroutineScope by MainScope() {

    override fun contentProtection(
        authentication: LcpAuthenticating
    ): ContentProtection =
        LcpContentProtection(this, authentication, assetOpener)

    override fun publicationRetriever(): LcpPublicationRetriever {
        return LcpPublicationRetriever(
            context,
            downloadManager,
            assetOpener.assetSniffer
        )
    }

    override suspend fun retrieveLicense(
        asset: Asset,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean
    ): Try<LcpLicense, LcpError> =
        try {
            val licenseContainer = createLicenseContainer(context, asset)
            val license = retrieveLicense(
                licenseContainer,
                authentication,
                allowUserInteraction
            )
            Try.success(license)
        } catch (e: Exception) {
            Try.failure(LcpError.wrap(e))
        }

    private suspend fun retrieveLicense(
        container: LicenseContainer,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean
    ): LcpLicense {
        // WARNING: Using the Default dispatcher in the state machine code is critical. If we were using the Main Dispatcher,
        // calling runBlocking in LicenseValidation.handle would block the main thread and cause a severe issue
        // with LcpAuthenticating.retrievePassphrase. Specifically, the interaction of runBlocking and suspendCoroutine
        // blocks the current thread before the passphrase popup has been showed until some button not yet showed is clicked.
        val license = withContext(Dispatchers.Default) {
            retrieveLicenseUnsafe(
                container,
                authentication,
                allowUserInteraction
            )
        }
        Timber.d("license retrieved ${license.license}")

        return license
    }

    private suspend fun retrieveLicenseUnsafe(
        container: LicenseContainer,
        authentication: LcpAuthenticating?,
        allowUserInteraction: Boolean
    ): License =
        suspendCancellableCoroutine { cont ->
            retrieveLicense(
                container,
                authentication,
                allowUserInteraction
            ) { license ->
                if (cont.isActive) {
                    cont.resume(license)
                }
            }
        }

    private fun retrieveLicense(
        container: LicenseContainer,
        authentication: LcpAuthenticating?,
        allowUserInteraction: Boolean,
        completion: (License) -> Unit
    ) {
        var initialData = container.read()
        Timber.d("license ${LicenseDocument(data = initialData).json}")

        val validation = LicenseValidation(
            authentication = authentication,
            crl = this.crl,
            device = this.device,
            network = this.network,
            passphrases = this.passphrases,
            context = this.context,
            allowUserInteraction = allowUserInteraction,
            ignoreInternetErrors = container is WritableLicenseContainer
        ) { licenseDocument ->
            try {
                launch {
                    this@LicensesService.licenses.addLicense(licenseDocument)
                }
            } catch (error: Error) {
                Timber.d("Failed to add the LCP License to the local database: $error")
            }
            if (!licenseDocument.toByteArray().contentEquals(initialData)) {
                try {
                    (container as? WritableLicenseContainer)
                        ?.let { container.write(licenseDocument) }

                    Timber.d("licenseDocument ${licenseDocument.json}")

                    initialData = container.read()
                    Timber.d("license ${LicenseDocument(data = initialData).json}")
                    Timber.d("Wrote updated License Document in container")
                } catch (error: Error) {
                    Timber.d("Failed to write updated License Document in container: $error")
                }
            }
        }

        validation.validate(LicenseValidation.Document.license(initialData)) { documents, error ->
            documents?.let {
                Timber.d("validated documents $it")
                try {
                    documents.getContext()
                    launch {
                        completion(
                            License(
                                documents = it,
                                validation = validation,
                                licenses = this@LicensesService.licenses,
                                device = this@LicensesService.device,
                                network = this@LicensesService.network
                            )
                        )
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
            error?.let { throw error }

            // Both error and documents can be null if the user cancelled the passphrase prompt.
            if (documents == null) {
                throw CancellationException("License validation was interrupted.")
            }
        }
    }
}
