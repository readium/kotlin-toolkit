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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.readium.lcp.sdk.Lcp
import org.readium.r2.lcp.*
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.LicensesRepository
import org.readium.r2.lcp.service.NetworkService
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class License(
    private var documents: ValidatedDocuments,
    private val validation: LicenseValidation,
    private val licenses: LicensesRepository,
    private val device: DeviceService,
    private val network: NetworkService
) : LcpLicense, LCPLicense {

    override val license: LicenseDocument
        get() = documents.license
    override val status: StatusDocument?
        get() = documents.status
    override val encryptionProfile: String?
        get() = license.encryption.profile

    override suspend fun decrypt(data: ByteArray): Try<ByteArray, LcpException> = withContext(Dispatchers.Default) {
        try {
            Try.success(decipher(data))
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }
    }

    @Throws(LcpException.Decryption::class)
    override fun decipher(data: ByteArray): ByteArray {
        // LCP lib crashes if we call decrypt on an empty ByteArray
        if (data.isEmpty())
            return ByteArray(0)

        val context = documents.getContext()
        return Lcp().decrypt(context, data)
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

    override fun copy(text: String): String? {
        var charactersLeft = charactersToCopyLeft ?: return text
        if (charactersLeft <= 0) {
            return null
        }
        var result = text
        if (result.length > charactersLeft) {
            result = result.substring(0, charactersLeft)
        }
        try {
            charactersLeft = maxOf(0, charactersLeft - result.length)
            licenses.setCopiesLeft(charactersLeft, license.id)
        } catch (error: Error) {
            if (DEBUG) Timber.e(error)
        }
        return result
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

    override suspend fun renewLoan(end: DateTime?, urlPresenter: suspend (URL) -> Unit): Try<Unit, LcpException> =
        try {
            _renewLoan(end, urlPresenter)
            Try.success(Unit)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }

    override fun renewLoan(end: DateTime?, present: URLPresenter, completion: (LCPError?) -> Unit) {

        suspend fun presentUrl(url: URL): Unit = suspendCoroutine { cont ->
            present(url) {
                cont.resume(Unit)
            }
        }

        return try {
            runBlocking { _renewLoan(end, ::presentUrl) }
            completion(null)
        } catch (e: Exception) {
            completion(LCPError.wrap(e))
        }
    }

    private suspend fun _renewLoan(end: DateTime?,  urlPresenter: suspend (URL) -> Unit) {

        suspend fun callPUT(url: URL, parameters: URLParameters): ByteArray {
            val (status, data) = this.network.fetch(url.toString(), NetworkService.Method.PUT, parameters)
            when (status) {
                HttpURLConnection.HTTP_OK -> return data!!
                HttpURLConnection.HTTP_BAD_REQUEST -> throw LcpException.Renew.RenewFailed
                HttpURLConnection.HTTP_FORBIDDEN -> throw LcpException.Renew.InvalidRenewalPeriod(maxRenewDate = this.maxRenewDate)
                else -> throw LcpException.Renew.UnexpectedServerError
            }
        }

        // TODO needs to be tested
        suspend fun callHTML(url: URL, parameters: URLParameters): ByteArray {
            val statusURL = try {
                this.license.url(LicenseDocument.Rel.status)
            } catch (e: Throwable) {
                null
            } ?: throw LcpException.LicenseInteractionNotAvailable
            urlPresenter(url)
            val (status, data) = this.network.fetch(statusURL.toString(), parameters = parameters)
            return if (status != HttpURLConnection.HTTP_OK)
                throw LcpException.Network(null)
            else
                data!!
        }

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
        val data = if (link.type == "text/html") {
            callHTML(url, parameters)
        } else {
            callPUT(url, parameters)
        }
        validateStatusDocument(data)
    }

    override val canReturnPublication: Boolean
        get() = status?.link(StatusDocument.Rel.`return`) != null

    override suspend fun returnPublication(): Try<Unit, LcpException> =
        try {
            _returnPublication()
            Try.success(Unit)
        } catch (e: Exception) {
            Try.failure(LcpException.wrap(e))
        }

    override fun returnPublication(completion: (LCPError?) -> Unit) = runBlocking {
        try {
            _returnPublication()
            completion(null)
        } catch (e: Exception) {
            completion(LCPError.wrap(e))
        }
    }

    private suspend fun _returnPublication() {
        val status = this.documents.status
        val url = try {
            status?.url(StatusDocument.Rel.`return`, device.asQueryParameters)
        } catch (e: Throwable) {
            null
        }
        if (status == null || url == null) {
            throw LCPError.licenseInteractionNotAvailable
        }

        val (statusCode, data) = network.fetch(url.toString(), method = NetworkService.Method.PUT)
        when (statusCode) {
            HttpURLConnection.HTTP_OK -> validateStatusDocument(data!!)
            HttpURLConnection.HTTP_BAD_REQUEST -> throw LcpException.Return.ReturnFailed
            HttpURLConnection.HTTP_FORBIDDEN -> throw LcpException.Return.AlreadyReturnedOrExpired
            else -> throw LcpException.Return.UnexpectedServerError
        }
    }

    init {
        LicenseValidation.observe(validation) { documents, error ->
            documents?.let {
                this.documents = documents
            }
        }
    }

    private fun validateStatusDocument(data: ByteArray): Unit =
            validation.validate(LicenseValidation.Document.status(data)) { validatedDocuments: ValidatedDocuments?, error: Exception? -> }

}


