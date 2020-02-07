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
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import java.io.Serializable
import java.net.URL

class PublicationTest {

    private fun createPublication(
        title: String = "Title",
        language: String = "EN",
        readingProgression: ReadingProgression = ReadingProgression.AUTO,
        links: List<Link> = listOf(),
        readingOrder: List<Link> = emptyList(),
        resources: List<Link> = emptyList(),
        positionListFactory: PositionListFactory = { emptyList() }
    ) = Publication(
        metadata = Metadata(
            localizedTitle = LocalizedString(title),
            languages = listOf(language),
            readingProgression = readingProgression
        ),
        links = links,
        readingOrder = readingOrder,
        resources = resources,
        positionListFactory = positionListFactory as Serializable
    )

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Publication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Publication.fromJSON(JSONObject("""{
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
            Publication(
                context = listOf("https://readium.org/webpub-manifest/context.jsonld"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/image.png", type = "image/png")),
                tableOfContents = listOf(Link(href = "/cover.html"), Link(href = "/chap1.html")),
                otherCollections = listOf(PublicationCollection(role = "sub", links = listOf(Link(href = "/sublink"))))
            ),
            Publication.fromJSON(JSONObject("""{
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
            Publication(
                context = listOf("context1", "context2"),
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Publication.fromJSON(JSONObject("""{
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
        assertNull(Publication.fromJSON(JSONObject("""{
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
            Publication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Publication.fromJSON(JSONObject("""{
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
            Publication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(
                    Link(href = "/manifest.json", rels = listOf("self")),
                    Link(href = "/withrel", rels = listOf("withrel"))
                ),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Publication.fromJSON(JSONObject("""{
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
            Publication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html"))
            ),
            Publication.fromJSON(JSONObject("""{
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
            Publication(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                links = listOf(Link(href = "/manifest.json", rels = listOf("self"))),
                readingOrder = listOf(Link(href = "/chap1.html", type = "text/html")),
                resources = listOf(Link(href = "/withtype", type = "text/html"))
            ),
            Publication.fromJSON(JSONObject("""{
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
            Publication(
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
            Publication(
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

    @Test fun `get the default empty {positionList}`() {
        assertEquals(emptyList<Locator>(), createPublication().positionList)
    }

    @Test fun `get the {positionList} computed from the {positionListFactory}`() {
        assertEquals(
            listOf(Locator(href = "locator", type = "")),
            createPublication(
                positionListFactory = { listOf(Locator(href="locator", type = "")) }
            ).positionList
        )
    }

    @Test fun `get the {positionListByResource} computed from the {positionListFactory}`() {
        assertEquals(
            mapOf(
                "res1" to listOf(
                    Locator(href="res1", type = "text/html", title = "Loc A"),
                    Locator(href="res1", type = "text/html", title = "Loc B")
                ),
                "res2" to listOf(
                    Locator(href="res2", type = "text/html", title = "Loc B")
                )
            ),
            createPublication(
                positionListFactory = { listOf(
                    Locator(href="res1", type = "text/html", title = "Loc A"),
                    Locator(href="res2", type = "text/html", title = "Loc B"),
                    Locator(href="res1", type = "text/html", title = "Loc B")
                ) }
            ).positionListByResource
        )
    }

    @Test fun `get {contentLayout} for the default language`() {
        assertEquals(
            ContentLayout.RTL,
            createPublication(language = "AR").contentLayout
        )
    }

    @Test fun `get {contentLayout} for the given language`() {
        val publication = createPublication()

        assertEquals(ContentLayout.RTL, publication.contentLayoutForLanguage("AR"))
        assertEquals(ContentLayout.LTR, publication.contentLayoutForLanguage("EN"))
    }

    @Test fun `get {contentLayout} fallbacks on the {readingProgression}`() {
        assertEquals(
            ContentLayout.RTL,
            createPublication(
                language = "en",
                readingProgression = ReadingProgression.RTL
            ).contentLayoutForLanguage("EN")
        )
    }

    @Test fun `set {self} link`() {
        val publication = createPublication()
        publication.setSelfLink("http://manifest.json")

        assertEquals(
            "http://manifest.json",
            publication.linkWithRel("self")?.href
        )
    }

    @Test fun `set {self} link replaces existing {self} link`() {
        val publication = createPublication(
            links = listOf(Link(href = "previous", rels = listOf("self")))
        )
        publication.setSelfLink("http://manifest.json")

        assertEquals(
            "http://manifest.json",
            publication.linkWithRel("self")?.href
        )
    }

    @Test fun `get {baseUrl} computes the URL from the {self} link`() {
        val publication = createPublication(
            links = listOf(Link(href = "http://domain.com/path/manifest.json", rels = listOf("self")))
        )
        assertEquals(
            URL("http://domain.com/path/"),
            publication.baseUrl
        )
    }

    @Test fun `get {baseUrl} when missing`() {
        assertNull(createPublication().baseUrl)
    }

    @Test fun `find the first {Link} matching the given predicate`() {
        val link1 = Link(href = "href1", title = "link1")
        val link2 = Link(href = "href2", title = "link2")
        val link3 = Link(href = "href3", title = "link3", alternates = listOf(link2))
        val link4 = Link(href = "href4", title = "link4")
        val link5 = Link(href = "href5", title = "link5")
        val link6 = Link(href = "href6", title = "link6", alternates = listOf(link4, link5))

        val publication = createPublication(
                links = listOf(Link(href = "other"), link1),
                readingOrder = listOf(Link(href = "other"), link2, link6),
                resources = listOf(Link(href = "other"), link3)
        )

        fun predicate(title: String) =
                { link: Link -> link.title == title}

        assertEquals(link1, publication.link(predicate("link1")))
        assertEquals(link2, publication.link(predicate("link2")))
        assertEquals(link3, publication.link(predicate("link3")))
        assertEquals(link4, publication.link(predicate("link4")))
        assertEquals(link5, publication.link(predicate("link5")))
        assertEquals(link6, publication.link(predicate("link6")))
    }

    @Test fun `find the first {Link} with the given predicate when not found`() {
        assertNull(createPublication().link { it.href == "foobar" })
    }

    @Test fun `find the first {Link} with the given {rel}`() {
        val link1 = Link(href = "found", rels = listOf("rel1"))
        val link2 = Link(href = "found", rels = listOf("rel2"))
        val link3 = Link(href = "found", rels = listOf("rel3"))
        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(Link(href = "other"), link2),
            resources = listOf(Link(href = "other"), link3)
        )

        assertEquals(link1, publication.linkWithRel("rel1"))
        assertEquals(link2, publication.linkWithRel("rel2"))
        assertEquals(link3, publication.linkWithRel("rel3"))
    }

    @Test fun `find the first {Link} with the given {rel} when missing`() {
        assertNull(createPublication().linkWithRel("foobar"))
    }

    @Test fun `find the first {Link} with the given {href}`() {
        val link1 = Link(href = "href1")
        val link2 = Link(href = "href2")
        val link3 = Link(href = "href3")
        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(Link(href = "other"), link2),
            resources = listOf(Link(href = "other"), link3)
        )

        assertEquals(link1, publication.linkWithHref("href1"))
        assertEquals(link2, publication.linkWithHref("href2"))
        assertEquals(link3, publication.linkWithHref("href3"))
    }

    @Test fun `find the first {Link} with the given {href} when missing`() {
        assertNull(createPublication().linkWithHref("foobar"))
    }

    @Test fun `find the first resource {Link} with the given {href}`() {
        val link1 = Link(href = "href1")
        val link2 = Link(href = "href2")
        val link3 = Link(href = "href3")
        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(Link(href = "other"), link2),
            resources = listOf(Link(href = "other"), link3)
        )

        assertNull(publication.resourceWithHref("href1"))
        assertEquals(link2, publication.resourceWithHref("href2"))
        assertEquals(link3, publication.resourceWithHref("href3"))
    }

    @Test fun `find the first resource {Link} with the given {href} when missing`() {
        assertNull(createPublication().resourceWithHref("foobar"))
    }

    @Test fun `find the cover {Link}`() {
        val coverLink = Link(href = "cover", rels = listOf("cover"))
        val publication = createPublication(
            links = listOf(Link(href = "other"), coverLink),
            readingOrder = listOf(Link(href = "other")),
            resources = listOf(Link(href = "other"))
        )

        assertEquals(coverLink, publication.coverLink)
    }

    @Test fun `find the cover {Link} when missing`() {
        val publication = createPublication(
            links = listOf(Link(href = "other")),
            readingOrder = listOf(Link(href = "other")),
            resources = listOf(Link(href = "other"))
        )

        assertNull(publication.coverLink)
    }

}
