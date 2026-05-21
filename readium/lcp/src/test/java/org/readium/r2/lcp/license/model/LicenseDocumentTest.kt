/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model

import kotlin.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.lcp.LcpError
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LicenseDocumentTest {

    private val validJsonStr = """
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

    @Test
    fun `parse valid JSON LicenseDocument`() {
        val result = LicenseDocument.fromBytes(data = validJsonStr.toByteArray())

        assertTrue(result.isSuccess)
        val doc = result.getOrNull()!!

        assertEquals("ProviderName", doc.provider)
        assertEquals("doc_id_123", doc.id)
        assertEquals(Instant.parse(input = "2020-01-01T12:00:00Z"), doc.issued)
        assertEquals(Instant.parse(input = "2020-01-02T12:00:00Z"), doc.updated)
        assertEquals("http://readium.org/lcp/basic", doc.encryption.profile)
        assertEquals(2, doc.links.links.size)
        assertEquals("sha-256", doc.signature.algorithm)
        assertEquals("License(doc_id_123)", doc.description)
        assertNotNull(doc.publicationLink)
    }

    @Test
    fun `parsing fails on malformed JSON`() {
        val result = LicenseDocument.fromBytes(data = "not json".toByteArray())

        assertTrue(result.isFailure)
        assertTrue(result.failureOrNull() is LcpError.Parsing.MalformedJSON)
    }

    @Test
    fun `parsing fails when provider is missing`() {
        val json = validJsonStr.replace(""""provider": "ProviderName",""", "")
        val result = LicenseDocument.fromBytes(data = json.toByteArray())

        assertTrue(result.isFailure)
        assertTrue(result.failureOrNull() is LcpError.Parsing.LicenseDocument)
    }

    @Test
    fun `parsing fails when id is missing`() {
        val json = validJsonStr.replace(""""id": "doc_id_123",""", "")
        val result = LicenseDocument.fromBytes(data = json.toByteArray())

        assertTrue(result.isFailure)
        assertTrue(result.failureOrNull() is LcpError.Parsing.LicenseDocument)
    }

    @Test
    fun `parsing fails when issued date is missing`() {
        val json = validJsonStr.replace(""""issued": "2020-01-01T12:00:00Z",""", "")
        val result = LicenseDocument.fromBytes(data = json.toByteArray())

        assertTrue(result.isFailure)
        assertTrue(result.failureOrNull() is LcpError.Parsing.LicenseDocument)
    }

    @Test
    fun `parsing fails when hint link is missing`() {
        val json = JSONObject(validJsonStr)
        json.getJSONArray("links").remove(0)

        val result = LicenseDocument.fromJSON(json = json)

        assertTrue(result.isFailure)
        assertTrue(result.failureOrNull() is LcpError.Parsing.LicenseDocument)
    }

    @Test
    fun `parsing fails when publication link is missing`() {
        val json = JSONObject(validJsonStr)
        json.getJSONArray("links").remove(1)

        val result = LicenseDocument.fromJSON(json = json)

        assertTrue(result.isFailure)
        assertTrue(result.failureOrNull() is LcpError.Parsing.LicenseDocument)
    }

    @Test
    fun `toByteArray returns valid JSON`() {
        val result = LicenseDocument.fromBytes(data = validJsonStr.toByteArray())
        assertTrue(result.isSuccess)
        val doc = result.getOrNull()!!

        val bytes = doc.toByteArray()
        val parsedBack = JSONObject(bytes.decodeToString())

        assertEquals("ProviderName", parsedBack.getString("provider"))
        assertEquals("doc_id_123", parsedBack.getString("id"))
    }

    @Test
    fun `url retrieves and resolves parameter correctly`() {
        val json = JSONObject(validJsonStr)
        val hintLink = json.getJSONArray("links").getJSONObject(0)
        hintLink.put("href", "http://example.com/hint{?user}")
        hintLink.put("templated", true)

        val doc = LicenseDocument.fromJSON(json = json).getOrNull()!!
        val url = doc.url(rel = LicenseDocument.Rel.Hint, parameters = mapOf("user" to "123"))

        assertEquals(Url("http://example.com/hint?user=123")!!, url)
    }
}
