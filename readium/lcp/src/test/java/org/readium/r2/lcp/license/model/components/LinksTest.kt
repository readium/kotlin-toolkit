/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model.components

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinksTest {

    @Test
    fun `parses valid JSONArray ignoring invalid entries`() {
        val jsonArray = JSONArray(
            """
            [
                {
                    "href": "http://example.com/publication.epub",
                    "rel": "publication"
                },
                {
                    "rel": "invalid_missing_href"
                },
                "Not a JSONObject",
                {
                    "href": "http://example.com/search",
                    "rel": ["search"]
                }
            ]
        """
        )

        val links = Links(json = jsonArray)

        assertEquals(2, links.links.size)
        assertEquals(setOf("publication"), links.links[0].rels)
        assertEquals(setOf("search"), links.links[1].rels)
    }

    @Test
    fun `firstWithRel returns the first matching link`() {
        val jsonArray = JSONArray(
            """
            [
                { "href": "http://example.com/publication1", "rel": "publication" },
                { "href": "http://example.com/publication2", "rel": "publication" }
            ]
        """
        )
        val links = Links(json = jsonArray)

        val firstLink = links.firstWithRel(rel = "publication")
        assertEquals("http://example.com/publication1", firstLink?.href?.toString())
    }

    @Test
    fun `firstWithRel returns null if no link matches`() {
        val jsonArray = JSONArray(
            """
            [
                { "href": "http://example.com/publication", "rel": "publication" }
            ]
        """
        )
        val links = Links(json = jsonArray)

        assertNull(links.firstWithRel(rel = "search"))
    }

    @Test
    fun `firstWithRel matches mediaType`() {
        val jsonArray = JSONArray(
            """
            [
                { "href": "http://example.com/pub1", "rel": "publication", "type": "application/pdf" },
                { "href": "http://example.com/pub2", "rel": "publication", "type": "application/epub+zip" }
            ]
        """
        )
        val links = Links(json = jsonArray)

        val pdfLink =
            links.firstWithRel(rel = "publication", type = MediaType(string = "application/pdf"))
        assertEquals("http://example.com/pub1", pdfLink?.href?.toString())

        val epubLink = links.firstWithRel(
            rel = "publication",
            type = MediaType(string = "application/epub+zip")
        )
        assertEquals("http://example.com/pub2", epubLink?.href?.toString())
    }

    @Test
    fun `allWithRel returns all matching links`() {
        val jsonArray = JSONArray(
            """
            [
                { "href": "http://example.com/pub1", "rel": "publication", "type": "application/pdf" },
                { "href": "http://example.com/search", "rel": "search" },
                { "href": "http://example.com/pub2", "rel": "publication", "type": "application/epub+zip" }
            ]
        """
        )
        val links = Links(json = jsonArray)

        val publicationLinks = links.allWithRel(rel = "publication")
        assertEquals(2, publicationLinks.size)
        assertEquals("http://example.com/pub1", publicationLinks[0].href.toString())
        assertEquals("http://example.com/pub2", publicationLinks[1].href.toString())
    }

    @Test
    fun `get operator works like allWithRel`() {
        val jsonArray = JSONArray(
            """
            [
                { "href": "http://example.com/pub1", "rel": "publication" },
                { "href": "http://example.com/search", "rel": "search" },
                { "href": "http://example.com/pub2", "rel": "publication" }
            ]
        """
        )
        val links = Links(json = jsonArray)

        val publicationLinks = links["publication"]
        assertEquals(2, publicationLinks.size)
    }

    @Test
    fun `firstWithRelAndNoType returns the first link matching rel without a media type`() {
        val jsonArray = JSONArray(
            """
            [
                { "href": "http://example.com/pub1", "rel": "publication", "type": "application/pdf" },
                { "href": "http://example.com/pub2", "rel": "publication" },
                { "href": "http://example.com/pub3", "rel": "publication" }
            ]
        """
        )
        val links = Links(json = jsonArray)

        val firstLinkNoType = links.firstWithRelAndNoType(rel = "publication")
        assertEquals("http://example.com/pub2", firstLinkNoType?.href?.toString())
        assertNull(firstLinkNoType?.mediaType)
    }
}
