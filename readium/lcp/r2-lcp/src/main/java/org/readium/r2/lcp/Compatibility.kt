package org.readium.r2.lcp

import kotlinx.coroutines.suspendCancellableCoroutine
import org.readium.r2.lcp.public.*
import kotlin.coroutines.resume


internal fun LCPAuthenticating?.toLcpAuthenticating(): LcpAuthenticating {

    fun LcpAuthenticating.AuthenticatedLicense.toLCPAuthenticatedLicense(): LCPAuthenticatedLicense =
        LCPAuthenticatedLicense(document)

    fun LcpAuthenticating.AuthenticationReason.toLCPAuthenticationReason(): LCPAuthenticationReason = when(this) {
        LcpAuthenticating.AuthenticationReason.InvalidPassphrase -> LCPAuthenticationReason.invalidPassphrase
        LcpAuthenticating.AuthenticationReason.PassphraseNotFound -> LCPAuthenticationReason.passphraseNotFound
    }

    return when(this) {

        null -> object : LcpAuthenticating {
            override suspend fun retrievePassphrase(
                license: LcpAuthenticating.AuthenticatedLicense,
                reason: LcpAuthenticating.AuthenticationReason,
                allowUserInteraction: Boolean,
                sender: Any?
            ): String? = null
        }

        else -> object : LcpAuthenticating {

            override suspend fun retrievePassphrase(
                license: LcpAuthenticating.AuthenticatedLicense,
                reason: LcpAuthenticating.AuthenticationReason,
                allowUserInteraction: Boolean,
                sender: Any?
            ): String? = suspendCancellableCoroutine { cont ->
                this@toLcpAuthenticating.requestPassphrase(license.toLCPAuthenticatedLicense(), reason.toLCPAuthenticationReason()) {
                    if (cont.isActive) {
                        cont.resume(it)
                    }
                }
            }
        }
    }
}

internal fun LcpException.toLCPError(): LCPError = LCPError.wrap(
    when(this) {
        is LcpException.LicenseStatus.Cancelled -> StatusError.cancelled(date)
        is LcpException.LicenseStatus.Expired -> StatusError.expired(start, end)
        is LcpException.LicenseStatus.Returned -> StatusError.returned(date)
        is LcpException.LicenseStatus.Revoked -> StatusError.revoked(date, devicesCount)

        is LcpException.Renew.InvalidRenewalPeriod -> RenewError.invalidRenewalPeriod(maxRenewDate)
        LcpException.Renew.RenewFailed -> RenewError.renewFailed
        LcpException.Renew.UnexpectedServerError -> RenewError.unexpectedServerError

        LcpException.Return.AlreadyReturnedOrExpired -> ReturnError.alreadyReturnedOrExpired
        LcpException.Return.ReturnFailed -> ReturnError.returnFailed
        LcpException.Return.UnexpectedServerError -> ReturnError.unexpectedServerError

        LcpException.Parsing.Encryption -> ParsingError.encryption
        LcpException.Parsing.LicenseDocument -> ParsingError.licenseDocument
        LcpException.Parsing.Link -> ParsingError.link
        LcpException.Parsing.MalformedJSON -> ParsingError.malformedJSON
        LcpException.Parsing.Signature -> ParsingError.signature
        LcpException.Parsing.StatusDocument -> ParsingError.statusDocument
        is LcpException.Parsing.Url -> ParsingError.url(rel)

        LcpException.Container.OpenFailed -> ContainerError.openFailed
        is LcpException.Container.FileNotFound -> ContainerError.fileNotFound(path)
        is LcpException.Container.ReadFailed -> ContainerError.readFailed(path)
        is LcpException.Container.WriteFailed -> ContainerError.writeFailed(path)

        LcpException.LicenseInteractionNotAvailable -> LCPError.licenseInteractionNotAvailable
        LcpException.LicenseProfileNotSupported -> LCPError.licenseProfileNotSupported
        LcpException.CrlFetching -> LCPError.crlFetching
        is LcpException.Network -> LCPError.network(cause)
        is LcpException.Runtime -> LCPError.runtime(message)
        is LcpException.Unknown -> LCPError.unknown(cause)

        LcpException.LicenseIntegrity.CertificateRevoked -> LCPClientError.certificateRevoked
        LcpException.LicenseIntegrity.CertificateSignatureInvalid -> LCPClientError.certificateSignatureInvalid
        LcpException.LicenseIntegrity.LicenseSignatureDateInvalid -> LCPClientError.licenseSignatureDateInvalid
        LcpException.LicenseIntegrity.LicenseSignatureInvalid -> LCPClientError.licenseSignatureInvalid

        LcpException.LicenseIntegrity.UserKeyCheckInvalid -> LCPClientError.userKeyCheckInvalid

        LcpException.Decryption.ContentKeyDecryptError -> LCPClientError.contentKeyDecryptError
        LcpException.Decryption.ContentDecryptError -> LCPClientError.contentDecryptError
    }
)

internal fun LcpService.ImportedPublication.toLCPImportedPublication() : LCPImportedPublication =
    LCPImportedPublication(localURL = localURL, suggestedFilename = suggestedFilename)