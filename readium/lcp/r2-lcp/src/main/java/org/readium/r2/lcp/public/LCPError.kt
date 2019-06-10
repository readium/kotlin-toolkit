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

sealed class LCPError : Error() {
    object licenseIsBusy : LCPError()
    data class licenseIntegrity(val error: Error) : LCPError()
    data class licenseStatus(val error: StatusError) : LCPError()
    object licenseContainer : LCPError()
    object licenseInteractionNotAvailable : LCPError()
    object licenseProfileNotSupported : LCPError()
    data class licenseRenew(val error: RenewError) : LCPError()
    data class licenseReturn(val error: ReturnError) : LCPError()
    object crlFetching : LCPError()
    data class parsing(val error: ParsingError) : LCPError()
    data class network(val error: Error?) : LCPError()
    data class runtime(val error: String) : LCPError()
    data class unknown(val error: Error?) : LCPError()

    val errorDescription: String?
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
            }
        }
}

sealed class StatusError : Error() {
    data class cancelled(val date: DateTime) : StatusError()

    data class returned(val date: DateTime) : StatusError()
    data class expired(val start: DateTime, val end: DateTime) : StatusError()
    data class revoked(val date: DateTime, val devicesCount: Int) : StatusError()

    public val errorDescription: String?
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
                is revoked -> "This license has been revoked by its provider on ${date.toLocalDate()}.\nThe license was registered by ${devicesCount} device${if (devicesCount > 1) "s" else ""}."
            }
        }

}

sealed class RenewError : Error() {
    object renewFailed : RenewError()
    data class invalidRenewalPeriod
    (val maxRenewDate: DateTime?) : RenewError()

    object unexpectedServerError : RenewError()

    val errorDescription: String?
        get() {
            return when (this) {
                is renewFailed -> "Your publication could not be renewed properly."
                is invalidRenewalPeriod -> "Incorrect renewal period, your publication could not be renewed."
                is unexpectedServerError -> "An unexpected error has occurred on the server."
            }
        }
}

sealed class ReturnError : Error() {

    object returnFailed : ReturnError()
    object alreadyReturnedOrExpired : ReturnError()
    object unexpectedServerError : ReturnError()

    val errorDescription: String?
        get() {
            return when (this) {
                is returnFailed -> "Your publication could not be returned properly."
                is alreadyReturnedOrExpired -> "Your publication has already been returned before or is expired."
                is unexpectedServerError -> "An unexpected error has occurred on the server."
            }
        }
}

sealed class ParsingError : Error() {
    object malformedJSON : ParsingError()
    object licenseDocument : ParsingError()
    object statusDocument : ParsingError()
    object link : ParsingError()
    object encryption : ParsingError()
    object signature : ParsingError()
    data class url(val rel: String) : ParsingError()

    public val errorDescription: String?
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
