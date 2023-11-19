/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.Date
import org.readium.r2.lcp.LcpError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Url
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

sealed class LcpUserError(
    override val content: UserError.Content,
    override val cause: UserError? = null
) : UserError {

    constructor(userMessageId: Int, vararg args: Any, quantity: Int? = null) :
        this(UserError.Content(userMessageId, quantity, args))

    constructor(@StringRes userMessageId: Int, vararg args: Any) :
        this(UserError.Content(userMessageId, *args))

    /** The interaction is not available with this License. */
    object LicenseInteractionNotAvailable : LcpUserError(
        R.string.lcp_error_license_interaction_not_available
    )

    /** This License's profile is not supported by liblcp. */
    object LicenseProfileNotSupported : LcpUserError(
        R.string.lcp_error_license_profile_not_supported
    )

    /** Failed to retrieve the Certificate Revocation List. */
    object CrlFetching : LcpUserError(R.string.lcp_error_crl_fetching)

    /** A network request failed with the given exception. */
    class Network(val error: Error?) :
        LcpUserError(R.string.lcp_error_network)

    /**
     * An unexpected LCP exception occurred. Please post an issue on r2-lcp-kotlin with the error
     * message and how to reproduce it.
     */
    class Runtime(val message: String) :
        LcpUserError(R.string.lcp_error_runtime)

    /** An unknown low-level exception was reported. */
    class Unknown(val error: Error?) :
        LcpUserError(R.string.lcp_error_unknown)

    /**
     * Errors while checking the status of the License, using the Status Document.
     *
     * The app should notify the user and stop there. The message to the user must be clear about
     * the status of the license: don't display "expired" if the status is "revoked". The date and
     * time corresponding to the new status should be displayed (e.g. "The license expired on 01
     * January 2018").
     */
    sealed class LicenseStatus(userMessageId: Int, vararg args: Any, quantity: Int? = null) :
        LcpUserError(userMessageId, args, quantity = quantity) {

        constructor(@StringRes userMessageId: Int, vararg args: Any) :
            this(userMessageId, *args, quantity = null)

        constructor(@PluralsRes userMessageId: Int, quantity: Int, vararg args: Any) :
            this(userMessageId, *args, quantity = quantity)

        class Cancelled(val date: Date) :
            LicenseStatus(R.string.lcp_error_license_status_cancelled, date)

        class Returned(val date: Date) :
            LicenseStatus(R.string.lcp_error_license_status_returned, date)

        class NotStarted(val start: Date) :
            LicenseStatus(R.string.lcp_error_license_status_not_started, start)

        class Expired(val end: Date) :
            LicenseStatus(R.string.lcp_error_license_status_expired, end)

        /**
         * If the license has been revoked, the user message should display the number of devices which
         * registered to the server. This count can be calculated from the number of "register" events
         * in the status document. If no event is logged in the status document, no such message should
         * appear (certainly not "The license was registered by 0 devices").
         */
        class Revoked(val date: Date, val devicesCount: Int) :
            LicenseStatus(
                R.plurals.lcp_error_license_status_revoked,
                devicesCount,
                date,
                devicesCount
            )
    }

    /**
     * Errors while renewing a loan.
     */
    sealed class Renew(@StringRes userMessageId: Int) : LcpUserError(userMessageId) {

        /** Your publication could not be renewed properly. */
        object RenewFailed : Renew(R.string.lcp_error_renew_renew_failed)

        /** Incorrect renewal period, your publication could not be renewed. */
        class InvalidRenewalPeriod(val maxRenewDate: Date?) :
            Renew(R.string.lcp_error_renew_invalid_renewal_period)

        /** An unexpected error has occurred on the licensing server. */
        object UnexpectedServerError :
            Renew(R.string.lcp_error_renew_unexpected_server_error)
    }

    /**
     * Errors while returning a loan.
     */
    sealed class Return(@StringRes userMessageId: Int) : LcpUserError(userMessageId) {

        /** Your publication could not be returned properly. */
        object ReturnFailed : Return(R.string.lcp_error_return_return_failed)

        /** Your publication has already been returned before or is expired. */

        object AlreadyReturnedOrExpired :
            Return(R.string.lcp_error_return_already_returned_or_expired)

        /** An unexpected error has occurred on the licensing server. */
        object UnexpectedServerError :
            Return(R.string.lcp_error_return_unexpected_server_error)
    }

    /**
     * Errors while parsing the License or Status JSON Documents.
     */
    sealed class Parsing(
        @StringRes userMessageId: Int = R.string.lcp_error_parsing
    ) : LcpUserError(userMessageId) {

        /** The JSON is malformed and can't be parsed. */
        object MalformedJSON : Parsing(R.string.lcp_error_parsing_malformed_json)

        /** The JSON is not representing a valid License Document. */
        object LicenseDocument : Parsing(R.string.lcp_error_parsing_license_document)

        /** The JSON is not representing a valid Status Document. */
        object StatusDocument : Parsing(R.string.lcp_error_parsing_status_document)

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
    sealed class Container(@StringRes userMessageId: Int) : LcpUserError(userMessageId) {

        /** Can't access the container, it's format is wrong. */
        object OpenFailed : Container(R.string.lcp_error_container_open_failed)

        /** The file at given relative path is not found in the Container. */
        class FileNotFound(val url: Url) : Container(R.string.lcp_error_container_file_not_found)

        /** Can't read the file at given relative path in the Container. */
        class ReadFailed(val url: Url?) : Container(R.string.lcp_error_container_read_failed)

        /** Can't write the file at given relative path in the Container. */
        class WriteFailed(val url: Url?) : Container(R.string.lcp_error_container_write_failed)
    }

    /**
     * An error occurred while checking the integrity of the License, it can't be retrieved.
     */
    sealed class LicenseIntegrity(@StringRes userMessageId: Int) : LcpUserError(
        userMessageId
    ) {

        object CertificateRevoked :
            LicenseIntegrity(R.string.lcp_error_license_integrity_certificate_revoked)

        object InvalidCertificateSignature :
            LicenseIntegrity(R.string.lcp_error_license_integrity_invalid_certificate_signature)

        object InvalidLicenseSignatureDate :
            LicenseIntegrity(R.string.lcp_error_license_integrity_invalid_license_signature_date)

        object InvalidLicenseSignature :
            LicenseIntegrity(R.string.lcp_error_license_integrity_invalid_license_signature)

        object InvalidUserKeyCheck :
            LicenseIntegrity(R.string.lcp_error_license_integrity_invalid_user_key_check)
    }

    sealed class Decryption(@StringRes userMessageId: Int) : LcpUserError(userMessageId) {

        object ContentKeyDecryptError :
            Decryption(R.string.lcp_error_decryption_content_key_decrypt_error)

        object ContentDecryptError :
            Decryption(R.string.lcp_error_decryption_content_decrypt_error)
    }

    companion object {

        operator fun invoke(error: LcpError): LcpUserError =
            when (error) {
                is LcpError.Container ->
                    when (error) {
                        is LcpError.Container.FileNotFound ->
                            Container.FileNotFound(error.url)
                        LcpError.Container.OpenFailed ->
                            Container.OpenFailed
                        is LcpError.Container.ReadFailed ->
                            Container.ReadFailed(error.url)
                        is LcpError.Container.WriteFailed ->
                            Container.WriteFailed(error.url)
                    }
                LcpError.CrlFetching ->
                    CrlFetching
                is LcpError.Decryption ->
                    when (error) {
                        LcpError.Decryption.ContentDecryptError ->
                            Decryption.ContentDecryptError
                        LcpError.Decryption.ContentKeyDecryptError ->
                            Decryption.ContentKeyDecryptError
                    }
                is LcpError.LicenseIntegrity ->
                    when (error) {
                        LcpError.LicenseIntegrity.CertificateRevoked ->
                            LicenseIntegrity.CertificateRevoked
                        LcpError.LicenseIntegrity.InvalidCertificateSignature ->
                            LicenseIntegrity.InvalidCertificateSignature
                        LcpError.LicenseIntegrity.InvalidLicenseSignature ->
                            LicenseIntegrity.InvalidLicenseSignature
                        LcpError.LicenseIntegrity.InvalidLicenseSignatureDate ->
                            LicenseIntegrity.InvalidLicenseSignatureDate
                        LcpError.LicenseIntegrity.InvalidUserKeyCheck ->
                            LicenseIntegrity.InvalidUserKeyCheck
                    }
                LcpError.LicenseInteractionNotAvailable ->
                    LicenseInteractionNotAvailable
                LcpError.LicenseProfileNotSupported ->
                    LicenseProfileNotSupported
                is LcpError.LicenseStatus ->
                    when (error) {
                        is LcpError.LicenseStatus.Cancelled ->
                            LicenseStatus.Cancelled(error.date)
                        is LcpError.LicenseStatus.Expired ->
                            LicenseStatus.Expired(error.end)
                        is LcpError.LicenseStatus.NotStarted ->
                            LicenseStatus.NotStarted(error.start)
                        is LcpError.LicenseStatus.Returned ->
                            LicenseStatus.Returned(error.date)
                        is LcpError.LicenseStatus.Revoked ->
                            LicenseStatus.Revoked(error.date, error.devicesCount)
                    }
                is LcpError.Network ->
                    Network(error.cause)
                is LcpError.Parsing ->
                    when (error) {
                        LcpError.Parsing.Encryption ->
                            Parsing.Encryption
                        LcpError.Parsing.LicenseDocument ->
                            Parsing.LicenseDocument
                        LcpError.Parsing.Link ->
                            Parsing.Link
                        LcpError.Parsing.MalformedJSON ->
                            Parsing.MalformedJSON
                        LcpError.Parsing.Signature ->
                            Parsing.Signature
                        LcpError.Parsing.StatusDocument ->
                            Parsing.StatusDocument
                        is LcpError.Parsing.Url ->
                            Parsing.Url(error.rel)
                    }

                is LcpError.Renew ->
                    when (error) {
                        is LcpError.Renew.InvalidRenewalPeriod ->
                            Renew.InvalidRenewalPeriod(error.maxRenewDate)
                        LcpError.Renew.RenewFailed ->
                            Renew.RenewFailed
                        LcpError.Renew.UnexpectedServerError ->
                            Renew.UnexpectedServerError
                    }
                is LcpError.Return ->
                    when (error) {
                        LcpError.Return.AlreadyReturnedOrExpired ->
                            Return.AlreadyReturnedOrExpired
                        LcpError.Return.ReturnFailed ->
                            Return.ReturnFailed
                        LcpError.Return.UnexpectedServerError ->
                            Return.UnexpectedServerError
                    }
                is LcpError.Runtime ->
                    Runtime(error.message)
                is LcpError.Unknown ->
                    Unknown(error.cause)
            }
    }
}
