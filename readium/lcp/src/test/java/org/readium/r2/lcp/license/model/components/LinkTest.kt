/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model.components

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkTest {

    @Test
    fun `parse valid JSON link with minimal required fields`() {
        val json = JSONObject(
            """
            {
                "href": "http://example.com/publication.epub",
                "rel": "publication"
            }
        """
        )

        val link = Link(json = json)

        assertEquals("http://example.com/publication.epub", link.href.toString())
        assertEquals(false, link.href.isTemplated)
        assertEquals(setOf("publication"), link.rels)
        assertNull(link.mediaType)
        assertNull(link.title)
        assertNull(link.profile)
        assertNull(link.length)
        assertNull(link.hash)
    }

    @Test
    fun `parse valid JSON link with all fields`() {
        val json = JSONObject(
            """
            {
                "href": "http://example.com/search{?query}",
                "templated": true,
                "type": "application/opds+json",
                "title": "Search",
                "rel": ["search", "index"],
                "profile": "http://opds-spec.org",
                "length": 1024,
                "hash": "abcdef123456"
            }
        """
        )

        val link = Link(json = json)

        assertEquals("http://example.com/search{?query}", link.href.toString())
        assertEquals(true, link.href.isTemplated)
        assertEquals(MediaType("application/opds+json"), link.mediaType)
        assertEquals("Search", link.title)
        assertEquals(setOf("search", "index"), link.rels)
        assertEquals("http://opds-spec.org", link.profile)
        assertEquals(1024, link.length)
        assertEquals("abcdef123456", link.hash)
    }

    @Test
    fun `parsing throws when href is missing`() {
        val json = JSONObject(
            """
            {
                "rel": "publication"
            }
        """
        )

        val exception = assertThrows(LcpException::class.java) {
            Link(json = json)
        }
        assertTrue(exception.error is LcpError.Parsing)
    }

    @Test
    fun `parsing throws when rel is missing`() {
        val json = JSONObject(
            """
            {
                "href": "http://example.com/publication.epub"
            }
        """
        )

        val exception = assertThrows(LcpException::class.java) {
            Link(json = json)
        }
        assertTrue(exception.error is LcpError.Parsing)
    }

    @Test
    fun `url resolves non-templated href`() {
        val link = Link(
            href = Href(href = "http://example.com/path")!!,
            rels = setOf("test")
        )
        val url = link.url()
        assertEquals(Url("http://example.com/path")!!, url)
    }

    @Test
    fun `url resolves templated href with parameters`() {
        val link = Link(
            href = Href(href = "http://example.com/search{?query,lang}", templated = true)!!,
            rels = setOf("search")
        )
        val url = link.url(parameters = mapOf("query" to "test", "lang" to "en"))
        assertEquals(Url("http://example.com/search?query=test&lang=en")!!, url)
    }
}
