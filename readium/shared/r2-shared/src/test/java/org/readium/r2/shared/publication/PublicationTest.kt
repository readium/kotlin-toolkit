/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.instanceOf
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.fetcher.EmptyFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.services.DefaultLocatorService
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import java.net.URL
import kotlin.reflect.KClass

class PublicationTest {

    private fun createPublication(
        title: String = "Title",
        language: String = "en",
        readingProgression: ReadingProgression = ReadingProgression.AUTO,
        links: List<Link> = listOf(),
        readingOrder: List<Link> = emptyList(),
        resources: List<Link> = emptyList(),
        servicesBuilder: Publication.ServicesBuilder = Publication.ServicesBuilder()
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
        servicesBuilder = servicesBuilder
    )

    @Test fun `get the type computed from the manifest content`() {
        val fixtures = Fixtures("format")
        fun parseAt(path: String): Publication =
            Publication(manifest = Manifest.fromJSON(JSONObject(fixtures.fileAt(path).readText()))!!)

        assertEquals(Publication.TYPE.AUDIO, parseAt("audiobook.json").type)
        assertEquals(Publication.TYPE.DiViNa, parseAt("divina.json").type)
        assertEquals(Publication.TYPE.WEBPUB, parseAt("webpub.json").type)
        assertEquals(Publication.TYPE.WEBPUB, parseAt("opds2-publication.json").type)
    }

    @Test fun `get the default empty {positions}`() {
        assertEquals(emptyList<Locator>(), runBlocking { createPublication().positions() })
    }

