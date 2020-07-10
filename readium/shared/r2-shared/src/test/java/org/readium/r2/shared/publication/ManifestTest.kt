/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */


package org.readium.r2.shared.publication

import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import kotlin.test.assertEquals

class ManifestTest {

    @Test
    fun `parse minimal JSON`() {
        assertEquals(
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = emptyList(),
                readingOrder = emptyList()
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "metadata": {"title": "Title"},
                "links": [],
                "readingOrder": []
            }"""
                )
            )
        )
    }

    @Test
    fun `parse full JSON`() {
        assertEquals(
            Manifest(
                context = listOf("https://readium.org/webpub-manifest/context.jsonld"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/image.png", type = "image/png")),
                tableOfContents = listOf(Link(href = "/cover.html"), Link(href = "/chap1.html")),
                subcollections = mapOf("sub" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink")))))
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "@context": "https://readium.org/webpub-manifest/context.jsonld",
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"}
                ],
                "resources": [
                    {"href": "/image.png", "type": "image/png"}
                ],
                "toc": [
                    {"href": "/cover.html"},
                    {"href": "/chap1.html"}
                ],
                "sub": {
                    "links": [
                        {"href": "/sublink"}
                    ]
                }
            }"""
                )
            )
        )
    }

    @Test
    fun `parse JSON {context} as array`() {
        assertEquals(
            Manifest(
                context = listOf("context1", "context2"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "@context": ["context1", "context2"],
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
            }"""
                )
            )
        )
    }

    @Test
    fun `parse JSON requires {metadata}`() {
        Assert.assertNull(
            Manifest.fromJSON(
                JSONObject(
                    """{
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
        }"""
                )
            )
        )
    }

    // {readingOrder} used to be {spine}, so we parse {spine} as a fallback.
    @Test
    fun `parse JSON {spine} as {readingOrder}`() {
        assertEquals(
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "spine": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
            }"""
                )
            )
        )
    }

    @Test
    fun `parse JSON ignores {readingOrder} without {type}`() {
        assertEquals(
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"},
                    {"href": "/chap2.html"}
                ]
            }"""
                )
            )
        )
    }

    @Test
    fun `parse JSON ignores {resources} without {type}`() {
        assertEquals(
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/withtype", type = "text/html"))
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"},
                ],
                "resources": [
                    {"href": "/withtype", "type": "text/html"},
                    {"href": "/withouttype"}
                ]
            }"""
                )
            )
        )
    }

    @Test
    fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "metadata": {"title": {"und": "Title"}, "readingProgression": "auto"},
                "links": [],
                "readingOrder": []
            }"""),
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = emptyList(),
                readingOrder = emptyList()
            ).toJSON()
        )
    }

    @Test
    fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "@context": ["https://readium.org/webpub-manifest/context.jsonld"],
                "metadata": {"title": {"und": "Title"}, "readingProgression": "auto"},
                "links": [
                    {"href": "/manifest.json", "rel": ["self"], "templated": false}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html", "templated": false}
                ],
                "resources": [
                    {"href": "/image.png", "type": "image/png", "templated": false}
                ],
                "toc": [
                    {"href": "/cover.html", "templated": false},
                    {"href": "/chap1.html", "templated": false}
                ],
                "sub": {
                    "metadata": {},
                    "links": [
                        {"href": "/sublink", "templated": false}
                    ]
                }
            }"""),
            Manifest(
                context = listOf("https://readium.org/webpub-manifest/context.jsonld"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/image.png", type = "image/png")),
                tableOfContents = listOf(Link(href = "/cover.html"), Link(href = "/chap1.html")),
                subcollections = mapOf("sub" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink")))))
            ).toJSON()
        )
    }

    @Test
    fun `self link is replaced when parsing a package`() {
         assertEquals(
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("alternate")))
            ),
            Manifest.fromJSON(
                JSONObject("""{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": ["self"], "templated": false}
                ]
                }"""),
                packaged = true
            )
        )
    }

    @Test
    fun `self link is kept when parsing a remote manifest`() {
        assertEquals(
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = setOf("self")))
            ),
            Manifest.fromJSON(
                JSONObject("""{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": ["self"]}
                ]
                }""")
            )
        )
    }

    @Test
    fun `href are resolved to root when parsing a package`() {
        val json =  JSONObject("""{
            "metadata": {"title": "Title"},
            "links": [
                {"href": "http://example.com/manifest.json", "rel": ["self"], "templated": false}
            ],
            "readingOrder": [
                {"href": "chap1.html", "type": "text/html", "templated": false}
            ]
        }""")

        assertEquals(
            "/chap1.html",
            Manifest.fromJSON(json, packaged = true)?.readingOrder?.first()?.href
        )
    }

    @Test
    fun `href are resolved to self link when parsing a remote manifest`() {
        val json = JSONObject("""{
            "metadata": {"title": "Title"},
            "links": [
                {"href": "http://example.com/directory/manifest.json", "rel": ["self"], "templated": false}
            ],
            "readingOrder": [
                {"href": "chap1.html", "type": "text/html", "templated": false}
            ]
        }""")

        assertEquals(
            "http://example.com/directory/chap1.html",
            Manifest.fromJSON(json)?.readingOrder?.first()?.href
        )
    }

}
