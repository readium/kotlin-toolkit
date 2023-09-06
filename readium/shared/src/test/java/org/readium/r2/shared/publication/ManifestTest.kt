/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import kotlin.test.assertEquals
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
                links = listOf(Link(href = Href("manifest.json")!!, rels = setOf("self"))),
                readingOrder = listOf(
                    Link(href = Href("chap1.html")!!, mediaType = MediaType.HTML)
                ),
                resources = listOf(Link(href = Href("image.png")!!, mediaType = MediaType.PNG)),
                tableOfContents = listOf(
                    Link(href = Href("cover.html")!!),
                    Link(href = Href("chap1.html")!!)
                ),
                subcollections = mapOf(
                    "sub" to listOf(
                        PublicationCollection(links = listOf(Link(href = Href("sublink")!!)))
                    )
                )
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "@context": "https://readium.org/webpub-manifest/context.jsonld",
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "chap1.html", "type": "text/html"}
                ],
                "resources": [
                    {"href": "image.png", "type": "image/png"}
                ],
                "toc": [
                    {"href": "cover.html"},
                    {"href": "chap1.html"}
                ],
                "sub": {
                    "links": [
                        {"href": "sublink"}
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
                links = listOf(Link(href = Href("manifest.json")!!, rels = setOf("self"))),
                readingOrder = listOf(
                    Link(href = Href("chap1.html")!!, mediaType = MediaType.HTML)
                )
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "@context": ["context1", "context2"],
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "chap1.html", "type": "text/html"}
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
                    {"href": "manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "chap1.html", "type": "text/html"}
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
                links = listOf(Link(href = Href("manifest.json")!!, rels = setOf("self"))),
                readingOrder = listOf(
                    Link(href = Href("chap1.html")!!, mediaType = MediaType.HTML)
                )
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "manifest.json", "rel": "self"}
                ],
                "spine": [
                    {"href": "chap1.html", "type": "text/html"}
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
                links = listOf(Link(href = Href("manifest.json")!!, rels = setOf("self"))),
                readingOrder = listOf(
                    Link(href = Href("chap1.html")!!, mediaType = MediaType.HTML)
                )
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "chap1.html", "type": "text/html"},
                    {"href": "chap2.html"}
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
                links = listOf(Link(href = Href("manifest.json")!!, rels = setOf("self"))),
                readingOrder = listOf(
                    Link(href = Href("chap1.html")!!, mediaType = MediaType.HTML)
                ),
                resources = listOf(Link(href = Href("withtype")!!, mediaType = MediaType.HTML))
            ),
            Manifest.fromJSON(
                JSONObject(
                    """{
                "metadata": {"title": "Title"},
                "links": [
                    {"href": "manifest.json", "rel": "self"}
                ],
                "readingOrder": [
                    {"href": "chap1.html", "type": "text/html"}
                ],
                "resources": [
                    {"href": "withtype", "type": "text/html"},
                    {"href": "withouttype"}
                ]
            }"""
                )
            )
        )
    }

    @Test
    fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject(
                """{
                "metadata": {"title": {"und": "Title"}, "readingProgression": "auto"},
                "links": [],
                "readingOrder": []
            }"""
            ),
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
            JSONObject(
                """{
                "@context": ["https://readium.org/webpub-manifest/context.jsonld"],
                "metadata": {"title": {"und": "Title"}, "readingProgression": "auto"},
                "links": [
                    {"href": "manifest.json", "rel": ["self"], "templated": false}
                ],
                "readingOrder": [
                    {"href": "chap1.html", "type": "text/html", "templated": false}
                ],
                "resources": [
                    {"href": "image.png", "type": "image/png", "templated": false}
                ],
                "toc": [
                    {"href": "cover.html", "templated": false},
                    {"href": "chap1.html", "templated": false}
                ],
                "sub": {
                    "metadata": {},
                    "links": [
                        {"href": "sublink", "templated": false}
                    ]
                }
            }"""
            ),
            Manifest(
                context = listOf("https://readium.org/webpub-manifest/context.jsonld"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = Href("manifest.json")!!, rels = setOf("self"))),
                readingOrder = listOf(
                    Link(href = Href("chap1.html")!!, mediaType = MediaType.HTML)
                ),
                resources = listOf(Link(href = Href("image.png")!!, mediaType = MediaType.PNG)),
                tableOfContents = listOf(
                    Link(href = Href("cover.html")!!),
                    Link(href = Href("chap1.html")!!)
                ),
                subcollections = mapOf(
                    "sub" to listOf(
                        PublicationCollection(links = listOf(Link(href = Href("sublink")!!)))
                    )
                )
            ).toJSON()
        )
    }

    @Test fun `get a {Locator} from a minimal {Link}`() {
        val manifest = Manifest(
            metadata = Metadata(localizedTitle = LocalizedString()),
            readingOrder = listOf(
                Link(href = Href("href")!!, mediaType = MediaType.HTML, title = "Resource")
            )
        )
        Assert.assertEquals(
            Locator(
                href = Url("href")!!,
                mediaType = MediaType.HTML,
                title = "Resource",
                locations = Locator.Locations(progression = 0.0)
            ),
            manifest.locatorFromLink(Link(href = Href("href")!!))
        )
    }

    @Test fun `get a {Locator} from a link in the reading order, resources or links`() {
        val manifest = Manifest(
            metadata = Metadata(localizedTitle = LocalizedString()),
            readingOrder = listOf(Link(href = Href("href1")!!, mediaType = MediaType.HTML)),
            resources = listOf(Link(href = Href("href2")!!, mediaType = MediaType.HTML)),
            links = listOf(Link(href = Href("href3")!!, mediaType = MediaType.HTML))
        )
        Assert.assertEquals(
            Locator(
                href = Url("href1")!!,
                mediaType = MediaType.HTML,
                locations = Locator.Locations(progression = 0.0)
            ),
            manifest.locatorFromLink(Link(href = Href("href1")!!))
        )
        Assert.assertEquals(
            Locator(
                href = Url("href2")!!,
                mediaType = MediaType.HTML,
                locations = Locator.Locations(progression = 0.0)
            ),
            manifest.locatorFromLink(Link(href = Href("href2")!!))
        )
        Assert.assertEquals(
            Locator(
                href = Url("href3")!!,
                mediaType = MediaType.HTML,
                locations = Locator.Locations(progression = 0.0)
            ),
            manifest.locatorFromLink(Link(href = Href("href3")!!))
        )
    }

    @Test fun `get a {Locator} from a full {Link} with fragment`() {
        val manifest = Manifest(
            metadata = Metadata(localizedTitle = LocalizedString()),
            readingOrder = listOf(
                Link(href = Href("href")!!, mediaType = MediaType.HTML, title = "Resource")
            )
        )
        Assert.assertEquals(
            Locator(
                href = Url("href")!!,
                mediaType = MediaType.HTML,
                title = "Resource",
                locations = Locator.Locations(fragments = listOf("page=42"))
            ),
            manifest.locatorFromLink(
                Link(href = Href("href#page=42")!!, mediaType = MediaType.XML, title = "My link")
            )
        )
    }

    @Test fun `get a {Locator} falling back on the {Link} title`() {
        val manifest = Manifest(
            metadata = Metadata(localizedTitle = LocalizedString()),
            readingOrder = listOf(Link(href = Href("href")!!, mediaType = MediaType.HTML))
        )
        Assert.assertEquals(
            Locator(
                href = Url("href")!!,
                mediaType = MediaType.HTML,
                title = "My link",
                locations = Locator.Locations(fragments = listOf("page=42"))
            ),
            manifest.locatorFromLink(
                Link(href = Href("href#page=42")!!, mediaType = MediaType.XML, title = "My link")
            )
        )
    }

    @Test fun `get a {Locator} from a {Link} not found in the manifest`() {
        val manifest = Manifest(
            metadata = Metadata(localizedTitle = LocalizedString()),
            readingOrder = listOf(Link(href = Href("href")!!, mediaType = MediaType.HTML))
        )
        Assert.assertNull(manifest.locatorFromLink(Link(href = Href("notfound")!!)))
    }
}
