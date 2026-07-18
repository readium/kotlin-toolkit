/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Page
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.contains
import org.readium.r2.shared.publication.page
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.xml.XmlParser
import org.readium.r2.streamer.Fixtures
import org.readium.r2.streamer.assertContainsExactlyInAnyOrder

fun parsePackageDocument(path: String): Manifest {
    val pub = Fixtures("epub").read(path).toByteArray()
        .let { XmlParser().parse(it) }
        .let { PackageDocument.parse(it, Url("OEBPS/content.opf")!!) }
        ?.let { ManifestAdapter(it) }
        ?.adapt()
    checkNotNull(pub)
    return pub
}

class ReadingProgressionTest {
    @Test
    fun `No page progression direction is mapped to default`() {
        assertEquals(null, parsePackageDocument("package/progression-none.opf").metadata.readingProgression)
    }

    @Test
    fun `Default page progression direction is rightly parsed`() {
        assertEquals(null, parsePackageDocument("package/progression-default.opf").metadata.readingProgression)
    }

    @Test
    fun `Ltr page progression direction is rightly parsed`() {
        assertEquals(ReadingProgression.LTR, parsePackageDocument("package/progression-ltr.opf").metadata.readingProgression)
    }

    @Test
    fun `Rtl page progression direction is rightly parsed`() {
        assertEquals(ReadingProgression.RTL, parsePackageDocument("package/progression-rtl.opf").metadata.readingProgression)
    }
}

class LinkPropertyTest {
    private val propertiesPub = parsePackageDocument("package/links-properties.opf")

    @Test
    fun `contains is rightly filled`() {
        with(propertiesPub) {
            assertContainsExactlyInAnyOrder(
                listOf(
                    "mathml"
                ),
                readingOrder[0].properties.contains
            )
            assertContainsExactlyInAnyOrder(
                listOf(
                    "remote-resources"
                ),
                readingOrder[1].properties.contains
            )
            assertContainsExactlyInAnyOrder(
                listOf(
                    "js",
                    "svg"
                ),
                readingOrder[2].properties.contains
            )
            assertTrue(readingOrder[3].properties.contains.isEmpty())
            assertTrue(readingOrder[4].properties.contains.isEmpty())
        }
    }

    @Test
    fun `rels is rightly filled`() {
        with(propertiesPub) {
            assertEquals(setOf("cover"), resources[0].rels)
            assertTrue(readingOrder[0].rels.isEmpty())
            assertTrue(readingOrder[1].rels.isEmpty())
            assertTrue(readingOrder[2].rels.isEmpty())
            assertEquals(setOf("contents"), readingOrder[3].rels)
            assertTrue(readingOrder[4].rels.isEmpty())
        }
    }

    @Test
    fun `page properties are parsed`() {
        with(propertiesPub) {
            assertEquals(Page.RIGHT, readingOrder[0].properties.page)
            assertEquals(Page.LEFT, readingOrder[1].properties.page)

            assertEquals(Page.CENTER, readingOrder[2].properties.page)
            assertNull(readingOrder[3].properties.page)
        }
    }
}

class LinkTest {
    private val resourcesPub = parsePackageDocument("package/links.opf")

    @Test
    fun `readingOrder is rightly computed`() {
        assertEquals(
            listOf(
                Link(
                    href = Href("titlepage.xhtml")!!,
                    mediaType = MediaType.XHTML
                ),
                Link(
                    href = Href("OEBPS/chapter01.xhtml")!!,
                    mediaType = MediaType.XHTML
                )
            ),
            resourcesPub.readingOrder
        )
    }

    @Test
    fun `resources are rightly computed`() {
        assertContainsExactlyInAnyOrder(
            listOf(
                Link(
                    href = Href("OEBPS/fonts/MinionPro.otf")!!,
                    mediaType = MediaType("application/vnd.ms-opentype")!!
                ),
                Link(
                    href = Href("OEBPS/nav.xhtml")!!,
                    mediaType = MediaType.XHTML,
                    rels = setOf("contents")
                ),
                Link(
                    href = Href("style.css")!!,
                    mediaType = MediaType.CSS
                ),
                Link(
                    href = Href("OEBPS/chapter01.smil")!!,
                    mediaType = MediaType.SMIL
                ),
                Link(
                    href = Href("OEBPS/chapter02.smil")!!,
                    mediaType = MediaType.SMIL,
                    duration = 1949.0
                ),
                Link(
                    href = Href("OEBPS/images/alice01a.png")!!,
                    mediaType = MediaType.PNG,
                    rels = setOf("cover")
                ),
                Link(
                    href = Href("OEBPS/images/alice02a.gif")!!,
                    mediaType = MediaType.GIF
                ),
                Link(
                    href = Href("OEBPS/chapter02.xhtml")!!,
                    mediaType = MediaType.XHTML
                ),
                Link(
                    href = Href("OEBPS/nomediatype.txt")!!
                )
            ),
            resourcesPub.resources
        )
    }
}

class LinkMiscTest {
    fun `Fallbacks are mapped to alternates`() {
        assertEquals<Any?>(
            Link(
                href = Href("OEBPS/chap1_docbook.xml")!!,
                mediaType = MediaType("application/docbook+xml")!!,
                alternates = listOf(
                    Link(
                        href = Href("OEBPS/chap1.xml")!!,
                        mediaType = MediaType("application/z3998-auth+xml")!!,
                        alternates = listOf(
                            Link(
                                href = Href("OEBPS/chap1.xhtml")!!,
                                mediaType = MediaType.XHTML
                            )
                        )
                    )
                )
            ),
            parsePackageDocument("package/fallbacks.opf")
        )
    }

    @Test
    fun `Fallback computing terminates even if there are crossed dependencies`() =
        runTest(timeout = 1.seconds) {
            parsePackageDocument("package/fallbacks-termination.opf")
        }
}

class GuideTest {
    private val guidePub = parsePackageDocument("package/guide-epub2.opf")

    @Test
    fun `Guide is rightly computed`() {
        assertEquals(
            mapOf(
                "landmarks" to
                    listOf(
                        PublicationCollection(
                            links = listOf(
                                Link(
                                    href = Href("OEBPS/toc.html")!!,
                                    title = "Table of Contents",
                                    rels = setOf("http://idpf.org/epub/vocab/structure/#toc")
                                ),
                                Link(
                                    href = Href("OEBPS/toc.html#figures")!!,
                                    title = "List Of Illustrations",
                                    rels = setOf("http://idpf.org/epub/vocab/structure/#loi")
                                ),
                                Link(
                                    href = Href("OEBPS/beginpage.html")!!,
                                    title = "Introduction",
                                    rels = setOf("http://idpf.org/epub/vocab/structure/#bodymatter")
                                ),
                            )
                        )
                    )
            ),
            guidePub.subcollections
        )
    }
}
