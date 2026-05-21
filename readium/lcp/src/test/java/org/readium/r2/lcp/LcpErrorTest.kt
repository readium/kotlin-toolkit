/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LcpErrorTest {

    @Test
    fun `MissingPassphrase message is correct`() {
        assertEquals(
            "Passphrase is not available.",
            LcpError.MissingPassphrase.message
        )
    }

    @Test
    fun `LicenseInteractionNotAvailable message is correct`() {
        assertEquals(
            "This interaction is not available.",
            LcpError.LicenseInteractionNotAvailable.message
        )
    }

    @Test
    fun `LicenseProfileNotSupported message is correct`() {
        assertEquals(
            "This License has a profile identifier that this app cannot handle, the publication cannot be processed",
            LcpError.LicenseProfileNotSupported.message
        )
    }

    @Test
    fun `CrlFetching message is correct`() {
        assertEquals(
            "Can't retrieve the Certificate Revocation List",
            LcpError.CrlFetching.message
        )
    }

    @Test
    fun `Network message is correct`() {
        assertEquals(
            "NetworkError",
            LcpError.Network(cause = null).message
        )
    }

    @Test
    fun `Runtime message is correct`() {
        assertEquals(
            "Unexpected LCP error",
            LcpError.Runtime("details").message
        )
    }

    @Test
    fun `Unknown message is correct`() {
        assertEquals(
            "Unknown LCP error",
            LcpError.Unknown(cause = null).message
        )
    }

    @Test
    fun `LicenseStatus Cancelled message is correct`() {
        val date = Instant.parse("2023-01-01T12:00:00Z")
        assertEquals(
            "This license was cancelled on 2023-01-01T12:00:00Z",
            LcpError.LicenseStatus.Cancelled(date).message
        )
    }

    @Test
    fun `LicenseStatus Returned message is correct`() {
        val date = Instant.parse("2023-01-02T12:00:00Z")
        assertEquals(
            "This license has been returned on 2023-01-02T12:00:00Z",
            LcpError.LicenseStatus.Returned(date).message
        )
    }

    @Test
    fun `LicenseStatus NotStarted message is correct`() {
        val date = Instant.parse("2023-01-03T12:00:00Z")
        assertEquals(
            "This license starts on 2023-01-03T12:00:00Z",
            LcpError.LicenseStatus.NotStarted(date).message
        )
    }

    @Test
    fun `LicenseStatus Expired message is correct`() {
        val date = Instant.parse("2023-01-04T12:00:00Z")
        assertEquals(
            "This license expired on 2023-01-04T12:00:00Z",
            LcpError.LicenseStatus.Expired(date).message
        )
    }

    @Test
    fun `LicenseStatus Revoked message is correct`() {
        val date = Instant.parse("2023-01-05T12:00:00Z")
        assertEquals(
            "This license was revoked by its provider on 2023-01-05T12:00:00Z. It was registered by 3 device(s).",
            LcpError.LicenseStatus.Revoked(date, 3).message
        )
    }

    @Test
    fun `Renew errors messages are correct`() {
        assertEquals(
            "Publication could not be renewed properly",
            LcpError.Renew.RenewFailed.message
        )
        assertEquals(
            "Incorrect renewal period, your publication could not be renewed",
            LcpError.Renew.InvalidRenewalPeriod(null).message
        )
        assertEquals(
            "An unexpected error has occurred on the server",
            LcpError.Renew.UnexpectedServerError.message
        )
    }

    @Test
    fun `Return errors messages are correct`() {
        assertEquals(
            "Publication could not be returned properly",
            LcpError.Return.ReturnFailed.message
        )
        assertEquals(
            "Publication has already been returned before or is expired",
            LcpError.Return.AlreadyReturnedOrExpired.message
        )
        assertEquals(
            "An unexpected error has occurred on the server",
            LcpError.Return.UnexpectedServerError.message
        )
    }

    @Test
    fun `Parsing errors messages are correct`() {
        assertEquals(
            "The JSON is malformed and can't be parsed",
            LcpError.Parsing.MalformedJSON.message
        )
        assertEquals(
            "The JSON is not representing a valid License Document",
            LcpError.Parsing.LicenseDocument.message
        )
        assertEquals(
            "The JSON is not representing a valid Status Document",
            LcpError.Parsing.StatusDocument.message
        )
        assertEquals("The JSON is not representing a valid document", LcpError.Parsing.Link.message)
        assertEquals(
            "The JSON is not representing a valid document",
            LcpError.Parsing.Encryption.message
        )
        assertEquals(
            "The JSON is not representing a valid document",
            LcpError.Parsing.Signature.message
        )
        assertEquals(
            "The JSON is not representing a valid document",
            LcpError.Parsing.Url("hint").message
        )
    }

    @Test
    fun `Container errors messages are correct`() {
        assertEquals("Can't open the license container", LcpError.Container.OpenFailed.message)
        assertEquals(
            "License not found in container",
            LcpError.Container.FileNotFound(Url("test")!!).message
        )
        assertEquals(
            "Can't read license from container",
            LcpError.Container.ReadFailed(Url("test")!!).message
        )
        assertEquals(
            "Can't write license in container",
            LcpError.Container.WriteFailed(Url("test")!!).message
        )
    }

    @Test
    fun `LicenseIntegrity errors messages are correct`() {
        assertEquals(
            "Certificate has been revoked in the CRL",
            LcpError.LicenseIntegrity.CertificateRevoked.message
        )
        assertEquals(
            "Certificate has not been signed by CA",
            LcpError.LicenseIntegrity.InvalidCertificateSignature.message
        )
        assertEquals(
            "License has been issued by an expired certificate",
            LcpError.LicenseIntegrity.InvalidLicenseSignatureDate.message
        )
        assertEquals(
            "License signature does not match",
            LcpError.LicenseIntegrity.InvalidLicenseSignature.message
        )
        assertEquals(
            "User key check invalid",
            LcpError.LicenseIntegrity.InvalidUserKeyCheck.message
        )
    }

    @Test
    fun `Decryption errors messages are correct`() {
        assertEquals(
            "Unable to decrypt encrypted content key from user key",
            LcpError.Decryption.ContentKeyDecryptError.message
        )
        assertEquals(
            "Unable to decrypt encrypted content from content key",
            LcpError.Decryption.ContentDecryptError.message
        )
    }
}