    @Test fun `get the {positions} computed from the {PositionsService}`() {
        assertEquals(
            listOf(Locator(href = "locator", type = "")),
            createPublication(
                servicesBuilder = Publication.ServicesBuilder(
                    positions = {
                        object: PositionsService {
                            override suspend fun positionsByReadingOrder(): List<List<Locator>> = listOf(listOf(Locator(href = "locator", type = "")))
                        }
                    }
                )
            ).let { runBlocking { it.positions() } }
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
                servicesBuilder = Publication.ServicesBuilder(
                    positions = {
                        object: PositionsService {
                            override suspend fun positionsByReadingOrder(): List<List<Locator>> = listOf(
                                listOf(
                                    Locator(href="res1", type = "text/html", title = "Loc A"),
                                    Locator(href="res1", type = "text/html", title = "Loc B")
                                ),
                                listOf(Locator(href="res2", type = "text/html", title = "Loc B"))
                            )
                        }
                    }
                )
            ).let { runBlocking { it.positionsByReadingOrder() }  }
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

    @Test fun `get {baseUrl} when it's a root`() {
        val publication = createPublication(
            links = listOf(Link(href = "http://domain.com/manifest.json", rels = setOf("self")))
        )
        assertEquals(
            URL("http://domain.com/"),
            publication.baseUrl
        )
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

    @Test fun `find all the links with the given {rel}`() {
        val publication = createPublication(
            links = listOf(
                Link(href = "l1"),
                Link(href = "l2", rels = setOf("rel1"))
            ),
            readingOrder = listOf(
                Link(href = "l3"),
                Link(href = "l4", rels = setOf("rel1"))
            ),
            resources = listOf(
                Link(href = "l5", alternates = listOf(
                    Link(href = "alternate", rels = setOf("rel1"))
                )),
                Link(href = "l6", rels = setOf("rel1"))
            )
        )

        assertEquals(
            listOf(
                Link(href = "l4", rels = setOf("rel1")),
                Link(href = "l6", rels = setOf("rel1")),
                Link(href = "l2", rels = setOf("rel1"))
            ),
            publication.linksWithRel("rel1")
        )
    }

    @Test fun `find all the links with the given {rel} when not found`() {
        assertTrue(createPublication().linksWithRel("foobar").isEmpty())
    }

    @Test fun `find the first {Link} with the given {href}`() {
        val link1 = Link(href = "href1")
        val link2 = Link(href = "href2")
        val link3 = Link(href = "href3")
        val link4 = Link(href = "href4")
        val link5 = Link(href = "href5")
        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(
                Link(
                    href = "other",
                    alternates = listOf(
                        Link(
                            href = "alt1",
                            alternates = listOf(
                                link2
                            )
                        )
                    )
                ),
                link3
            ),
            resources = listOf(
                Link(
                    href = "other",
                    children = listOf(
                        Link(
                            href = "alt1",
                            children = listOf(
                                link4
                            )
                        )
                    )
                ),
                link5
            )
        )

        assertEquals(link1, publication.linkWithHref("href1"))
        assertEquals(link2, publication.linkWithHref("href2"))
        assertEquals(link3, publication.linkWithHref("href3"))
        assertEquals(link4, publication.linkWithHref("href4"))
        assertEquals(link5, publication.linkWithHref("href5"))
    }

    @Test fun `find the first {Link} with the given {href} without query parameters`() {
        val link = Link(href = "http://example.com/index.html")
        val publication = createPublication(
            readingOrder = listOf(Link(href = "other"), link)
        )

        assertEquals(link, publication.linkWithHref("http://example.com/index.html?title=titre&action=edit"))
    }

    @Test fun `find the first {Link} with the given {href} without anchor`() {
        val link = Link(href = "http://example.com/index.html")
        val publication = createPublication(
            readingOrder = listOf(Link(href = "other"), link)
        )

        assertEquals(link, publication.linkWithHref("http://example.com/index.html#sec1"))
    }

    @Test fun `find the first {Link} with the given {href} when missing`() {
        assertNull(createPublication().linkWithHref("foobar"))
    }

    @Test fun `get method passes on href parameters to services`() {
        val service = object: Publication.Service {
            override fun get(link: Link): Resource? {
                assertFalse(link.templated)
                assertEquals("param1=a&param2=b", link.href.substringAfter("?"))
                return StringResource(link,"test passed")
            }
        }

        val link = Link(href = "link?param1=a&param2=b")
        val publication = createPublication(
            resources = listOf(link),
            servicesBuilder = Publication.ServicesBuilder(
                positions = { service }
            )
        )
        assertEquals("test passed", runBlocking { publication.get(link).readAsString().getOrNull() })
    }

    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
    @Test fun `find the first resource {Link} with the given {href} when missing`() {
        assertNull(createPublication().resourceWithHref("foobar"))
    }

    @Suppress("DEPRECATION")
    @Test fun `find the cover {Link}`() {
        val coverLink = Link(href = "cover", rels = setOf("cover"))
        val publication = createPublication(
            links = listOf(Link(href = "other"), coverLink),
            readingOrder = listOf(Link(href = "other")),
            resources = listOf(Link(href = "other"))
        )

        assertEquals(coverLink, publication.coverLink)
    }

    @Suppress("DEPRECATION")
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

    open class FooService: Publication.Service
    class FooServiceA: FooService()
    class FooServiceB: FooService()
    class FooServiceC(val wrapped: FooService?): FooService()

    open class BarService: Publication.Service
    class BarServiceA: BarService()

    private val context = Publication.Service.Context(
        Manifest(metadata = Metadata(localizedTitle = LocalizedString())),
        EmptyFetcher()
    )

    @Test
    fun testBuild() {
        val services = Publication.ServicesBuilder(cover = null)
            .apply {
                set(FooService::class) { FooServiceA() }
                set(BarService::class) { BarServiceA() }
            }
            .build(context)

        assertNotNull(services.find<FooServiceA>())
        assertNotNull(services.find<BarServiceA>())
    }

    @Test
    fun testBuildEmpty() {
        val builder = Publication.ServicesBuilder(cover = null)
        val services = builder.build(context)
        assertEquals(1, services.size)
        assertNotNull(services.find<DefaultLocatorService>())
    }

    @Test
    fun testSetOverwrite() {
        val services = Publication.ServicesBuilder(cover = null)
            .apply {
                set(FooService::class) { FooServiceA() }
                set(FooService::class) { FooServiceB() }
            }
            .build(context)

        assertNotNull(services.find<FooServiceB>())
        assertNull(services.find<FooServiceA>())
    }

    @Test
    fun testRemoveExisting() {
        val services = Publication.ServicesBuilder(cover = null)
            .apply {
                set(FooService::class) { FooServiceA() }
                set(BarService::class) { BarServiceA() }
                remove(FooService::class)
            }
            .build(context)

        assertNotNull(services.find<BarServiceA>())
        assertNull(services.find<FooServiceA>())
    }

    @Test
    fun testRemoveUnknown() {
        val services = Publication.ServicesBuilder(cover = null)
            .apply {
                set(FooService::class) { FooServiceA() }
                remove(BarService::class)
            }
            .build(context)

        assertNotNull(services.find<FooServiceA>())
        assertNull(services.find<BarService>())
    }

    @Test
    fun testDecorate() {
        val services = Publication.ServicesBuilder(cover = null)
            .apply {
                set(FooService::class) { FooServiceB() }
                set(BarService::class) { BarServiceA() }
                decorate(FooService::class) { oldFactory ->
                    { context ->
                        FooServiceC(oldFactory?.let { it(context) as? FooService })
                    }
                }
            }
            .build(context)

        assertNotNull(services.find<FooServiceC>())
        assertThat(services.find<FooServiceC>()?.wrapped,  instanceOf(FooServiceB::class.java))
        assertNotNull(services.find<BarServiceA>())
    }

    private inline fun <reified T> List<Publication.Service>.find(): T? =
        firstOrNull { it is T } as? T

}