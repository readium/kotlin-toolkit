/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.auth

import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.license.model.LicenseDocument
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LcpPassphraseAuthenticationTest {

    private class FakeFallbackAuth(val returnedPassphrase: String?) : LcpAuthenticating {
        var called = false
        var lastReason: LcpAuthenticating.AuthenticationReason? = null

        override suspend fun retrievePassphrase(
            license: LcpAuthenticating.AuthenticatedLicense,
            reason: LcpAuthenticating.AuthenticationReason,
            allowUserInteraction: Boolean,
        ): String? {
            called = true
            lastReason = reason
            return returnedPassphrase
        }
    }

    private val validLicenseJson = JSONObject(
        """
        {
          "provider": "ProviderName",
          "id": "doc_id_123",
          "issued": "2020-01-01T12:00:00Z",
          "updated": "2020-01-02T12:00:00Z",
          "encryption": {
            "profile": "http://readium.org/lcp/basic",
            "content_key": {
              "algorithm": "aes-256-cbc",
              "encrypted_value": "xxxx"
            },
            "user_key": {
              "text_hint": "Enter your password",
              "algorithm": "sha-256",
              "key_check": "yyyy"
            }
          },
          "links": [
            {
              "href": "http://example.com/hint",
              "rel": "hint"
            },
            {
              "href": "http://example.com/publication.epub",
              "rel": "publication"
            }
          ],
          "signature": {
            "algorithm": "sha-256",
            "certificate": "cert_content",
            "value": "sig_val"
          }
        }
        """.trimIndent()
    )

    private val fakeLicense = LcpAuthenticating.AuthenticatedLicense(
        document = LicenseDocument.fromJSON(json = validLicenseJson).getOrNull()!!
    )

    @Test
    fun `returns passphrase when reason is PassphraseNotFound`() = runTest {
        val auth = LcpPassphraseAuthentication(passphrase = "my_secret_passphrase")

        val result = auth.retrievePassphrase(
            license = fakeLicense,
            reason = LcpAuthenticating.AuthenticationReason.PassphraseNotFound,
            allowUserInteraction = true
        )

        assertEquals("my_secret_passphrase", result)
    }

    @Test
    fun `delegates to fallback when reason is not PassphraseNotFound`() = runTest {
        val fallback = FakeFallbackAuth(returnedPassphrase = "fallback_passphrase")
        val auth =
            LcpPassphraseAuthentication(passphrase = "my_secret_passphrase", fallback = fallback)

        val result = auth.retrievePassphrase(
            license = fakeLicense,
            reason = LcpAuthenticating.AuthenticationReason.InvalidPassphrase,
            allowUserInteraction = true
        )

        assertEquals("fallback_passphrase", result)
        assertEquals(true, fallback.called)
        assertEquals(LcpAuthenticating.AuthenticationReason.InvalidPassphrase, fallback.lastReason)
    }

    @Test
    fun `returns null when reason is not PassphraseNotFound and there is no fallback`() = runTest {
        val auth = LcpPassphraseAuthentication(passphrase = "my_secret_passphrase", fallback = null)

        val result = auth.retrievePassphrase(
            license = fakeLicense,
            reason = LcpAuthenticating.AuthenticationReason.InvalidPassphrase,
            allowUserInteraction = true
        )

        assertNull(result)
    }

    @Test
    fun `does not delegate to fallback when reason is PassphraseNotFound`() = runTest {
        val fallback = FakeFallbackAuth(returnedPassphrase = "fallback_passphrase")
        val auth =
            LcpPassphraseAuthentication(passphrase = "my_secret_passphrase", fallback = fallback)

        val result = auth.retrievePassphrase(
            license = fakeLicense,
            reason = LcpAuthenticating.AuthenticationReason.PassphraseNotFound,
            allowUserInteraction = true
        )

        assertEquals("my_secret_passphrase", result)
        assertEquals(false, fallback.called)
    }
}
