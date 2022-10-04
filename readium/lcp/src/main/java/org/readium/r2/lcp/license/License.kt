/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.LcpClient
import org.readium.r2.lcp.service.LicensesRepository
import org.readium.r2.lcp.service.NetworkService
import org.readium.r2.shared.extensions.toIso8601String
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber
import java.net.HttpURLConnection
import java.util.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class License(
    private var documents: ValidatedDocuments,
    private val validation: LicenseValidation,
    private val licenses: LicensesRepository,
    private val device: DeviceService,
    private val network: NetworkService
) : LcpLicense {

    override val license: LicenseDocument
        get() = documents.license
    override val status: StatusDocument?
        get() = documents.status

    override suspend fun decrypt(data: ByteArray): Try<ByteArray, LcpException> = withContext(Dispatchers.Default) {
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
            Try.failure(LcpException.wrap(e))
        }
    }

    override val charactersToCopyLeft: Int?
        get() {
            try {
                val charactersLeft = licenses.copiesLeft(license.id)
                if (charactersLeft != null) {
                    return charactersLeft
                }
            } catch (error: Error) {
                if (DEBUG) Timber.e(error)
            }
            return null
        }

    override val canCopy: Boolean
        get() = (charactersToCopyLeft ?: 1) > 0

    override fun canCopy(text: String): Boolean =
        charactersToCopyLeft?.let { it <= text.length }
            ?: true

    override fun copy(text: String): Boolean {
        var charactersLeft = charactersToCopyLeft ?: return true
        if (text.length > charactersLeft) {
            return false
        }

        try {
            charactersLeft = maxOf(0, charactersLeft - text.length)
            licenses.setCopiesLeft(charactersLeft, license.id)
        } catch (error: Error) {
            if (DEBUG) Timber.e(error)
        }
        return true
    }

    override val pagesToPrintLeft: Int?
        get() {
            try {
                val pagesLeft = licenses.printsLeft(license.id)
                if (pagesLeft != null) {
                    return pagesLeft
                }
            } catch (error: Error) {
                if (DEBUG) Timber.e(error)
            }
            return null
        }

    override val canPrint: Boolean
        get() = (pagesToPrintLeft ?: 1) > 0

    override fun canPrint(pageCount: Int): Boolean =
        pagesToPrintLeft?.let { it <= pageCount }
            ?: true

    override fun print(pageCount: Int): Boolean {
        var pagesLeft = pagesToPrintLeft ?: return true
        if (pagesLeft < pageCount) {
            return false
        }
        try {
            pagesLeft = maxOf(0, pagesLeft - pageCount)
            licenses.setPrintsLeft(pagesLeft, license.id)
        } catch (error: Error) {
            if (DEBUG) Timber.e(error)
        }
        return true
    }

    override val canRenewLoan: Boolean
        get() = status?.link(StatusDocument.Rel.renew) != null

    override val maxRenewDate: Date?
        get() = status?.potentialRights?.end

    override suspend fun renewLoan(listener: LcpLicense.RenewListener, prefersWebPage: Boolean): Try<Date?, LcpException> {

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
                return status.link(StatusDocument.Rel.renew, type = type)
                    ?: continue
            }

            // Fallback on the first renew link with no media type set and assume it's a PUT action.
            return status.linkWithNoType(StatusDocument.Rel.renew)
        }

        // Programmatically renew the loan with a PUT request.
        suspend fun renewProgrammatically(link: Link): ByteArray {
            val endDate =
                if (link.templateParameters.contains("end"))
                    listener.preferredEndDate(maxRenewDate)
                else null

            val parameters = this.device.asQueryParameters.toMutableMap()
            if (endDate != null) {
                parameters["end"] = endDate.toIso8601String()
            }

            val url = link.url(parameters)

            return network.fetch(url.toString(), NetworkService.Method.PUT)
                .getOrElse { error ->
                    when (error.status) {
                        HttpURLConnection.HTTP_BAD_REQUEST -> throw LcpException.Renew.RenewFailed
                        HttpURLConnection.HTTP_FORBIDDEN -> throw LcpException.Renew.InvalidRenewalPeriod(maxRenewDate = this.maxRenewDate)
                        else -> throw LcpException.Renew.UnexpectedServerError
                    }
                }
        }

        // Renew the loan by presenting a web page to the user.
        suspend fun renewWithWebPage(link: Link): ByteArray {
            // The reading app will open the URL in a web view and return when it is dismissed.
            listener.openWebPage(link.url)

            val statusURL = tryOrNull {
                license.url(LicenseDocument.Rel.status, preferredType = MediaType.LCP_STATUS_DOCUMENT)
            } ?: throw LcpException.LicenseInteractionNotAvailable

            return network.fetch(statusURL.toString(), headers = mapOf("Accept" to MediaType.LCP_STATUS_DOCUMENT.toString())).getOrThrow()
        }

        try {
            val link = findRenewLink()
                ?: throw LcpException.LicenseInteractionNotAvailable

            val data =
                if (link.mediaType.isHtml) {
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
            return Try.failure(LcpException.wrap(e))
        }
    }

    override val canReturnPublication: Boolean
        get() = status?.link(StatusDocument.Rel.`return`) != null

    override suspend fun returnPublication(): Try<Unit, LcpException> {
        try {
            val status = this.documents.status
            val url = try {
                status?.url(StatusDocument.Rel.`return`, preferredType = null, parameters = device.asQueryParameters)
            } catch (e: Throwable) {
                null
            }
            if (status == null || url == null) {
                throw LcpException.LicenseInteractionNotAvailable
            }

            network.fetch(url.toString(), method = NetworkService.Method.PUT)
                .onSuccess { validateStatusDocument(it) }
                .onFailure { error ->
                    when (error.status) {
                        HttpURLConnection.HTTP_BAD_REQUEST -> throw LcpException.Return.ReturnFailed
                        HttpURLConnection.HTTP_FORBIDDEN -> throw LcpException.Return.AlreadyReturnedOrExpired
                        else -> throw LcpException.Return.UnexpectedServerError
                    }
                }

            return Try.success(Unit)

        } catch (e: Exception) {
            return Try.failure(LcpException.wrap(e))
        }
    }

    init {
        LicenseValidation.observe(validation) { documents, _ ->
            documents?.let {
                this.documents = documents
            }
        }
    }

    private fun validateStatusDocument(data: ByteArray): Unit =
        validation.validate(LicenseValidation.Document.status(data)) { _, _ -> }

}


