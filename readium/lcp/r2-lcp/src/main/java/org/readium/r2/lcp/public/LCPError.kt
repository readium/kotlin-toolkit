/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.public

import org.joda.time.DateTime
import java.net.SocketTimeoutException

sealed class LCPError : Exception() {
    object licenseIsBusy : LCPError()
    data class licenseIntegrity(val error: Exception) : LCPError()
    data class licenseStatus(val error: StatusError) : LCPError()
    object licenseContainer : LCPError()
    object licenseInteractionNotAvailable : LCPError()
    object licenseProfileNotSupported : LCPError()
    data class licenseRenew(val error: RenewError) : LCPError()
    data class licenseReturn(val error: ReturnError) : LCPError()
    object crlFetching : LCPError()
    data class parsing(val error: ParsingError) : LCPError()
    data class network(val error: Exception?) : LCPError()
    data class runtime(val error: String) : LCPError()
    data class unknown(val error: Exception?) : LCPError()

    open val errorDescription: String?
        get() {
            return when (this) {
                is licenseIsBusy -> "Can't perform this operation at the moment."
                is licenseIntegrity -> error.localizedMessage
                is licenseStatus -> error.localizedMessage
                is licenseContainer -> "Can't access the License Document."
                is licenseInteractionNotAvailable -> "This interaction is not available."
                is licenseProfileNotSupported -> "This License has a profile identifier that this app cannot handle, the publication cannot be processed."
                is crlFetching -> "Can't retrieve the Certificate Revocation List."
                is licenseRenew -> error.localizedMessage
                is licenseReturn -> error.localizedMessage
                is parsing -> error.localizedMessage
                is network -> error?.localizedMessage ?: "Network error."
                is runtime -> error
                is unknown -> error?.localizedMessage
                else -> TODO()
            }
        }


    companion object {

        fun wrap(optionalError: Exception?): LCPError {
            val error = optionalError ?: return unknown(null)
            if (error is LCPError) {
                return error
            }
            if (error is StatusError) {
                return licenseStatus(error)
            }
            if (error is RenewError) {
                return licenseRenew(error)
            }
            if (error is ReturnError) {
                return licenseReturn(error)
            }
            if (error is LCPClientError) {
                return licenseIntegrity(error)
            }
            if (error is ParsingError) {
                return parsing(error)
            }
            if (error is Error) {
                when (error) {
                    is SocketTimeoutException -> network(error)
                    else -> unknown(error)
                }
            }

            return unknown(error)
        }


        fun <T> wrap(completion: (T?, LCPError?) -> Unit): (T?, Exception?) -> Unit {
            return { value, error ->
                if (error != null) {
                    completion(value, wrap(error))
                } else {
                    completion(value, null)
                }
            }
        }

        fun wrap(completion: (LCPError?) -> Unit): (Exception?) -> Unit =
                { error ->
                    if (error != null) {
                        completion(wrap(error))
                    } else {
                        completion(null)
                    }
                }

    }
}

sealed class StatusError : LCPError() {
    data class cancelled(val date: DateTime) : StatusError()

    data class returned(val date: DateTime) : StatusError()
    data class expired(val start: DateTime, val end: DateTime) : StatusError()
    data class revoked(val date: DateTime, val devicesCount: Int) : StatusError()

    override val errorDescription: String?
        get() {
            return when (this) {
                is cancelled -> "You have cancelled this license on ${date.toLocalDate()}."
                is returned -> "This license has been returned on ${date.toLocalDate()}."
                is expired -> {
                    if (start > DateTime()) {
                        "This license starts on ${start.toLocalDate()}."
                    } else {
                        "This license expired on ${end.toLocalDate()}."
                    }
                }
                is revoked -> "This license has been revoked by its provider on ${date.toLocalDate()}. The license was registered by $devicesCount device${if (devicesCount > 1) "s" else ""}."
            }
        }

}

sealed class RenewError : LCPError() {
    object renewFailed : RenewError()
    data class invalidRenewalPeriod
    (val maxRenewDate: DateTime?) : RenewError()

    object unexpectedServerError : RenewError()

    override val errorDescription: String?
        get() {
            return when (this) {
                is renewFailed -> "Your publication could not be renewed properly."
                is invalidRenewalPeriod -> "Incorrect renewal period, your publication could not be renewed."
                is unexpectedServerError -> "An unexpected error has occurred on the server."
            }
        }
}

sealed class ReturnError : LCPError() {

    object returnFailed : ReturnError()
    object alreadyReturnedOrExpired : ReturnError()
    object unexpectedServerError : ReturnError()

    override val errorDescription: String?
        get() {
            return when (this) {
                is returnFailed -> "Your publication could not be returned properly."
                is alreadyReturnedOrExpired -> "Your publication has already been returned before or is expired."
                is unexpectedServerError -> "An unexpected error has occurred on the server."
            }
        }
}

sealed class ParsingError : LCPError() {
    object malformedJSON : ParsingError()
    object licenseDocument : ParsingError()
    object statusDocument : ParsingError()
    object link : ParsingError()
    object encryption : ParsingError()
    object signature : ParsingError()
    data class url(val rel: String) : ParsingError()

    override val errorDescription: String?
        get() {
            return when (this) {
                is malformedJSON -> "The JSON is malformed and can't be parsed."
                is licenseDocument -> "The JSON is not representing a valid License Document."
                is statusDocument -> "The JSON is not representing a valid Status Document."
                is link -> "Invalid Link."
                is encryption -> "Invalid Encryption."
                is signature -> "Invalid License Document Signature."
                is url -> "Invalid URL for link with rel $rel."
            }
        }
}

sealed class LCPClientError(errorCode: Int? = null) : LCPError() {

    object licenseOutOfDate : LCPClientError()

    object certificateRevoked : LCPClientError()
    object certificateSignatureInvalid : LCPClientError()
    object licenseSignatureDateInvalid : LCPClientError()
    object licenseSignatureInvalid : LCPClientError()
    object contextInvalid : LCPClientError()
    object contentKeyDecryptError : LCPClientError()
    object userKeyCheckInvalid : LCPClientError()
    object contentDecryptError : LCPClientError()
    object unknown : LCPClientError()


    init {
        when (errorCode) {
            11 ->  licenseOutOfDate
            101 -> certificateRevoked
            102 -> certificateSignatureInvalid
            111 -> licenseSignatureDateInvalid
            112 -> licenseSignatureInvalid
            121 -> contextInvalid
            131 -> contentKeyDecryptError
            141 -> userKeyCheckInvalid
            151 -> contentDecryptError
//            0 -> return null
            else -> unknown
        }
    }

    override val errorDescription: String?
        get() {
            return when (this) {
                is licenseOutOfDate -> "License is out of date (check start and end date)."
                is certificateRevoked -> "Certificate has been revoked in the CRL."
                is certificateSignatureInvalid -> "Certificate has not been signed by CA."
                is licenseSignatureDateInvalid -> "License has been issued by an expired certificate."
                is licenseSignatureInvalid -> "License signature does not match."
                is contextInvalid -> "The drm context is invalid."
                is contentKeyDecryptError -> "Unable to decrypt encrypted content key from user key."
                is userKeyCheckInvalid -> "User key check invalid."
                is contentDecryptError -> "Unable to decrypt encrypted content from content key."
                is unknown -> "Unknown error."
                else -> TODO()
            }
        }
}
