/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.publication.webpub.link.Link
import org.readium.r2.shared.publication.webpub.metadata.Metadata

class WebPublicationTest {

    @Test fun `parse minimal JSON`() {
        assertEquals(
            WebPublication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            WebPublication.fromJSON(JSONObject("""{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
            }"""))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            WebPublication(
                context = listOf("https://readium.org/webpub-manifest/context.jsonld"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/image.png", type = "image/png")),
                tableOfContents = listOf(Link(href = "/cover.html"), Link(href = "/chap1.html")),
                otherCollections = listOf(PublicationCollection(role = "sub", links = listOf(Link(href = "/sublink"))))
            ),
            WebPublication.fromJSON(JSONObject("""{
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
            }"""))
        )
    }

    @Test fun `parse JSON {context} as array`() {
        assertEquals(
            WebPublication(
                context = listOf("context1", "context2"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            WebPublication.fromJSON(JSONObject("""{
                "@context": ["context1", "context2"],
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
            }"""))
        )
    }

    @Test fun `parse JSON requires {metadata}`() {
        assertNull(WebPublication.fromJSON(JSONObject("""{
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
        }""")))
    }

    // {readingOrder} used to be {spine}, so we parse {spine} as a fallback.
    @Test fun `parse JSON {spine} as {readingOrder}`() {
        assertEquals(
            WebPublication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            WebPublication.fromJSON(JSONObject("""{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "spine": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
            }"""))
        )
    }

    @Test fun `parse JSON ignores {links} without {rel}`() {
        assertEquals(
            WebPublication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(
                    Link(href = "/manifest.json", rels = listOf("self")),
                    Link(href = "/withrel", rels = listOf("withrel"))
                ),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            WebPublication.fromJSON(JSONObject("""{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"},
                    {"href": "/withrel", "rel": "withrel"},
                    {"href": "/withoutrel"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"}
                ]
            }"""))
        )
    }

    @Test fun `parse JSON ignores {readingOrder} without {type}`() {
        assertEquals(
            WebPublication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            WebPublication.fromJSON(JSONObject("""{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "/manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html"},
                    {"href": "/chap2.html"}
                ]
            }"""))
        )
    }

    @Test fun `parse JSON ignores {resources} without {type}`() {
        assertEquals(
            WebPublication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/withtype", type = "text/html"))
            ),
            WebPublication.fromJSON(JSONObject("""{
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
            }"""))
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "metadata": {"title": {"UND": "Title"}, "readingProgression": "auto"},
                "links": [
                    {"href": "/manifest.json", "rel": ["self"], "templated": false}
                ],
                "readingOrder": [
                    {"href": "/chap1.html", "type": "text/html", "templated": false}
                ]
            }"""),
            WebPublication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ).toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "@context": ["https://readium.org/webpub-manifest/context.jsonld"],
                "metadata": {"title": {"UND": "Title"}, "readingProgression": "auto"},
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
            WebPublication(
                context = listOf("https://readium.org/webpub-manifest/context.jsonld"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/image.png", type = "image/png")),
                tableOfContents = listOf(Link(href = "/cover.html"), Link(href = "/chap1.html")),
                otherCollections = listOf(PublicationCollection(role = "sub", links = listOf(Link(href = "/sublink"))))
            ).toJSON()
        )
    }

}