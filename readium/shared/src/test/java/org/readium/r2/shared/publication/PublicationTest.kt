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
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Publication.Profile
import org.readium.r2.shared.publication.services.DefaultLocatorService
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.EmptyContainer
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PublicationTest {

    private fun createPublication(
        conformsTo: Set<Profile> = emptySet(),
        title: String = "Title",
        language: String = "en",
        readingProgression: ReadingProgression? = null,
        links: List<Link> = listOf(),
        readingOrder: List<Link> = emptyList(),
        resources: List<Link> = emptyList(),
        servicesBuilder: Publication.ServicesBuilder = Publication.ServicesBuilder(),
    ) = Publication(
        manifest = Manifest(
            metadata = Metadata(
                conformsTo = conformsTo,
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

    @Test fun `get the default empty {positions}`() {
        assertEquals(emptyList<Locator>(), runBlocking { createPublication().positions() })
    }

    @Test fun `get the {positions} computed from the {PositionsService}`() {
        assertEquals(
            listOf(Locator(href = Url("locator")!!, mediaType = MediaType.HTML)),
            createPublication(
                servicesBuilder = Publication.ServicesBuilder(
                    positions = {
                        object : PositionsService {
                            override suspend fun positionsByReadingOrder(): List<List<Locator>> = listOf(
                                listOf(Locator(href = Url("locator")!!, mediaType = MediaType.HTML))
                            )
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
                    Locator(href = Url("res1")!!, mediaType = MediaType.HTML, title = "Loc A"),
                    Locator(href = Url("res1")!!, mediaType = MediaType.HTML, title = "Loc B")
                ),
                listOf(
                    Locator(href = Url("res2")!!, mediaType = MediaType.HTML, title = "Loc B")
                )
            ),
            createPublication(
                servicesBuilder = Publication.ServicesBuilder(
                    positions = {
                        object : PositionsService {
                            override suspend fun positionsByReadingOrder(): List<List<Locator>> = listOf(
                                listOf(
                                    Locator(
                                        href = Url("res1")!!,
                                        mediaType = MediaType.HTML,
                                        title = "Loc A"
                                    ),
                                    Locator(
                                        href = Url("res1")!!,
                                        mediaType = MediaType.HTML,
                                        title = "Loc B"
                                    )
                                ),
                                listOf(
                                    Locator(
                                        href = Url("res2")!!,
                                        mediaType = MediaType.HTML,
                                        title = "Loc B"
                                    )
                                )
                            )
                        }
                    }
                )
            ).let { runBlocking { it.positionsByReadingOrder() } }
        )
    }

    @Test fun `get {baseUrl} computes the URL from the {self} link`() {
        val publication = createPublication(
            links = listOf(
                Link(href = Href("http://domain.com/path/manifest.json")!!, rels = setOf("self"))
            )
        )
        assertEquals(
            Url("http://domain.com/path/manifest.json")!!,
            publication.baseUrl
        )
    }

    @Test fun `get {baseUrl} when missing`() {
        assertNull(createPublication().baseUrl)
    }

    @Test fun `conforms to the given profile`() {
        // An empty reading order doesn't conform to anything.
        assertFalse(
            createPublication(readingOrder = emptyList(), conformsTo = setOf(Profile.EPUB))
                .conformsTo(Profile.EPUB)
        )

        assertTrue(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.mp3")!!, mediaType = MediaType.MP3),
                    Link(href = Href("c2.aac")!!, mediaType = MediaType.AAC)
                )
            ).conformsTo(Profile.AUDIOBOOK)
        )
        assertTrue(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.jpg")!!, mediaType = MediaType.JPEG),
                    Link(href = Href("c2.png")!!, mediaType = MediaType.PNG)
                )
            ).conformsTo(Profile.DIVINA)
        )
        assertTrue(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.pdf")!!, mediaType = MediaType.PDF),
                    Link(href = Href("c2.pdf")!!, mediaType = MediaType.PDF)
                )
            ).conformsTo(Profile.PDF)
        )

        // Mixed media types disable implicit conformance.
        assertFalse(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.mp3")!!, mediaType = MediaType.MP3),
                    Link(href = Href("c2.jpg")!!, mediaType = MediaType.JPEG)
                )
            ).conformsTo(Profile.AUDIOBOOK)
        )
        assertFalse(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.mp3")!!, mediaType = MediaType.MP3),
                    Link(href = Href("c2.jpg")!!, mediaType = MediaType.JPEG)
                )
            ).conformsTo(Profile.DIVINA)
        )

        // XHTML could be EPUB or a Web Publication, so we require an explicit EPUB profile.
        assertTrue(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.xhtml")!!, mediaType = MediaType.XHTML),
                    Link(href = Href("c2.xhtml")!!, mediaType = MediaType.XHTML)
                ),
                conformsTo = setOf(Profile.EPUB)
            ).conformsTo(Profile.EPUB)
        )
        assertTrue(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.html")!!, mediaType = MediaType.HTML),
                    Link(href = Href("c2.html")!!, mediaType = MediaType.HTML)
                ),
                conformsTo = setOf(Profile.EPUB)
            ).conformsTo(Profile.EPUB)
        )
        assertFalse(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.xhtml")!!, mediaType = MediaType.XHTML),
                    Link(href = Href("c2.xhtml")!!, mediaType = MediaType.XHTML)
                )
            ).conformsTo(Profile.EPUB)
        )
        assertFalse(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.html")!!, mediaType = MediaType.HTML),
                    Link(href = Href("c2.html")!!, mediaType = MediaType.HTML)
                )
            ).conformsTo(Profile.EPUB)
        )
        assertFalse(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.pdf")!!, mediaType = MediaType.PDF),
                    Link(href = Href("c2.pdf")!!, mediaType = MediaType.PDF)
                ),
                conformsTo = setOf(Profile.EPUB)
            ).conformsTo(Profile.EPUB)
        )

        // Implicit conformance always take precedence over explicit profiles.
        assertTrue(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.mp3")!!, mediaType = MediaType.MP3),
                    Link(href = Href("c2.aac")!!, mediaType = MediaType.AAC)
                ),
                conformsTo = setOf(Profile.DIVINA)
            ).conformsTo(Profile.AUDIOBOOK)
        )
        assertFalse(
            createPublication(
                readingOrder = listOf(
                    Link(href = Href("c1.mp3")!!, mediaType = MediaType.MP3),
                    Link(href = Href("c2.aac")!!, mediaType = MediaType.AAC)
                ),
                conformsTo = setOf(Profile.DIVINA)
            ).conformsTo(Profile.DIVINA)
        )

        // Unknown profile
        val profile = Profile("http://extension")
        assertTrue(
            createPublication(
                readingOrder = listOf(Link(href = Href("file")!!)),
                conformsTo = setOf(profile)
            ).conformsTo(profile)
        )
    }

    @Test fun `find the first {Link} with the given {rel}`() {
        val link1 = Link(href = Href("found")!!, rels = setOf("rel1"))
        val link2 = Link(href = Href("found")!!, rels = setOf("rel2"))
        val link3 = Link(href = Href("found")!!, rels = setOf("rel3"))
        val publication = createPublication(
            links = listOf(Link(href = Href("other")!!), link1),
            readingOrder = listOf(Link(href = Href("other")!!), link2),
            resources = listOf(Link(href = Href("other")!!), link3)
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
                Link(href = Href("l1")!!),
                Link(href = Href("l2")!!, rels = setOf("rel1"))
            ),
            readingOrder = listOf(
                Link(href = Href("l3")!!),
                Link(href = Href("l4")!!, rels = setOf("rel1"))
            ),
            resources = listOf(
                Link(
                    href = Href("l5")!!,
                    alternates = listOf(
                        Link(href = Href("alternate")!!, rels = setOf("rel1"))
                    )
                ),
                Link(href = Href("l6")!!, rels = setOf("rel1"))
            )
        )

        assertEquals(
            listOf(
                Link(href = Href("l4")!!, rels = setOf("rel1")),
                Link(href = Href("l6")!!, rels = setOf("rel1")),
                Link(href = Href("l2")!!, rels = setOf("rel1"))
            ),
            publication.linksWithRel("rel1")
        )
    }

    @Test fun `find all the links with the given {rel} when not found`() {
        assertTrue(createPublication().linksWithRel("foobar").isEmpty())
    }

    @Test fun `find the first {Link} with the given {href}`() {
        val link1 = Link(href = Href("href1")!!)
        val link2 = Link(href = Href("href2")!!)
        val link3 = Link(href = Href("href3")!!)
        val link4 = Link(href = Href("href4")!!)
        val link5 = Link(href = Href("href5")!!)
        val publication = createPublication(
            links = listOf(Link(href = Href("other")!!), link1),
            readingOrder = listOf(
                Link(
                    href = Href("other")!!,
                    alternates = listOf(
                        Link(
                            href = Href("alt1")!!,
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
                    href = Href("other")!!,
                    children = listOf(
                        Link(
                            href = Href("alt1")!!,
                            children = listOf(
                                link4
                            )
                        )
                    )
                ),
                link5
            )
        )

        assertEquals(link1, publication.linkWithHref(Url("href1")!!))
        assertEquals(link2, publication.linkWithHref(Url("href2")!!))
        assertEquals(link3, publication.linkWithHref(Url("href3")!!))
        assertEquals(link4, publication.linkWithHref(Url("href4")!!))
        assertEquals(link5, publication.linkWithHref(Url("href5")!!))
    }

    @Test fun `find the first {Link} with the given {href} without query parameters`() {
        val link = Link(href = Href("http://example.com/index.html")!!)
        val publication = createPublication(
            readingOrder = listOf(Link(href = Href("other")!!), link)
        )

        assertEquals(
            link,
            publication.linkWithHref(Url("http://example.com/index.html?title=titre&action=edit")!!)
        )
    }

    @Test fun `find the first {Link} with the given {href} without anchor`() {
        val link = Link(href = Href("http://example.com/index.html")!!)
        val publication = createPublication(
            readingOrder = listOf(Link(href = Href("other")!!), link)
        )

        assertEquals(link, publication.linkWithHref(Url("http://example.com/index.html#sec1")!!))
    }

    @Test fun `find the first {Link} with the given {href} when missing`() {
        assertNull(createPublication().linkWithHref(Url("foobar")!!))
    }

    @Test fun `find the first resource {Link} with the given {href}`() {
        val link1 = Link(href = Href("href1")!!)
        val link2 = Link(href = Href("href2")!!)
        val link3 = Link(href = Href("href3")!!)
        val publication = createPublication(
            links = listOf(Link(href = Href("other")!!), link1),
            readingOrder = listOf(Link(href = Href("other")!!), link2),
            resources = listOf(Link(href = Href("other")!!), link3)
        )

        assertEquals(link1, publication.linkWithHref(Url("href1")!!))
        assertEquals(link2, publication.linkWithHref(Url("href2")!!))
        assertEquals(link3, publication.linkWithHref(Url("href3")!!))
    }

    @Test fun `find the first resource {Link} with the given {href} when missing`() {
        assertNull(createPublication().linkWithHref(Url("foobar")!!))
    }
}

@RunWith(RobolectricTestRunner::class)
class ServicesBuilderTest {

    open class FooService : Publication.Service
    class FooServiceA : FooService()
    class FooServiceB : FooService()
    class FooServiceC(val wrapped: FooService?) : FooService()

    open class BarService : Publication.Service
    class BarServiceA : BarService()

    private val context = Publication.Service.Context(
        manifest = Manifest(metadata = Metadata(localizedTitle = LocalizedString())),
        container = EmptyContainer(),
        services = ListPublicationServicesHolder()
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
        assertTrue(services.find<FooServiceC>()?.wrapped is FooServiceB)
        assertNotNull(services.find<BarServiceA>())
    }

    private inline fun <reified T> List<Publication.Service>.find(): T? =
        firstOrNull { it is T } as? T
}
