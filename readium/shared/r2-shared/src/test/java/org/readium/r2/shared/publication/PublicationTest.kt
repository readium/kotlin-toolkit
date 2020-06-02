/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.fetcher.EmptyFetcher
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.publication.services.positionsServiceFactory
import java.net.URL

class PublicationTest {

    private fun createPublication(
        title: String = "Title",
        language: String = "en",
        readingProgression: ReadingProgression = ReadingProgression.AUTO,
        links: List<Link> = listOf(),
        readingOrder: List<Link> = emptyList(),
        resources: List<Link> = emptyList(),
        positionsServiceFactory: ((Publication.Service.Context) -> PositionsService?)? = null
    ) = Publication(
            manifest = Manifest(
                    metadata = Metadata(
                        localizedTitle = LocalizedString(title),
                        languages = listOf(language),
                        readingProgression = readingProgression
                    ),
                    links = links,
                    readingOrder = readingOrder,
                    resources = resources
                ),
            servicesBuilder = Publication.ServicesBuilder(positions = positionsServiceFactory)
    )

    @Test fun `get the default empty {positions}`() {
        assertEquals(emptyList<Locator>(), createPublication().positions)
    }

    @Test fun `get the {positions} computed from the {PositionsService}`() {
        assertEquals(
            listOf(Locator(href = "locator", type = "")),
            createPublication(
                positionsServiceFactory = { context ->
                    object: PositionsService {
                        override val positionsByReadingOrder: List<List<Locator>> = listOf(listOf(Locator(href = "locator", type = "")))
                    }
                }
            ).positions
        )
    }

    @Test fun `get the {positionsByReadingOrder} computed from the {PositionsService}`() {
        assertEquals(
            listOf(
                listOf(
                    Locator(href="res1", type = "text/html", title = "Loc A"),
                    Locator(href="res1", type = "text/html", title = "Loc B")
                ),
                listOf(
                    Locator(href="res2", type = "text/html", title = "Loc B")
                )
            ),
            createPublication(
                positionsServiceFactory = { context ->
                    object: PositionsService {
                        override val positionsByReadingOrder: List<List<Locator>> = listOf(
                            listOf(
                                Locator(href="res1", type = "text/html", title = "Loc A"),
                                Locator(href="res1", type = "text/html", title = "Loc B")
                            ),
                            listOf(Locator(href="res2", type = "text/html", title = "Loc B"))
                        )
                    }
                }
            ).positionsByReadingOrder
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
            links = listOf(Link(href = "previous", rels = setOf("self")))
        )
        publication.setSelfLink("http://manifest.json")

        assertEquals(
            "http://manifest.json",
            publication.linkWithRel("self")?.href
        )
    }

    @Test fun `get {baseUrl} computes the URL from the {self} link`() {
        val publication = createPublication(
            links = listOf(Link(href = "http://domain.com/path/manifest.json", rels = setOf("self")))
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
        val link1 = Link(href = "found", rels = setOf("rel1"))
        val link2 = Link(href = "found", rels = setOf("rel2"))
        val link3 = Link(href = "found", rels = setOf("rel3"))
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
        val coverLink = Link(href = "cover", rels = setOf("cover"))
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

class ServicesBuilderTest {

    open class FooService: Publication.Service {}
    class FooServiceA: FooService()
    class FooServiceB: FooService()
    class FooServiceC(val wrapped: FooService?): FooService()

    open class BarService: Publication.Service {}
    class BarServiceA: BarService() {}

    private val context = Publication.Service.Context(
        Manifest(metadata = Metadata(localizedTitle = LocalizedString())),
        EmptyFetcher()
    )

    @Test
    fun testBuild() {
        val builder = Publication.ServicesBuilder().apply {
            set(FooService::class.java) { FooServiceA() }
            set(BarService::class.java) { BarServiceA() }
        }

        val services = builder.build(context)

        assertEquals(2, services.size)
        assertThat(services[0], instanceOf(FooServiceA::class.java))
        assertThat(services[1], instanceOf(BarServiceA::class.java))
    }

    @Test
    fun testBuildEmpty() {
        val builder = Publication.ServicesBuilder()
        val services = builder.build(context)
        assertTrue(services.isEmpty())
    }

    @Test
    fun testSetOverwrite() {
        val builder = Publication.ServicesBuilder().apply {
            set(FooService::class.java) { FooServiceA() }
            set(FooService::class.java) { FooServiceB() }
        }

        val services = builder.build(context)

        assertEquals(1, services.size)
        assertThat(services[0], instanceOf(FooServiceB::class.java))
    }

    @Test
    fun testRemoveExisting() {
        val builder = Publication.ServicesBuilder()
        builder.set(FooService::class.java) { FooServiceA() }
        builder.set(BarService::class.java) { BarServiceA() }

        builder.remove(FooService::class.java)

        val services = builder.build(context)
        assertEquals(1, services.size)
        assertThat(services[0], instanceOf(BarServiceA::class.java))
    }

    @Test
    fun testRemoveUnknown() {
        val builder = Publication.ServicesBuilder()
        builder.set(FooService::class.java) { FooServiceA() }

        builder.remove(BarService::class.java)

        val services = builder.build(context)
        assertEquals(1, services.size)
        assertThat(services[0], instanceOf(FooServiceA::class.java))
    }

    @Test
    fun testWrap() {
        val builder = Publication.ServicesBuilder().apply {
            set(FooService::class.java) { FooServiceB() }
            set(BarService::class.java) { BarServiceA() }
        }

        builder.wrap(FooService::class.java) { oldFactory -> { context ->
            FooServiceC(oldFactory?.let { it(context)  as? FooService })
        }
        }

        val services = builder.build(context)
        assertEquals(2, services.size)
        assertThat(services[0], instanceOf(FooServiceC::class.java))
        assertThat((services[0] as? FooServiceC)?.wrapped,  instanceOf(FooServiceB::class.java))
        assertThat(services[1], instanceOf(BarServiceA::class.java))
    }
}
