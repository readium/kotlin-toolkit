// TODO see below
/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.readium.r2.lcp.*
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.LcpClient
import org.readium.r2.lcp.service.LicensesRepository
import org.readium.r2.lcp.service.NetworkService
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

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

    override fun print(pagesCount: Int): Boolean {
        var pagesLeft = pagesToPrintLeft ?: return true
        if (pagesLeft < pagesCount) {
            return false
        }
        try {
            pagesLeft = maxOf(0, pagesLeft - pagesCount)
            licenses.setPrintsLeft(pagesLeft, license.id)
        } catch (error: Error) {
            if (DEBUG) Timber.e(error)
        }
        return true
    }

    override val canRenewLoan: Boolean
        get() = status?.link(StatusDocument.Rel.renew) != null

    override val maxRenewDate: DateTime?
        get() = status?.potentialRights?.end

    override suspend fun renewLoan(end: DateTime?, urlPresenter: suspend (URL) -> Unit): Try<Unit, LcpException> {

        suspend fun callPUT(url: URL): ByteArray {
            val (status, data) = this.network.fetch(url.toString(), NetworkService.Method.PUT)
            when (status) {
                HttpURLConnection.HTTP_OK -> return data!!
                HttpURLConnection.HTTP_BAD_REQUEST -> throw LcpException.Renew.RenewFailed
                HttpURLConnection.HTTP_FORBIDDEN -> throw LcpException.Renew.InvalidRenewalPeriod(maxRenewDate = this.maxRenewDate)
                else -> throw LcpException.Renew.UnexpectedServerError
            }
        }

        // TODO needs to be tested
        suspend fun callHTML(url: URL): ByteArray {
            val statusURL = try {
                this.license.url(LicenseDocument.Rel.status)
            } catch (e: Throwable) {
                null
            } ?: throw LcpException.LicenseInteractionNotAvailable
            urlPresenter(url)
            val (status, data) = this.network.fetch(statusURL.toString())
            return if (status != HttpURLConnection.HTTP_OK)
                throw LcpException.Network(null)
            else
                data!!
        }

        try {
            val parameters = this.device.asQueryParameters.toMutableMap()
            end?.let {
                parameters["end"] = end.toString()
            }
            val status = this.documents.status
            val link = status?.link(StatusDocument.Rel.renew)
            val url = link?.url(parameters)
            if (status == null || link == null || url == null) {
                throw LcpException.LicenseInteractionNotAvailable
            }
            val data = if (link.mediaType?.isHtml == true) {
                callHTML(url)
            } else {
                callPUT(url)
            }
            validateStatusDocument(data)

            return Try.success(Unit)

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
                status?.url(StatusDocument.Rel.`return`, device.asQueryParameters)
            } catch (e: Throwable) {
                null
            }
            if (status == null || url == null) {
                throw LcpException.LicenseInteractionNotAvailable
            }

            val (statusCode, data) = network.fetch(url.toString(), method = NetworkService.Method.PUT)
            when (statusCode) {
                HttpURLConnection.HTTP_OK -> validateStatusDocument(data!!)
                HttpURLConnection.HTTP_BAD_REQUEST -> throw LcpException.Return.ReturnFailed
                HttpURLConnection.HTTP_FORBIDDEN -> throw LcpException.Return.AlreadyReturnedOrExpired
                else -> throw LcpException.Return.UnexpectedServerError
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


