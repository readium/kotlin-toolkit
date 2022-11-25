/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.net.SocketTimeoutException
import java.util.*
import org.readium.r2.shared.UserException

sealed class LcpException(
    userMessageId: Int,
    vararg args: Any,
    quantity: Int? = null,
    cause: Throwable? = null
) : UserException(userMessageId, quantity, *args, cause = cause) {
    constructor(@StringRes userMessageId: Int, vararg args: Any, cause: Throwable? = null) : this(userMessageId, *args, quantity = null, cause = cause)
    constructor(
        @PluralsRes userMessageId: Int,
        quantity: Int,
        vararg args: Any,
        cause: Throwable? = null
    ) : this(userMessageId, *args, quantity = quantity, cause = cause)

    /** The interaction is not available with this License. */
    object LicenseInteractionNotAvailable : LcpException(R.string.r2_lcp_exception_license_interaction_not_available)

    /** This License's profile is not supported by liblcp. */
    object LicenseProfileNotSupported : LcpException(R.string.r2_lcp_exception_license_profile_not_supported)

    /** Failed to retrieve the Certificate Revocation List. */
    object CrlFetching : LcpException(R.string.r2_lcp_exception_crl_fetching)

    /** A network request failed with the given exception. */
    class Network(override val cause: Throwable?) : LcpException(R.string.r2_lcp_exception_network, cause = cause)

    /**
     * An unexpected LCP exception occurred. Please post an issue on r2-lcp-kotlin with the error
     * message and how to reproduce it.
     */
    class Runtime(override val message: String) : LcpException(R.string.r2_lcp_exception_runtime)

    /** An unknown low-level exception was reported. */
    class Unknown(override val cause: Throwable?) : LcpException(R.string.r2_lcp_exception_unknown)

    /**
     * Errors while checking the status of the License, using the Status Document.
     *
     * The app should notify the user and stop there. The message to the user must be clear about
     * the status of the license: don't display "expired" if the status is "revoked". The date and
     * time corresponding to the new status should be displayed (e.g. "The license expired on 01
     * January 2018").
     */
    sealed class LicenseStatus(userMessageId: Int, vararg args: Any, quantity: Int? = null) : LcpException(userMessageId, *args, quantity = quantity) {
        constructor(@StringRes userMessageId: Int, vararg args: Any) : this(userMessageId, *args, quantity = null)
        constructor(@PluralsRes userMessageId: Int, quantity: Int, vararg args: Any) : this(userMessageId, *args, quantity = quantity)

        class Cancelled(val date: Date) : LicenseStatus(R.string.r2_lcp_exception_license_status_cancelled, date)

        class Returned(val date: Date) : LicenseStatus(R.string.r2_lcp_exception_license_status_returned, date)

        class NotStarted(val start: Date) : LicenseStatus(R.string.r2_lcp_exception_license_status_not_started, start)

        class Expired(val end: Date) : LicenseStatus(R.string.r2_lcp_exception_license_status_expired, end)

        /**
         * If the license has been revoked, the user message should display the number of devices which
         * registered to the server. This count can be calculated from the number of "register" events
         * in the status document. If no event is logged in the status document, no such message should
         * appear (certainly not "The license was registered by 0 devices").
         */
        class Revoked(val date: Date, val devicesCount: Int) :
            LicenseStatus(R.plurals.r2_lcp_exception_license_status_revoked, devicesCount, date, devicesCount)
    }

    /**
     * Errors while renewing a loan.
     */
    sealed class Renew(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        /** Your publication could not be renewed properly. */
        object RenewFailed : Renew(R.string.r2_lcp_exception_renew_renew_failed)

        /** Incorrect renewal period, your publication could not be renewed. */
        class InvalidRenewalPeriod(val maxRenewDate: Date?) : Renew(R.string.r2_lcp_exception_renew_invalid_renewal_period)

        /** An unexpected error has occurred on the licensing server. */
        object UnexpectedServerError : Renew(R.string.r2_lcp_exception_renew_unexpected_server_error)
    }

    /**
     * Errors while returning a loan.
     */
    sealed class Return(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        /** Your publication could not be returned properly. */
        object ReturnFailed : Return(R.string.r2_lcp_exception_return_return_failed)

        /** Your publication has already been returned before or is expired. */

        object AlreadyReturnedOrExpired : Return(R.string.r2_lcp_exception_return_already_returned_or_expired)

        /** An unexpected error has occurred on the licensing server. */
        object UnexpectedServerError : Return(R.string.r2_lcp_exception_return_unexpected_server_error)
    }

    /**
     * Errors while parsing the License or Status JSON Documents.
     */
    sealed class Parsing(@StringRes userMessageId: Int = R.string.r2_lcp_exception_parsing) : LcpException(userMessageId) {

        /** The JSON is malformed and can't be parsed. */
        object MalformedJSON : Parsing(R.string.r2_lcp_exception_parsing_malformed_json)

        /** The JSON is not representing a valid License Document. */
        object LicenseDocument : Parsing(R.string.r2_lcp_exception_parsing_license_document)

        /** The JSON is not representing a valid Status Document. */
        object StatusDocument : Parsing(R.string.r2_lcp_exception_parsing_status_document)

        /** Invalid Link. */
        object Link : Parsing()

        /** Invalid Encryption. */
        object Encryption : Parsing()

        /** Invalid License Document Signature. */
        object Signature : Parsing()

        /** Invalid URL for link with [rel]. */
        class Url(val rel: String) : Parsing()
    }

    /**
     * Errors while reading or writing a LCP container (LCPL, EPUB, LCPDF, etc.)
     */
    sealed class Container(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        /** Can't access the container, it's format is wrong. */
        object OpenFailed : Container(R.string.r2_lcp_exception_container_open_failed)

        /** The file at given relative path is not found in the Container. */
        class FileNotFound(val path: String) : Container(R.string.r2_lcp_exception_container_file_not_found)

        /** Can't read the file at given relative path in the Container. */
        class ReadFailed(val path: String) : Container(R.string.r2_lcp_exception_container_read_failed)

        /** Can't write the file at given relative path in the Container. */
        class WriteFailed(val path: String) : Container(R.string.r2_lcp_exception_container_write_failed)
    }

    /**
     * An error occurred while checking the integrity of the License, it can't be retrieved.
     */
    sealed class LicenseIntegrity(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        object CertificateRevoked : LicenseIntegrity(R.string.r2_lcp_exception_license_integrity_certificate_revoked)

        object InvalidCertificateSignature : LicenseIntegrity(R.string.r2_lcp_exception_license_integrity_invalid_certificate_signature)

        object InvalidLicenseSignatureDate : LicenseIntegrity(R.string.r2_lcp_exception_license_integrity_invalid_license_signature_date)

        object InvalidLicenseSignature : LicenseIntegrity(R.string.r2_lcp_exception_license_integrity_invalid_license_signature)

        object InvalidUserKeyCheck : LicenseIntegrity(R.string.r2_lcp_exception_license_integrity_invalid_user_key_check)
    }

    sealed class Decryption(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        object ContentKeyDecryptError : Decryption(R.string.r2_lcp_exception_decryption_content_key_decrypt_error)

        object ContentDecryptError : Decryption(R.string.r2_lcp_exception_decryption_content_decrypt_error)
    }

    companion object {

        internal fun wrap(e: Exception?): LcpException = when (e) {
            is LcpException -> e
            is SocketTimeoutException -> Network(e)
            else -> Unknown(e)
        }
    }
}

@Deprecated("Renamed to `LcpException`", replaceWith = ReplaceWith("LcpException"))
typealias LCPError = LcpException

@Deprecated("Use `getUserMessage()` instead", replaceWith = ReplaceWith("getUserMessage(context)"))
val LcpException.errorDescription: String? get() = message
