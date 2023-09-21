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
import org.readium.r2.shared.util.Url

public sealed class LcpException(
    userMessageId: Int,
    vararg args: Any,
    quantity: Int? = null,
    cause: Throwable? = null
) : UserException(userMessageId, quantity, *args, cause = cause) {
    protected constructor(@StringRes userMessageId: Int, vararg args: Any, cause: Throwable? = null) : this(
        userMessageId,
        *args,
        quantity = null,
        cause = cause
    )
    protected constructor(
        @PluralsRes userMessageId: Int,
        quantity: Int,
        vararg args: Any,
        cause: Throwable? = null
    ) : this(userMessageId, *args, quantity = quantity, cause = cause)

    /** The interaction is not available with this License. */
    public object LicenseInteractionNotAvailable : LcpException(
        R.string.readium_lcp_exception_license_interaction_not_available
    )

    /** This License's profile is not supported by liblcp. */
    public object LicenseProfileNotSupported : LcpException(
        R.string.readium_lcp_exception_license_profile_not_supported
    )

    /** Failed to retrieve the Certificate Revocation List. */
    public object CrlFetching : LcpException(R.string.readium_lcp_exception_crl_fetching)

    /** A network request failed with the given exception. */
    public class Network(override val cause: Throwable?) : LcpException(
        R.string.readium_lcp_exception_network,
        cause = cause
    )

    /**
     * An unexpected LCP exception occurred. Please post an issue on r2-lcp-kotlin with the error
     * message and how to reproduce it.
     */
    public class Runtime(override val message: String) : LcpException(
        R.string.readium_lcp_exception_runtime
    )

    /** An unknown low-level exception was reported. */
    public class Unknown(override val cause: Throwable?) : LcpException(
        R.string.readium_lcp_exception_unknown
    )

    /**
     * Errors while checking the status of the License, using the Status Document.
     *
     * The app should notify the user and stop there. The message to the user must be clear about
     * the status of the license: don't display "expired" if the status is "revoked". The date and
     * time corresponding to the new status should be displayed (e.g. "The license expired on 01
     * January 2018").
     */
    public sealed class LicenseStatus(userMessageId: Int, vararg args: Any, quantity: Int? = null) : LcpException(
        userMessageId,
        *args,
        quantity = quantity
    ) {
        protected constructor(@StringRes userMessageId: Int, vararg args: Any) : this(
            userMessageId,
            *args,
            quantity = null
        )
        protected constructor(@PluralsRes userMessageId: Int, quantity: Int, vararg args: Any) : this(
            userMessageId,
            *args,
            quantity = quantity
        )

        public class Cancelled(public val date: Date) : LicenseStatus(
            R.string.readium_lcp_exception_license_status_cancelled,
            date
        )

        public class Returned(public val date: Date) : LicenseStatus(
            R.string.readium_lcp_exception_license_status_returned,
            date
        )

        public class NotStarted(public val start: Date) : LicenseStatus(
            R.string.readium_lcp_exception_license_status_not_started,
            start
        )

        public class Expired(public val end: Date) : LicenseStatus(
            R.string.readium_lcp_exception_license_status_expired,
            end
        )

        /**
         * If the license has been revoked, the user message should display the number of devices which
         * registered to the server. This count can be calculated from the number of "register" events
         * in the status document. If no event is logged in the status document, no such message should
         * appear (certainly not "The license was registered by 0 devices").
         */
        public class Revoked(public val date: Date, public val devicesCount: Int) :
            LicenseStatus(
                R.plurals.readium_lcp_exception_license_status_revoked,
                devicesCount,
                date,
                devicesCount
            )
    }

    /**
     * Errors while renewing a loan.
     */
    public sealed class Renew(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        /** Your publication could not be renewed properly. */
        public object RenewFailed : Renew(R.string.readium_lcp_exception_renew_renew_failed)

        /** Incorrect renewal period, your publication could not be renewed. */
        public class InvalidRenewalPeriod(public val maxRenewDate: Date?) : Renew(
            R.string.readium_lcp_exception_renew_invalid_renewal_period
        )

        /** An unexpected error has occurred on the licensing server. */
        public object UnexpectedServerError : Renew(
            R.string.readium_lcp_exception_renew_unexpected_server_error
        )
    }

    /**
     * Errors while returning a loan.
     */
    public sealed class Return(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        /** Your publication could not be returned properly. */
        public object ReturnFailed : Return(R.string.readium_lcp_exception_return_return_failed)

        /** Your publication has already been returned before or is expired. */

        public object AlreadyReturnedOrExpired : Return(
            R.string.readium_lcp_exception_return_already_returned_or_expired
        )

        /** An unexpected error has occurred on the licensing server. */
        public object UnexpectedServerError : Return(
            R.string.readium_lcp_exception_return_unexpected_server_error
        )
    }

    /**
     * Errors while parsing the License or Status JSON Documents.
     */
    public sealed class Parsing(
        @StringRes userMessageId: Int = R.string.readium_lcp_exception_parsing
    ) : LcpException(userMessageId) {

        /** The JSON is malformed and can't be parsed. */
        public object MalformedJSON : Parsing(R.string.readium_lcp_exception_parsing_malformed_json)

        /** The JSON is not representing a valid License Document. */
        public object LicenseDocument : Parsing(
            R.string.readium_lcp_exception_parsing_license_document
        )

        /** The JSON is not representing a valid Status Document. */
        public object StatusDocument : Parsing(
            R.string.readium_lcp_exception_parsing_status_document
        )

        /** Invalid Link. */
        public object Link : Parsing()

        /** Invalid Encryption. */
        public object Encryption : Parsing()

        /** Invalid License Document Signature. */
        public object Signature : Parsing()

        /** Invalid URL for link with [rel]. */
        public class Url(public val rel: String) : Parsing()
    }

    /**
     * Errors while reading or writing a LCP container (LCPL, EPUB, LCPDF, etc.)
     */
    public sealed class Container(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        /** Can't access the container, it's format is wrong. */
        public object OpenFailed : Container(R.string.readium_lcp_exception_container_open_failed)

        /** The file at given relative path is not found in the Container. */
        public class FileNotFound(public val url: Url) : Container(
            R.string.readium_lcp_exception_container_file_not_found
        )

        /** Can't read the file at given relative path in the Container. */
        public class ReadFailed(public val url: Url?) : Container(
            R.string.readium_lcp_exception_container_read_failed
        )

        /** Can't write the file at given relative path in the Container. */
        public class WriteFailed(public val url: Url?) : Container(
            R.string.readium_lcp_exception_container_write_failed
        )
    }

    /**
     * An error occurred while checking the integrity of the License, it can't be retrieved.
     */
    public sealed class LicenseIntegrity(@StringRes userMessageId: Int) : LcpException(
        userMessageId
    ) {

        public object CertificateRevoked : LicenseIntegrity(
            R.string.readium_lcp_exception_license_integrity_certificate_revoked
        )

        public object InvalidCertificateSignature : LicenseIntegrity(
            R.string.readium_lcp_exception_license_integrity_invalid_certificate_signature
        )

        public object InvalidLicenseSignatureDate : LicenseIntegrity(
            R.string.readium_lcp_exception_license_integrity_invalid_license_signature_date
        )

        public object InvalidLicenseSignature : LicenseIntegrity(
            R.string.readium_lcp_exception_license_integrity_invalid_license_signature
        )

        public object InvalidUserKeyCheck : LicenseIntegrity(
            R.string.readium_lcp_exception_license_integrity_invalid_user_key_check
        )
    }

    public sealed class Decryption(@StringRes userMessageId: Int) : LcpException(userMessageId) {

        public object ContentKeyDecryptError : Decryption(
            R.string.readium_lcp_exception_decryption_content_key_decrypt_error
        )

        public object ContentDecryptError : Decryption(
            R.string.readium_lcp_exception_decryption_content_decrypt_error
        )
    }

    public companion object {

        internal fun wrap(e: Exception?): LcpException = when (e) {
            is LcpException -> e
            is SocketTimeoutException -> Network(e)
            else -> Unknown(e)
        }
    }
}

@Deprecated(
    "Renamed to `LcpException`",
    replaceWith = ReplaceWith("LcpException"),
    level = DeprecationLevel.ERROR
)
public typealias LCPError = LcpException

@Deprecated(
    "Use `getUserMessage()` instead",
    replaceWith = ReplaceWith("getUserMessage(context)"),
    level = DeprecationLevel.ERROR
)
public val LcpException.errorDescription: String? get() = message
