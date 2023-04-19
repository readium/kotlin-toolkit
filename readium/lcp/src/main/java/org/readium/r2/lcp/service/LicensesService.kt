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
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.auth.LcpDumbAuthentication
import org.readium.r2.lcp.license.License
import org.readium.r2.lcp.license.LicenseValidation
import org.readium.r2.lcp.license.container.LicenseContainer
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.asset.RemoteAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

internal class LicensesService(
    private val licenses: LicensesRepository,
    private val crl: CRLService,
    private val device: DeviceService,
    private val network: NetworkService,
    private val passphrases: PassphrasesService,
    private val context: Context,
    private val archiveFactory: ArchiveFactory,
    private val httpClient: HttpClient
) : LcpService, CoroutineScope by MainScope() {

    override suspend fun isLcpProtected(file: File): Boolean =
        tryOr(false) {
            createLicenseContainer(file.path).read()
            true
        }

    override suspend fun remoteAssetForLicense(license: File): Try<PublicationAsset, LcpException> {
        return try {
            Try.success(remoteAssetForLicenseThrowing(license))
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }
    }

    private suspend fun remoteAssetForLicenseThrowing(licenseFile: File): PublicationAsset {
        // Update the license file to get a fresh publication URL.
        val license = retrieveLicense(licenseFile, LcpDumbAuthentication(), false)
            .getOrNull()

        val licenseDoc = license?.license
            ?: LicenseDocument(licenseFile.readBytes())

        val link = checkNotNull(licenseDoc.link(LicenseDocument.Rel.publication))
        val url = try {
            Url(link.url.toString()) ?: throw IllegalStateException()
        } catch (e: Exception) {
            throw LcpException.Parsing.Url(rel = LicenseDocument.Rel.publication.rawValue)
        }
        val baseAsset = RemoteAsset.Factory(archiveFactory, httpClient)
            .createAsset(url, link.mediaType)
            .getOrThrow()

        return LcpLicensedAsset(
            baseAsset.name,
            baseAsset.mediaType,
            baseAsset.fetcher,
            licenseFile,
            license
        )
    }

    override suspend fun acquirePublication(lcpl: ByteArray, onProgress: (Double) -> Unit): Try<LcpService.AcquiredPublication, LcpException> =
        try {
            val licenseDocument = LicenseDocument(lcpl)
            Timber.d("license ${licenseDocument.json}")
            fetchPublication(licenseDocument, onProgress).let { Try.success(it) }
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }

    override suspend fun retrieveLicense(
        file: File,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<LcpLicense, LcpException> =
        try {
            val container = createLicenseContainer(file.path)
            val license = retrieveLicense(container, authentication, allowUserInteraction, true, sender)
            Try.success(license)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }

    override suspend fun retrieveLicense(
        fetcher: Fetcher,
        mediaType: MediaType,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<LcpLicense, LcpException> =
        try {
            val container = createLicenseContainer(fetcher, mediaType)
            val license = retrieveLicense(container, authentication, allowUserInteraction, false, sender)
            Try.success(license)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }

    private suspend fun retrieveLicense(
        container: LicenseContainer,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean,
        ignoreInternetErrors: Boolean,
        sender: Any?
    ): LcpLicense {
        // WARNING: Using the Default dispatcher in the state machine code is critical. If we were using the Main Dispatcher,
        // calling runBlocking in LicenseValidation.handle would block the main thread and cause a severe issue
        // with LcpAuthenticating.retrievePassphrase. Specifically, the interaction of runBlocking and suspendCoroutine
        // blocks the current thread before the passphrase popup has been showed until some button not yet showed is clicked.
        val license = withContext(Dispatchers.Default) {
            retrieveLicenseUnsafe(
                container,
                authentication,
                allowUserInteraction,
                ignoreInternetErrors,
                sender
            )
        }
        Timber.d("license retrieved ${license.license}")

        return license
    }

    private suspend fun retrieveLicenseUnsafe(
        container: LicenseContainer,
        authentication: LcpAuthenticating?,
        allowUserInteraction: Boolean,
        ignoreInternetErrors: Boolean,
        sender: Any?
    ): License =
        suspendCancellableCoroutine { cont ->
            retrieveLicense(
                container,
                authentication,
                allowUserInteraction,
                ignoreInternetErrors,
                sender
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
        ignoreInternetErrors: Boolean,
        sender: Any?,
        completion: (License) -> Unit
    ) {

        var initialData = container.read()
        Timber.d("license ${LicenseDocument(data = initialData).json}")

        val validation = LicenseValidation(
            authentication = authentication, crl = this.crl,
            device = this.device, network = this.network, passphrases = this.passphrases, context = this.context,
            allowUserInteraction = allowUserInteraction, ignoreInternetErrors = ignoreInternetErrors,
            sender = sender
        ) { licenseDocument ->
            try {
                launch {
                    this@LicensesService.licenses.addLicense(licenseDocument)
                }
            } catch (error: Error) {
                Timber.d("Failed to add the LCP License to the local database: $error")
            }
            if (!licenseDocument.data.contentEquals(initialData)) {
                try {
                    container.write(licenseDocument)
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
                    completion(License(documents = it, validation = validation, licenses = this.licenses, device = this.device, network = this.network))
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

    private suspend fun fetchPublication(license: LicenseDocument, onProgress: (Double) -> Unit): LcpService.AcquiredPublication {
        val link = license.link(LicenseDocument.Rel.publication)
        val url = link?.url
            ?: throw LcpException.Parsing.Url(rel = LicenseDocument.Rel.publication.rawValue)

        val destination = withContext(Dispatchers.IO) {
            File.createTempFile("lcp-${System.currentTimeMillis()}", ".tmp")
        }
        Timber.i("LCP destination $destination")

        val mediaType = network.download(url, destination, mediaType = link.type, onProgress = onProgress) ?: MediaType.of(mediaType = link.type) ?: MediaType.EPUB

        // Saves the License Document into the downloaded publication
        val container = createLicenseContainer(destination.path, mediaType)
        container.write(license)

        return LcpService.AcquiredPublication(
            localFile = destination,
            suggestedFilename = "${license.id}.${mediaType.fileExtension}"
        )
    }
}
