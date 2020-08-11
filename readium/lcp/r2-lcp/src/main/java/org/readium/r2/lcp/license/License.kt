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

import android.content.Context
import org.joda.time.DateTime
import org.readium.lcp.sdk.Lcp
import org.readium.r2.lcp.*
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.license.container.createLicenseContainer
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.LicensesRepository
import org.readium.r2.lcp.service.NetworkService
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.format.Format
import timber.log.Timber
import java.io.File
import java.net.URL
import java.util.*

internal class License(
    private var documents: ValidatedDocuments,
    private val validation: LicenseValidation,
    private val licenses: LicensesRepository,
    private val device: DeviceService,
    private val network: NetworkService
) : LCPLicense {

    override val license: LicenseDocument
        get() = documents.license
    override val status: StatusDocument?
        get() = documents.status
    override val encryptionProfile: String?
        get() = license.encryption.profile

    override fun decipher(data: ByteArray): ByteArray? {
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

    override fun renewLoan(end: DateTime?, present: URLPresenter, completion: (LCPError?) -> Unit) {

        fun callPUT(url: URL, parameters: URLParameters, callback: (Result<ByteArray>) -> Unit) {
            this.network.fetch(url.toString(), NetworkService.Method.PUT, parameters) { status, data ->
                when (status) {
                    200 -> callback(Result.success(data!!))
                    400 -> callback(Result.failure(RenewError.renewFailed))
                    403 -> callback(Result.failure(RenewError.invalidRenewalPeriod(maxRenewDate = this.maxRenewDate)))
                    else -> callback(Result.failure(RenewError.unexpectedServerError))
                }
            }
        }

        // TODO needs to be tested
        fun callHTML(url: URL, parameters: URLParameters, callback: (Result<ByteArray>) -> Unit) {
            val statusURL = tryOrNull { this.license.url(LicenseDocument.Rel.status) }
            if (statusURL == null) {
                callback(Result.failure(LCPError.licenseInteractionNotAvailable))
                return
            }

            present(url) {
                this.network.fetch(statusURL.toString(), parameters = parameters) { status, data ->
                    if (status != 200) {
                        callback(Result.failure(LCPError.network(null)))
                    } else {
                        callback(Result.success(data!!))
                    }
                }
            }
        }

        fun validateResponse(result: Result<ByteArray>) {
            try {
                validateStatusDocument(result.getOrThrow(), completion)
            } catch (e: Exception) {
                completion(LCPError.wrap(e))
            }
        }

        val parameters = this.device.asQueryParameters.toMutableMap()
        end?.let {
            parameters["end"] = end.toString()
        }
        val status = this.documents.status
        val link = status?.link(StatusDocument.Rel.renew)
        val url = link?.url(parameters)
        if (status == null || link == null || url == null) {
            throw LCPError.licenseInteractionNotAvailable
        }

        if (link.type == "text/html") {
            callHTML(url, parameters, ::validateResponse)
        } else {
            callPUT(url, parameters, ::validateResponse)
        }
    }

    override val canReturnPublication: Boolean
        get() = status?.link(StatusDocument.Rel.`return`) != null

    override fun returnPublication(completion: (LCPError?) -> Unit) {
        val status = this.documents.status
        val url = tryOrNull { status?.url(StatusDocument.Rel.`return`, device.asQueryParameters) }
        if (status == null || url == null) {
            completion(LCPError.licenseInteractionNotAvailable)
            return
        }

        network.fetch(url.toString(), method = NetworkService.Method.PUT) { statusCode, data ->
            try {
                when (statusCode) {
                    200 -> validateStatusDocument(data!!, completion)
                    400 -> throw ReturnError.returnFailed
                    403 -> throw ReturnError.alreadyReturnedOrExpired
                    else -> throw ReturnError.unexpectedServerError
                }
            } catch (e: Exception) {
                completion(LCPError.wrap(e))
            }
        }
    }

    init {
        LicenseValidation.observe(validation) { documents, error ->
            documents?.let {
                this.documents = documents
            }
        }
    }

    internal suspend fun fetchPublication(context: Context): LCPImportedPublication {
        val license = this.documents.license
        val link = license.link(LicenseDocument.Rel.publication)
        val url = link?.url
            ?: throw ParsingError.url(rel = LicenseDocument.Rel.publication.rawValue)

        val properties =  Properties()
        val inputStream = context.assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir = properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        val rootDir: String =  if (useExternalFileDir) {
            context.getExternalFilesDir(null)?.path + "/"
        } else {
            context.filesDir.path + "/"
        }

        val fileName = UUID.randomUUID().toString()
        val destination = File(rootDir, fileName)
        if (DEBUG) Timber.i("LCP destination $destination")

        val format = network.download(url, destination) ?: Format.of(mediaType = link.type) ?: Format.EPUB

        // Saves the License Document into the downloaded publication
        val container = createLicenseContainer(destination.path, format)
        container.write(license)

        return LCPImportedPublication(
            localURL = destination.path,
            suggestedFilename = "${license.id}.${format.fileExtension}"
        )
    }

    private fun validateStatusDocument(data: ByteArray, completion: (LCPError?) -> Unit): Unit =
        validation.validate(LicenseValidation.Document.status(data)) { _, error -> completion(error?.let { LCPError.wrap(it) }) }

}


