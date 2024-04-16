/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import org.readium.r2.lcp.LcpError
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

fun LcpError.toUserError(): UserError = when (this) {
    LcpError.LicenseInteractionNotAvailable ->
        UserError(R.string.lcp_error_license_interaction_not_available, cause = this)
    LcpError.LicenseProfileNotSupported ->
        UserError(R.string.lcp_error_license_profile_not_supported, cause = this)
    LcpError.CrlFetching ->
        UserError(R.string.lcp_error_crl_fetching, cause = this)
    is LcpError.Network ->
        UserError(R.string.lcp_error_network, cause = this)

    is LcpError.Runtime, LcpError.MissingPassphrase ->
        UserError(R.string.lcp_error_runtime, cause = this)
    is LcpError.Unknown ->
        UserError(R.string.lcp_error_unknown, cause = this)

    is LcpError.Container ->
        when (this) {
            is LcpError.Container.FileNotFound ->
                UserError(R.string.lcp_error_container_file_not_found, cause = this)
            LcpError.Container.OpenFailed ->
                UserError(R.string.lcp_error_container_open_failed, cause = this)
            is LcpError.Container.ReadFailed ->
                UserError(R.string.lcp_error_container_read_failed, cause = this)
            is LcpError.Container.WriteFailed ->
                UserError(R.string.lcp_error_container_write_failed, cause = this)
        }

    is LcpError.Decryption ->
        when (this) {
            LcpError.Decryption.ContentDecryptError ->
                UserError(R.string.lcp_error_decryption_content_decrypt_error, cause = this)
            LcpError.Decryption.ContentKeyDecryptError ->
                UserError(R.string.lcp_error_decryption_content_key_decrypt_error, cause = this)
        }

    is LcpError.LicenseIntegrity ->
        when (this) {
            LcpError.LicenseIntegrity.CertificateRevoked ->
                UserError(R.string.lcp_error_license_integrity_certificate_revoked, cause = this)
            LcpError.LicenseIntegrity.InvalidCertificateSignature ->
                UserError(
                    R.string.lcp_error_license_integrity_invalid_certificate_signature,
                    cause = this
                )
            LcpError.LicenseIntegrity.InvalidLicenseSignature ->
                UserError(
                    R.string.lcp_error_license_integrity_invalid_license_signature,
                    cause = this
                )
            LcpError.LicenseIntegrity.InvalidLicenseSignatureDate ->
                UserError(
                    R.string.lcp_error_license_integrity_invalid_license_signature_date,
                    cause = this
                )
            LcpError.LicenseIntegrity.InvalidUserKeyCheck ->
                UserError(R.string.lcp_error_license_integrity_invalid_user_key_check, cause = this)
        }

    is LcpError.LicenseStatus ->
        when (this) {
            is LcpError.LicenseStatus.Cancelled ->
                UserError(R.string.lcp_error_license_status_cancelled, date, cause = this)
            is LcpError.LicenseStatus.Expired ->
                UserError(R.string.lcp_error_license_status_expired, end, cause = this)
            is LcpError.LicenseStatus.NotStarted ->
                UserError(R.string.lcp_error_license_status_not_started, start, cause = this)
            is LcpError.LicenseStatus.Returned ->
                UserError(R.string.lcp_error_license_status_returned, date, cause = this)
            is LcpError.LicenseStatus.Revoked ->
                UserError(
                    R.plurals.lcp_error_license_status_revoked,
                    devicesCount,
                    date,
                    devicesCount,
                    cause = this
                )
        }

    is LcpError.Parsing ->
        when (this) {
            LcpError.Parsing.LicenseDocument ->
                UserError(R.string.lcp_error_parsing_license_document, cause = this)
            LcpError.Parsing.MalformedJSON ->
                UserError(R.string.lcp_error_parsing_malformed_json, cause = this)
            LcpError.Parsing.StatusDocument ->
                UserError(R.string.lcp_error_parsing_license_document, cause = this)
            else ->
                UserError(R.string.lcp_error_parsing, cause = this)
        }

    is LcpError.Renew ->
        when (this) {
            is LcpError.Renew.InvalidRenewalPeriod ->
                UserError(R.string.lcp_error_renew_invalid_renewal_period, cause = this)
            LcpError.Renew.RenewFailed ->
                UserError(R.string.lcp_error_renew_renew_failed, cause = this)
            LcpError.Renew.UnexpectedServerError ->
                UserError(R.string.lcp_error_renew_unexpected_server_error, cause = this)
        }

    is LcpError.Return ->
        when (this) {
            LcpError.Return.AlreadyReturnedOrExpired ->
                UserError(R.string.lcp_error_return_already_returned_or_expired, cause = this)
            LcpError.Return.ReturnFailed ->
                UserError(R.string.lcp_error_return_return_failed, cause = this)
            LcpError.Return.UnexpectedServerError ->
                UserError(R.string.lcp_error_return_unexpected_server_error, cause = this)
        }
}
