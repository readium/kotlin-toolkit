/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license

import java.net.HttpURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.LcpClient
import org.readium.r2.lcp.service.LicensesRepository
import org.readium.r2.lcp.service.NetworkService
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.Instant
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

internal class License private constructor(
    private val coroutineScope: CoroutineScope,
    private var documents: ValidatedDocuments,
    private val validation: LicenseValidation,
    private val licenses: LicensesRepository,
    private val device: DeviceService,
    private val network: NetworkService,
    private val printsLeft: StateFlow<Int?>,
    private val copiesLeft: StateFlow<Int?>,
) : LcpLicense {

    companion object {

        suspend operator fun invoke(
            documents: ValidatedDocuments,
            validation: LicenseValidation,
            licenses: LicensesRepository,
            device: DeviceService,
            network: NetworkService,
        ): License {
            val coroutineScope = MainScope()

            val printsLeft = licenses
                .printsLeft(documents.license.id)
                .stateIn(coroutineScope)

            val copiesLeft = licenses
                .copiesLeft(documents.license.id)
                .stateIn(coroutineScope)

            return License(
                coroutineScope = coroutineScope,
                documents = documents,
                validation = validation,
                licenses = licenses,
                device = device,
                network = network,
                printsLeft = printsLeft,
                copiesLeft = copiesLeft
            )
        }
    }

    override val license: LicenseDocument
        get() = documents.license
    override val status: StatusDocument?
        get() = documents.status

    override suspend fun decrypt(data: ByteArray): Try<ByteArray, LcpError> = withContext(
        Dispatchers.Default
    ) {
        try {
            // LCP lib crashes if we call decrypt on an empty ByteArray
            if (data.isEmpty()) {
                Try.success(ByteArray(0))
            } else {
                val context = documents.getContext()
                val decryptedData = LcpClient.decrypt(context, data)
                Try.success(decryptedData)
            }
        } catch (e: Exception) {
            Try.failure(LcpError.wrap(e))
        }
    }

    override val charactersToCopyLeft: StateFlow<Int?>
        get() = copiesLeft

    override val canCopy: Boolean
        get() = (charactersToCopyLeft.value ?: 1) > 0

    override fun canCopy(text: String): Boolean =
        charactersToCopyLeft.value?.let { it <= text.length }
            ?: true

    override suspend fun copy(text: String): Boolean {
        return try {
            licenses.tryCopy(text.length, license.id)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            false
        }
    }

    override val pagesToPrintLeft: StateFlow<Int?> =
        printsLeft

    override val canPrint: Boolean
        get() = (pagesToPrintLeft.value ?: 1) > 0

    override fun canPrint(pageCount: Int): Boolean =
        pagesToPrintLeft.value?.let { it <= pageCount }
            ?: true

    override suspend fun print(pageCount: Int): Boolean {
        return try {
            licenses.tryPrint(pageCount, license.id)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            false
        }
    }

    override val canRenewLoan: Boolean
        get() = status?.link(StatusDocument.Rel.Renew) != null

    override val maxRenewDate: Instant?
        get() = status?.potentialRights?.end

    override suspend fun renewLoan(listener: LcpLicense.RenewListener, prefersWebPage: Boolean): Try<Instant?, LcpError> {
        // Finds the renew link according to `prefersWebPage`.
        fun findRenewLink(): Link? {
            val status = documents.status ?: return null

            val types = mutableListOf(MediaType.HTML, MediaType.XHTML)
            if (prefersWebPage) {
                types.add(MediaType.LCP_STATUS_DOCUMENT)
            } else {
                types.add(0, MediaType.LCP_STATUS_DOCUMENT)
            }

            for (type in types) {
                return status.link(StatusDocument.Rel.Renew, type = type)
                    ?: continue
            }

            // Fallback on the first renew link with no media type set and assume it's a PUT action.
            return status.linkWithNoType(StatusDocument.Rel.Renew)
        }

        // Programmatically renew the loan with a PUT request.
        suspend fun renewProgrammatically(link: Link): ByteArray {
            val endDate =
                if (link.href.parameters?.contains("end") == true) {
                    listener.preferredEndDate(maxRenewDate)
                } else {
                    null
                }

            val parameters = this.device.asQueryParameters.toMutableMap()
            if (endDate != null) {
                parameters["end"] = endDate.toString()
            }

            val url = link.url(parameters = parameters)

            return network.fetch(url.toString(), NetworkService.Method.PUT)
                .getOrElse { error ->
                    when (error.status) {
                        HttpURLConnection.HTTP_BAD_REQUEST ->
                            throw LcpException(LcpError.Renew.RenewFailed)
                        HttpURLConnection.HTTP_FORBIDDEN ->
                            throw LcpException(
                                LcpError.Renew.InvalidRenewalPeriod(
                                    maxRenewDate = this.maxRenewDate
                                )
                            )
                        else ->
                            throw LcpException(LcpError.Renew.UnexpectedServerError)
                    }
                }
        }

        // Renew the loan by presenting a web page to the user.
        suspend fun renewWithWebPage(link: Link): ByteArray {
            // The reading app will open the URL in a web view and return when it is dismissed.
            listener.openWebPage(link.url())

            val statusURL = tryOrNull {
                license.url(
                    LicenseDocument.Rel.Status,
                    preferredType = MediaType.LCP_STATUS_DOCUMENT
                )
            } ?: throw LcpException(LcpError.LicenseInteractionNotAvailable)

            return network.fetch(
                statusURL.toString(),
                headers = mapOf("Accept" to MediaType.LCP_STATUS_DOCUMENT.toString())
            ).getOrThrow()
        }

        try {
            val link = findRenewLink()
                ?: throw LcpException(LcpError.LicenseInteractionNotAvailable)

            val data =
                if (link.mediaType?.isHtml == true) {
                    renewWithWebPage(link)
                } else {
                    renewProgrammatically(link)
                }

            validateStatusDocument(data)

            return Try.success(documents.license.rights.end)
        } catch (e: CancellationException) {
            // Passthrough for cancelled coroutines
            throw e
        } catch (e: Exception) {
            return Try.failure(LcpError.wrap(e))
        }
    }

    override val canReturnPublication: Boolean
        get() = status?.link(StatusDocument.Rel.Return) != null

    @OptIn(DelicateReadiumApi::class)
    override suspend fun returnPublication(): Try<Unit, LcpError> {
        try {
            val status = this.documents.status
            val url = try {
                status?.url(
                    StatusDocument.Rel.Return,
                    preferredType = null,
                    parameters = device.asQueryParameters
                )
            } catch (e: Throwable) {
                null
            }
            if (status == null || url == null) {
                throw LcpException(LcpError.LicenseInteractionNotAvailable)
            }

            network.fetch(url.toString(), method = NetworkService.Method.PUT)
                .onSuccess { validateStatusDocument(it) }
                .onFailure { error ->
                    when (error.status) {
                        HttpURLConnection.HTTP_BAD_REQUEST -> throw LcpException(
                            LcpError.Return.ReturnFailed
                        )
                        HttpURLConnection.HTTP_FORBIDDEN -> throw LcpException(
                            LcpError.Return.AlreadyReturnedOrExpired
                        )
                        else -> throw LcpException(LcpError.Return.UnexpectedServerError)
                    }
                }

            return Try.success(Unit)
        } catch (e: Exception) {
            return Try.failure(LcpError.wrap(e))
        }
    }

    private fun validateStatusDocument(data: ByteArray): Unit =
        validation.validate(LicenseValidation.Document.status(data)) { _, _ -> }

    init {
        LicenseValidation.observe(validation) { documents, _ ->
            documents?.let {
                this.documents = documents
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }
}
