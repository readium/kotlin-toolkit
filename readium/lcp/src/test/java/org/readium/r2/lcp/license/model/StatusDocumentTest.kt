/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model

import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.components.lsd.Event
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StatusDocumentTest {

    private val validJsonStr = """
        {
            "id": "12345",
            "status": "ready",
            "message": "Ready to be downloaded",
            "updated": {
                "license": "2020-01-01T12:00:00Z",
                "status": "2020-01-01T12:05:00Z"
            },
            "links": [
                {
                    "href": "http://example.com/license",
                    "rel": "license"
                },
                {
                    "href": "http://example.com/register{?id,name}",
                    "rel": "register",
                    "templated": true
                }
            ],
            "potential_rights": {
                "end": "2020-02-01T12:00:00Z"
            },
            "events": [
                {
                    "type": "register",
                    "name": "Registration",
                    "id": "event_1",
                    "timestamp": "2020-01-01T12:06:00Z"
                },
                {
                    "type": "renew",
                    "name": "Renewal",
                    "id": "event_2",
                    "timestamp": "2020-01-10T12:06:00Z"
                }
            ]
        }
    """.trimIndent()

    @Test
    fun `parse valid JSON StatusDocument`() {
        val doc = StatusDocument(data = validJsonStr.toByteArray())

        assertEquals("12345", doc.id)
        assertEquals(StatusDocument.Status.Ready, doc.status)
        assertEquals("Ready to be downloaded", doc.message)
        assertEquals(Instant.parse(input = "2020-01-01T12:00:00Z"), doc.licenseUpdated)
        assertEquals(Instant.parse(input = "2020-01-01T12:05:00Z"), doc.statusUpdated)
        assertEquals(2, doc.links.links.size)
        assertNotNull(doc.potentialRights)
        assertEquals(2, doc.events.size)
        assertEquals("Status(ready)", doc.description)
    }

    @Test
    fun `parsing fails on malformed JSON`() {
        val exception = assertThrows(LcpException::class.java) {
            StatusDocument(data = "not json".toByteArray())
        }
        assertTrue(exception.error is LcpError.Parsing.MalformedJSON)
    }

    @Test
    fun `parsing fails when id is missing`() {
        val json = validJsonStr.replace(""""id": "12345",""", "")
        val exception = assertThrows(LcpException::class.java) {
            StatusDocument(data = json.toByteArray())
        }
        assertTrue(exception.error is LcpError.Parsing)
    }

    @Test
    fun `parsing fails when status is missing`() {
        val json = validJsonStr.replace(""""status": "ready",""", "")
        val exception = assertThrows(LcpException::class.java) {
            StatusDocument(data = json.toByteArray())
        }
        assertTrue(exception.error is LcpError.Parsing)
    }

    @Test
    fun `parsing fails when message is missing`() {
        val json = validJsonStr.replace(""""message": "Ready to be downloaded",""", "")
        val exception = assertThrows(LcpException::class.java) {
            StatusDocument(data = json.toByteArray())
        }
        assertTrue(exception.error is LcpError.Parsing)
    }

    @Test
    fun `parsing fails when updated license is missing`() {
        val json = validJsonStr.replace(""""license": "2020-01-01T12:00:00Z",""", "")
        val exception = assertThrows(LcpException::class.java) {
            StatusDocument(data = json.toByteArray())
        }
        assertTrue(exception.error is LcpError.Parsing)
    }

    @Test
    fun `parsing fails when links are missing`() {
        val jsonStrWithoutLinks = """
        {
            "id": "12345",
            "status": "ready",
            "message": "Ready to be downloaded",
            "updated": {
                "license": "2020-01-01T12:00:00Z",
                "status": "2020-01-01T12:05:00Z"
            },
            "potential_rights": {
                "end": "2020-02-01T12:00:00Z"
            },
            "events": [
                {
                    "type": "register",
                    "name": "Registration",
                    "id": "event_1",
                    "timestamp": "2020-01-01T12:06:00Z"
                }
            ]
        }
        """.trimIndent()
        val exception = assertThrows(LcpException::class.java) {
            StatusDocument(data = jsonStrWithoutLinks.toByteArray())
        }
        assertTrue(exception.error is LcpError.Parsing)
    }

    @Test
    fun `link retrieves matching link`() {
        val doc = StatusDocument(data = validJsonStr.toByteArray())

        val licenseLink = doc.link(rel = StatusDocument.Rel.License)
        assertNotNull(licenseLink)
        assertEquals("http://example.com/license", licenseLink?.href?.toString())

        val nonExistentLink = doc.link(rel = StatusDocument.Rel.Return)
        assertNull(nonExistentLink)
    }

    @Test
    fun `url resolves parameter for templated link`() {
        val doc = StatusDocument(data = validJsonStr.toByteArray())

        val url = doc.url(
            rel = StatusDocument.Rel.Register,
            parameters = mapOf("id" to "123", "name" to "test")
        )
        assertEquals(Url("http://example.com/register?id=123&name=test")!!, url)
    }

    @Test
    fun `url throws LcpException when rel is not found`() {
        val doc = StatusDocument(data = validJsonStr.toByteArray())

        val exception = assertThrows(LcpException::class.java) {
            doc.url(rel = StatusDocument.Rel.Return)
        }
        assertTrue(exception.error is LcpError.Parsing.Url)
    }

    @Test
    fun `events filters by EventType enum`() {
        val doc = StatusDocument(data = validJsonStr.toByteArray())

        val registerEvents = doc.events(type = Event.EventType.Register)
        assertEquals(1, registerEvents.size)
        assertEquals("event_1", registerEvents[0].id)
    }

    @Test
    fun `events filters by type string`() {
        val doc = StatusDocument(data = validJsonStr.toByteArray())

        val renewEvents = doc.events(type = "renew")
        assertEquals(1, renewEvents.size)
        assertEquals("event_2", renewEvents[0].id)
    }
}
