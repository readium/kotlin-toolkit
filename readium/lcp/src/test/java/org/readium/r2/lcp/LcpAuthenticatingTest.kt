/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONObject
import org.junit.runner.RunWith
import org.readium.r2.lcp.license.model.LicenseDocument
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LcpAuthenticatingTest {

    @Test
    fun `AuthenticatedLicense computes correct properties from LicenseDocument`() {
        val json = """
            {
              "id": "123",
              "issued": "2020-01-01T00:00:00Z",
              "provider": "https://provider.com",
              "encryption": {
                "profile": "http://readium.org/lcp/profile-1.0",
                "content_key": {
                  "algorithm": "http://www.w3.org/2001/04/xmlenc#aes256-cbc",
                  "encrypted_value": "encrypted_value"
                },
                "user_key": {
                  "algorithm": "http://www.w3.org/2001/04/xmlenc#sha256",
                  "text_hint": "Hint phrase",
                  "key_check": "key_check_value"
                }
              },
              "links": [
                { "rel": "publication", "href": "https://provider.com/publication.epub" },
                { "rel": "hint", "href": "https://provider.com/hint" },
                { "rel": "support", "href": "https://provider.com/support1" },
                { "rel": "support", "href": "https://provider.com/support2" }
              ],
              "user": {
                "id": "user123",
                "name": "Jane Doe",
                "email": "jane@doe.com"
              },
              "signature": {
                "algorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
                "certificate": "cert",
                "value": "sig"
              }
            }
        """.trimIndent()

        val document = LicenseDocument(json = JSONObject(json))
        val authenticatedLicense = LcpAuthenticating.AuthenticatedLicense(document)

        assertEquals("Hint phrase", authenticatedLicense.hint)
        assertEquals("https://provider.com/hint", authenticatedLicense.hintLink?.href?.toString())

        val supportLinks = authenticatedLicense.supportLinks
        assertEquals(2, supportLinks.size)
        assertEquals("https://provider.com/support1", supportLinks[0].href.toString())
        assertEquals("https://provider.com/support2", supportLinks[1].href.toString())

        assertEquals("https://provider.com", authenticatedLicense.provider)
        assertEquals("user123", authenticatedLicense.user.id)
        assertEquals("Jane Doe", authenticatedLicense.user.name)
        assertEquals("jane@doe.com", authenticatedLicense.user.email)
    }
}
