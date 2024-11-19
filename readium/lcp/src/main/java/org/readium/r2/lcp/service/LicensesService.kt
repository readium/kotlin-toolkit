/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.service

import android.content.Context
import java.io.File
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.LcpContentProtection
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.license.License
import org.readium.r2.lcp.license.LicenseValidation
import org.readium.r2.lcp.license.container.LicenseContainer
import org.readium.r2.lcp.license.container.WritableLicenseContainer
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.util.sha256
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

internal class LicensesService(
    private val licenses: LicensesRepository,
    private val crl: CRLService,
    private val device: DeviceService,
    private val network: NetworkService,
    private val passphrases: PassphrasesService,
    private val context: Context,
    private val assetRetriever: AssetRetriever,
) : LcpService, CoroutineScope by MainScope() {

    override fun contentProtection(
        authentication: LcpAuthenticating,
    ): ContentProtection =
        LcpContentProtection(this, authentication, assetRetriever)

    override suspend fun injectLicenseDocument(
        licenseDocument: LicenseDocument,
        publicationFile: File,
    ): Try<Unit, LcpError> {
        val hashIsCorrect = licenseDocument.publicationLink.hash
            ?.let { publicationFile.checkSha256(it) }

        if (hashIsCorrect == false) {
            return Try.failure(
                LcpError.Network(Exception("Digest mismatch: download looks corrupted."))
            )
        }

        val mediaType = licenseDocument.publicationLink.mediaType
        val format = assetRetriever.sniffFormat(publicationFile, FormatHints(mediaType))
            .getOrElse {
                Format(
                    specification = FormatSpecification(
                        Specification.Zip,
                        Specification.Epub,
                        Specification.Lcp
                    ),
                    mediaType = MediaType.EPUB,
                    fileExtension = FileExtension("epub")
                )
            }

        return withContext(Dispatchers.IO) {
            try {
                val container = createLicenseContainer(publicationFile, format.specification)
                container.write(licenseDocument)
                Try.success(Unit)
            } catch (e: Exception) {
                Try.failure(LcpError.wrap(e))
            }
        }
    }

    override suspend fun acquirePublication(
        lcpl: File,
        onProgress: (Double) -> Unit,
    ): Try<LcpService.AcquiredPublication, LcpError> {
        coroutineContext.ensureActive()
        val bytes = try {
            lcpl.readBytes()
        } catch (e: Exception) {
            return Try.failure(LcpError.wrap(e))
        }

        return acquirePublication(bytes, onProgress)
    }

    override suspend fun acquirePublication(
        lcpl: ByteArray,
        onProgress: (Double) -> Unit,
    ): Try<LcpService.AcquiredPublication, LcpError> {
        val destination =
            try {
                withContext(Dispatchers.IO) {
                    File.createTempFile("lcp-${System.currentTimeMillis()}", ".tmp")
                }
            } catch (e: Exception) {
                return Try.failure(LcpError.wrap(e))
            }

        return try {
            val licenseDocument = LicenseDocument(lcpl)
            Timber.d("license ${licenseDocument.json}")
            fetchPublication(licenseDocument, destination, onProgress).let { Try.success(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            tryOrLog { destination.delete() }
            Try.failure(LcpError.wrap(e))
        }
    }

    private suspend fun fetchPublication(
        license: LicenseDocument,
        destination: File,
        onProgress: (Double) -> Unit,
    ): LcpService.AcquiredPublication {
        val link = license.link(LicenseDocument.Rel.Publication)!!
        val url = link.url()

        Timber.i("LCP destination $destination")

        val serverMediaType = network.download(
            url,
            destination,
            mediaType = link.mediaType,
            onProgress = onProgress
        )

        val hashIsCorrect = license.publicationLink.hash
            ?.let { destination.checkSha256(it) }

        if (hashIsCorrect == false) {
            throw LcpException(
                LcpError.Network(Exception("Digest mismatch: download looks corrupted."))
            )
        }

        val format =
            assetRetriever.sniffFormat(
                destination,
                FormatHints(
                    mediaTypes = listOfNotNull(
                        license.publicationLink.mediaType ?: serverMediaType
                    )
                )
            ).getOrElse {
                when (it) {
                    is AssetRetriever.RetrieveError.Reading -> {
                        tryOrLog { destination.delete() }
                        throw LcpException(LcpError.wrap(ErrorException(it)))
                    }

                    is AssetRetriever.RetrieveError.FormatNotSupported -> {
                        Format(
                            specification = FormatSpecification(
                                Specification.Zip,
                                Specification.Epub,
                                Specification.Lcp
                            ),
                            mediaType = MediaType.EPUB,
                            fileExtension = FileExtension("epub")
                        )
                    }
                }
            }

        // Saves the License Document into the downloaded publication
        val container = createLicenseContainer(destination, format.specification)
        container.write(license)

        return LcpService.AcquiredPublication(
            localFile = destination,
            suggestedFilename = "${license.id}.${format.fileExtension.value}",
            format = format,
            licenseDocument = license
        )
    }

    /**
     * Checks that the sha256 sum of file content matches the expected one.
     * Returns null if we can't decide.
     */
    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    private fun File.checkSha256(expected: String): Boolean? {
        val actual = sha256() ?: return null

        // Supports hexadecimal encoding for compatibility.
        // See https://github.com/readium/lcp-specs/issues/52
        return when (expected.length) {
            44 -> Base64.encode(actual) == expected
            64 -> actual.toHexString() == expected
            else -> null
        }
    }

    override suspend fun retrieveLicense(
        asset: Asset,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean,
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

    override suspend fun retrieveLicenseDocument(
        asset: ContainerAsset,
    ): Try<LicenseDocument, LcpError> =
        withContext(Dispatchers.IO) {
            try {
                val licenseContainer = createLicenseContainer(context, asset)
                val licenseData = licenseContainer.read()
                Try.success(LicenseDocument(licenseData))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Try.failure(LcpError.wrap(e))
            }
        }

    private suspend fun retrieveLicense(
        container: LicenseContainer,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean,
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
        allowUserInteraction: Boolean,
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
        completion: (License) -> Unit,
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
                throw LcpException(LcpError.MissingPassphrase)
            }
        }
    }
}
