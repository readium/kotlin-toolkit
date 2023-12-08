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
        UserError(R.string.lcp_error_license_interaction_not_available)
    LcpError.LicenseProfileNotSupported ->
        UserError(R.string.lcp_error_license_profile_not_supported)
    LcpError.CrlFetching ->
        UserError(R.string.lcp_error_crl_fetching)
    is LcpError.Network ->
        UserError(R.string.lcp_error_network)

    is LcpError.Runtime ->
        UserError(R.string.lcp_error_runtime)
    is LcpError.Unknown ->
        UserError(R.string.lcp_error_unknown)

    is LcpError.Container ->
        when (this) {
            is LcpError.Container.FileNotFound ->
                UserError(R.string.lcp_error_container_file_not_found)
            LcpError.Container.OpenFailed ->
                UserError(R.string.lcp_error_container_open_failed)
            is LcpError.Container.ReadFailed ->
                UserError(R.string.lcp_error_container_read_failed)
            is LcpError.Container.WriteFailed ->
                UserError(R.string.lcp_error_container_write_failed)
        }

    is LcpError.Decryption ->
        when (this) {
            LcpError.Decryption.ContentDecryptError ->
                UserError(R.string.lcp_error_decryption_content_decrypt_error)
            LcpError.Decryption.ContentKeyDecryptError ->
                UserError(R.string.lcp_error_decryption_content_key_decrypt_error)
        }

    is LcpError.LicenseIntegrity ->
        when (this) {
            LcpError.LicenseIntegrity.CertificateRevoked ->
                UserError(R.string.lcp_error_license_integrity_certificate_revoked)
            LcpError.LicenseIntegrity.InvalidCertificateSignature ->
                UserError(R.string.lcp_error_license_integrity_invalid_certificate_signature)
            LcpError.LicenseIntegrity.InvalidLicenseSignature ->
                UserError(R.string.lcp_error_license_integrity_invalid_license_signature)
            LcpError.LicenseIntegrity.InvalidLicenseSignatureDate ->
                UserError(R.string.lcp_error_license_integrity_invalid_license_signature_date)
            LcpError.LicenseIntegrity.InvalidUserKeyCheck ->
                UserError(R.string.lcp_error_license_integrity_invalid_user_key_check)
        }

    is LcpError.LicenseStatus ->
        when (this) {
            is LcpError.LicenseStatus.Cancelled ->
                UserError(R.string.lcp_error_license_status_cancelled, date)
            is LcpError.LicenseStatus.Expired ->
                UserError(R.string.lcp_error_license_status_expired, end)
            is LcpError.LicenseStatus.NotStarted ->
                UserError(R.string.lcp_error_license_status_not_started, start)
            is LcpError.LicenseStatus.Returned ->
                UserError(R.string.lcp_error_license_status_returned, date)
            is LcpError.LicenseStatus.Revoked ->
                UserError(
                    R.plurals.lcp_error_license_status_revoked,
                    devicesCount,
                    date,
                    devicesCount
                )
        }

    is LcpError.Parsing ->
        when (this) {
            LcpError.Parsing.LicenseDocument ->
                UserError(R.string.lcp_error_parsing_license_document)
            LcpError.Parsing.MalformedJSON ->
                UserError(R.string.lcp_error_parsing_malformed_json)
            LcpError.Parsing.StatusDocument ->
                UserError(R.string.lcp_error_parsing_license_document)
            else ->
                UserError(R.string.lcp_error_parsing)
        }

    is LcpError.Renew ->
        when (this) {
            is LcpError.Renew.InvalidRenewalPeriod ->
                UserError(R.string.lcp_error_renew_invalid_renewal_period)
            LcpError.Renew.RenewFailed ->
                UserError(R.string.lcp_error_renew_renew_failed)
            LcpError.Renew.UnexpectedServerError ->
                UserError(R.string.lcp_error_renew_unexpected_server_error)
        }

    is LcpError.Return ->
        when (this) {
            LcpError.Return.AlreadyReturnedOrExpired ->
                UserError(R.string.lcp_error_return_already_returned_or_expired)
            LcpError.Return.ReturnFailed ->
                UserError(R.string.lcp_error_return_return_failed)
            LcpError.Return.UnexpectedServerError ->
                UserError(R.string.lcp_error_return_unexpected_server_error)
        }
}
