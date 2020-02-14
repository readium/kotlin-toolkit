/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import org.joda.time.DateTime
import java.net.SocketTimeoutException

sealed class LCPError : Exception() {
    /** The operation can't be done right now because another License operation is running. */
    object licenseIsBusy : LCPError()
    /** An error occured while checking the integrity of the License, it can't be retrieved. */
    data class licenseIntegrity(val error: Exception) : LCPError()
    /** The status of the License is not valid, it can't be used to decrypt the publication. */
    data class licenseStatus(val error: StatusError) : LCPError()
    /** Can't read or write the License Document from its container. */
    object licenseContainer : LCPError()
    /** The interaction is not available with this License. */
    object licenseInteractionNotAvailable : LCPError()
    /** This License's profile is not supported by liblcp. */
    object licenseProfileNotSupported : LCPError()
    /** Failed to renew the loan. */
    data class licenseRenew(val error: RenewError) : LCPError()
    /** Failed to return the loan. */
    data class licenseReturn(val error: ReturnError) : LCPError()
    /** Failed to retrieve the Certificate Revocation List. */
    object crlFetching : LCPError()
    /** Failed to parse information from the License or Status Documents. */
    data class parsing(val error: ParsingError) : LCPError()
    /** A network request failed with the given error. */
    data class network(val error: Exception?) : LCPError()
    /**
     * An unexpected LCP error occured. Please post an issue on r2-lcp-swift with the error message
     * and how to reproduce it.
     */
    data class runtime(val error: String) : LCPError()
    /** An unknown low-level error was reported. */
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

/**
 * Errors while checking the status of the License, using the Status Document.
 */
sealed class StatusError : LCPError() {

    /**
     * For the case (revoked, returned, cancelled, expired), app should notify the user and stop
     * there. The message to the user must be clear about the status of the license: don't display
     * "expired" if the status is "revoked". The date and time corresponding to the new status
     * should be displayed (e.g. "The license expired on 01 January 2018").
     */
    data class cancelled(val date: DateTime) : StatusError()

    data class returned(val date: DateTime) : StatusError()

    data class expired(val start: DateTime, val end: DateTime) : StatusError()

    /**
     * If the license has been revoked, the user message should display the number of devices which
     * registered to the server. This count can be calculated from the number of "register" events
     * in the status document. If no event is logged in the status document, no such message should
     * appear (certainly not "The license was registered by 0 devices").
     */
    data class revoked(val date: DateTime, val devicesCount: Int) : StatusError()

    override val errorDescription: String?
        get() {
            return when (this) {
                is cancelled -> "This license was cancelled on ${date.toLocalDate()}."
                is returned -> "This license has been returned on ${date.toLocalDate()}."
                is expired -> {
                    if (start > DateTime()) {
                        "This license starts on ${start.toLocalDate()}."
                    } else {
                        "This license expired on ${end.toLocalDate()}."
                    }
                }
                is revoked -> "This license was revoked by its provider on ${date.toLocalDate()}. It was registered by $devicesCount device${if (devicesCount > 1) "s" else ""}."
            }
        }

}

/**
 * Errors while renewing a loan.
 */
sealed class RenewError : LCPError() {
    /** Your publication could not be renewed properly. */
    object renewFailed : RenewError()
    /** Incorrect renewal period, your publication could not be renewed. */
    data class invalidRenewalPeriod(val maxRenewDate: DateTime?) : RenewError()
    /** An unexpected error has occurred on the licensing server. */
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

/**
 * Errors while returning a loan.
 */
sealed class ReturnError : LCPError() {
    /** Your publication could not be returned properly. */
    object returnFailed : ReturnError()
    /** Your publication has already been returned before or is expired. */
    object alreadyReturnedOrExpired : ReturnError()
    /** An unexpected error has occurred on the licensing server. */
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

/**
 * Errors while parsing the License or Status JSON Documents.
 */
sealed class ParsingError : LCPError() {
    /** The JSON is malformed and can't be parsed. */
    object malformedJSON : ParsingError()
    /** The JSON is not representing a valid License Document. */
    object licenseDocument : ParsingError()
    /** The JSON is not representing a valid Status Document. */
    object statusDocument : ParsingError()
    /** Invalid Link. */
    object link : ParsingError()
    /** Invalid Encryption. */
    object encryption : ParsingError()
    /** Invalid License Document Signature. */
    object signature : ParsingError()
    /** Invalid URL for link with [rel]. */
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

// FIXME: Missing ContainerError (see Swift)
