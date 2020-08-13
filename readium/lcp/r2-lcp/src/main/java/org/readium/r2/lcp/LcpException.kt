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
import org.readium.lcp.sdk.DRMException
import java.net.SocketTimeoutException

sealed class LcpException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    /** The interaction is not available with this License. */
    object LicenseInteractionNotAvailable : LcpException("This interaction is not available.")

    /** This License's profile is not supported by liblcp. */
    object LicenseProfileNotSupported : LcpException(
        "This License has a profile identifier that this app cannot handle, the publication cannot be processed."
    )

    /** Failed to retrieve the Certificate Revocation List. */
    object CrlFetching : LcpException("Can't retrieve the Certificate Revocation List.")

    /** A network request failed with the given exception. */
    class Network(override val cause: Exception?) : LcpException("Network error.")

    /**
     * An unexpected LCP exception occurred. Please post an issue on r2-lcp-kotlin with the error
     * message and how to reproduce it.
     */
    class Runtime(override val message: String) : LcpException()

    /** An unknown low-level exception was reported. */
    class Unknown(override val cause: Exception?) : LcpException()


    /**
     * Exceptions while checking the status of the License, using the Status Document.
     *
     * The app should notify the user and stop there. The message to the user must be clear about
     * the status of the license: don't display "expired" if the status is "revoked". The date and
     * time corresponding to the new status should be displayed (e.g. "The license expired on 01
     * January 2018").
     */
    sealed class LicenseStatus(message: String) : LcpException(message) {

        class Cancelled(val date: DateTime) : LicenseStatus("This license was cancelled on ${date.toLocalDate()}.")

        class Returned(val date: DateTime) : LicenseStatus("This license has been returned on ${date.toLocalDate()}.")

        class Expired(val start: DateTime, val end: DateTime) : LicenseStatus(
            if (start > DateTime()) {
                "This license starts on ${start.toLocalDate()}."
            } else {
                "This license expired on ${end.toLocalDate()}."
            }
        )

        /**
         * If the license has been revoked, the user message should display the number of devices which
         * registered to the server. This count can be calculated from the number of "register" events
         * in the status document. If no event is logged in the status document, no such message should
         * appear (certainly not "The license was registered by 0 devices").
         */
        class Revoked(val date: DateTime, val devicesCount: Int) : LicenseStatus(
            "This license was revoked by its provider on ${date.toLocalDate()}." +
                    "It was registered by $devicesCount device${if (devicesCount > 1) "s" else ""}."
        )
    }

    /**
     * Errors while renewing a loan.
     */
    sealed class Renew(message: String) : LcpException(message) {

        /** Your publication could not be renewed properly. */
        object RenewFailed : Renew("Your publication could not be renewed properly.")

        /** Incorrect renewal period, your publication could not be renewed. */
        class InvalidRenewalPeriod(val maxRenewDate: DateTime?) : Renew( "Incorrect renewal period, your publication could not be renewed.")

        /** An unexpected error has occurred on the licensing server. */
        object UnexpectedServerError : Renew("An unexpected error has occurred on the server.")
    }

    /**
     * Errors while returning a loan.
     */
    sealed class Return(message: String) : LcpException(message) {

        /** Your publication could not be returned properly. */
        object ReturnFailed : Return("Your publication could not be returned properly.")

        /** Your publication has already been returned before or is expired. */

        object AlreadyReturnedOrExpired : Return( "Your publication has already been returned before or is expired.")

        /** An unexpected error has occurred on the licensing server. */
        object UnexpectedServerError : Return("An unexpected error has occurred on the server.")
    }

    /**
     * Errors while parsing the License or Status JSON Documents.
     */
    sealed class Parsing(message: String) : LcpException(message) {

        /** The JSON is malformed and can't be parsed. */
        object MalformedJSON : Parsing("The JSON is malformed and can't be parsed.")

        /** The JSON is not representing a valid License Document. */
        object LicenseDocument : Parsing( "The JSON is not representing a valid License Document.")

        /** The JSON is not representing a valid Status Document. */
        object StatusDocument : Parsing( "The JSON is not representing a valid Status Document.")

        /** Invalid Link. */
        object Link : Parsing("Invalid Link.")

        /** Invalid Encryption. */
        object Encryption : Parsing("Invalid Encryption.")

        /** Invalid License Document Signature. */
        object Signature : Parsing( "Invalid License Document Signature.")

        /** Invalid URL for link with [rel]. */
        class Url(val rel: String) : Parsing("Invalid URL for link with rel $rel.")

    }

    /**
     * Errors while reading or writing a LCP container (LCPL, EPUB, LCPDF, etc.)
     */
    sealed class Container : LcpException( "Can't access the License Document.") {

        /** Can't access the container, it's format is wrong. */
        object OpenFailed : Container()

        /** The file at given relative path is not found in the Container. */
        class FileNotFound(val path: String) : Container()

        /** Can't read the file at given relative path in the Container. */
        class ReadFailed(val path: String) : Container()

        /** Can't write the file at given relative path in the Container. */
        class WriteFailed(val path: String) : Container()
    }

    /**
     * An error occurred while checking the integrity of the License, it can't be retrieved.
     */
    sealed class LicenseIntegrity(message: String) : LcpException(message) {

        object CertificateRevoked : LicenseIntegrity("Certificate has been revoked in the CRL.")

        object CertificateSignatureInvalid : LicenseIntegrity( "Certificate has not been signed by CA.")

        object LicenseSignatureDateInvalid : LicenseIntegrity("License has been issued by an expired certificate.")

        object LicenseSignatureInvalid : LicenseIntegrity("License signature does not match.")

        object UserKeyCheckInvalid : LicenseIntegrity("User key check invalid.")

    }

    sealed class Decryption(message: String) : LcpException(message) {

        object ContentKeyDecryptError : Decryption("Unable to decrypt encrypted content key from user key.")

        object ContentDecryptError : Decryption( "Unable to decrypt encrypted content from content key.")
    }

    companion object {

        internal fun wrap(e: Exception?): LcpException = when (e) {
            is LcpException -> e
            is SocketTimeoutException -> Network(e)
            is DRMException -> when(e.drmError.code) {
                // Error code 11 should never occur since we check the start/end date before calling createContext
                11 -> Runtime("License is out of date (check start and end date).")
                101 -> LicenseIntegrity.CertificateRevoked
                102 -> LicenseIntegrity.CertificateSignatureInvalid
                111 -> LicenseIntegrity.LicenseSignatureDateInvalid
                112 -> LicenseIntegrity.LicenseSignatureInvalid
                // Error code 121 seems to be unused in the C++ lib.
                121 -> Runtime("The drm context is invalid.")
                131 -> Decryption.ContentKeyDecryptError
                141 -> LicenseIntegrity.UserKeyCheckInvalid
                151 -> Decryption.ContentDecryptError
                else -> Unknown(e)
            }
            else -> Unknown(e)
        }
    }
}


@Deprecated("Renamed to `LcpException`", replaceWith = ReplaceWith("LcpException"))
typealias LCPError = LcpException

@Deprecated("Use `message` instead", replaceWith = ReplaceWith("message"))
val LcpException.errorDescription: String? get() = message
