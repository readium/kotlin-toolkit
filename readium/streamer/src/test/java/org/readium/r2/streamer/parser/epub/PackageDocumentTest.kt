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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.contains
import org.readium.r2.shared.publication.epub.layout
import org.readium.r2.shared.publication.presentation.*
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.xml.XmlParser
import org.robolectric.RobolectricTestRunner

fun parsePackageDocument(path: String): Manifest {
    val pub = PackageDocument::class.java.getResourceAsStream(path)
        ?.let { XmlParser().parse(it) }
        ?.let { PackageDocument.parse(it, Url("OEBPS/content.opf")!!) }
        ?.let { ManifestAdapter(it) }
        ?.adapt()
    checkNotNull(pub)
    return pub.manifest
}

const val PARSE_PUB_TIMEOUT = 1000L // milliseconds

@RunWith(RobolectricTestRunner::class)
class ReadingProgressionTest {
    @Test
    fun `No page progression direction is mapped to default`() {
        assertThat(parsePackageDocument("package/progression-none.opf").metadata.readingProgression)
            .isEqualTo(null)
    }

    @Test
    fun `Default page progression direction is rightly parsed`() {
        assertThat(
            parsePackageDocument("package/progression-default.opf").metadata.readingProgression
        )
            .isEqualTo(null)
    }

    @Test
    fun `Ltr page progression direction is rightly parsed`() {
        assertThat(parsePackageDocument("package/progression-ltr.opf").metadata.readingProgression)
            .isEqualTo(ReadingProgression.LTR)
    }

    @Test
    fun `Rtl page progression direction is rightly parsed`() {
        assertThat(parsePackageDocument("package/progression-rtl.opf").metadata.readingProgression)
            .isEqualTo(ReadingProgression.RTL)
    }
}

@RunWith(RobolectricTestRunner::class)
class LinkPropertyTest {
    private val propertiesPub = parsePackageDocument("package/links-properties.opf")

    @Test
    fun `contains is rightly filled`() {
        with(propertiesPub) {
            assertThat(readingOrder[0].properties.contains).containsExactlyInAnyOrder("mathml")
            assertThat(readingOrder[1].properties.contains).containsExactlyInAnyOrder(
                "remote-resources"
            )
            assertThat(readingOrder[2].properties.contains).containsExactlyInAnyOrder("js", "svg")
            assertThat(readingOrder[3].properties.contains).isEmpty()
            assertThat(readingOrder[4].properties.contains).isEmpty()
        }
    }

    @Test
    fun `rels is rightly filled`() {
        with(propertiesPub) {
            assertThat(resources[0].rels).containsExactly("cover")
            assertThat(readingOrder[0].rels).isEmpty()
            assertThat(readingOrder[1].rels).isEmpty()
            assertThat(readingOrder[2].rels).isEmpty()
            assertThat(readingOrder[3].rels).containsExactly("contents")
            assertThat(readingOrder[4].rels).isEmpty()
        }
    }

    @Test
    fun `presentation properties are parsed`() {
        with(propertiesPub) {
            assertThat(readingOrder[0].properties.layout).isEqualTo(EpubLayout.FIXED)
            assertThat(readingOrder[0].properties.overflow).isEqualTo(Presentation.Overflow.AUTO)
            assertThat(readingOrder[0].properties.orientation).isEqualTo(
                Presentation.Orientation.AUTO
            )
            assertThat(readingOrder[0].properties.page).isEqualTo(Presentation.Page.RIGHT)
            assertThat(readingOrder[0].properties.spread).isNull()

            assertThat(readingOrder[1].properties.layout).isEqualTo(EpubLayout.REFLOWABLE)
            assertThat(readingOrder[1].properties.overflow).isEqualTo(
                Presentation.Overflow.PAGINATED
            )
            assertThat(readingOrder[1].properties.orientation).isEqualTo(
                Presentation.Orientation.LANDSCAPE
            )
            assertThat(readingOrder[1].properties.page).isEqualTo(Presentation.Page.LEFT)
            assertThat(readingOrder[0].properties.spread).isNull()

            assertThat(readingOrder[2].properties.layout).isNull()
            assertThat(readingOrder[2].properties.overflow).isEqualTo(
                Presentation.Overflow.SCROLLED
            )
            assertThat(readingOrder[2].properties.orientation).isEqualTo(
                Presentation.Orientation.PORTRAIT
            )
            assertThat(readingOrder[2].properties.page).isEqualTo(Presentation.Page.CENTER)
            assertThat(readingOrder[2].properties.spread).isNull()

            assertThat(readingOrder[3].properties.layout).isNull()
            assertThat(readingOrder[3].properties.overflow).isEqualTo(
                Presentation.Overflow.SCROLLED
            )
            assertThat(readingOrder[3].properties.orientation).isNull()
            assertThat(readingOrder[3].properties.page).isNull()
            assertThat(readingOrder[3].properties.spread).isEqualTo(Presentation.Spread.AUTO)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class LinkTest {
    private val resourcesPub = parsePackageDocument("package/links.opf")

    @Test
    fun `readingOrder is rightly computed`() {
        assertThat(resourcesPub.readingOrder).containsExactly(
            Link(
                href = Href("titlepage.xhtml")!!,
                mediaType = MediaType.XHTML
            ),
            Link(
                href = Href("OEBPS/chapter01.xhtml")!!,
                mediaType = MediaType.XHTML
            )
        )
    }

    @Test
    fun `resources are rightly computed`() {
        assertThat(resourcesPub.resources).containsExactlyInAnyOrder(
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
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class LinkMiscTest {
    fun `Fallbacks are mapped to alternates`() {
        assertThat(parsePackageDocument("package/fallbacks.opf")).isEqualTo(
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
            )
        )
    }

    @Test(timeout = PARSE_PUB_TIMEOUT)
    fun `Fallback computing terminates even if there are crossed dependencies`() {
        parsePackageDocument("package/fallbacks-termination.opf")
    }
}

@RunWith(RobolectricTestRunner::class)
class GuideTest {
    private val guidePub = parsePackageDocument("package/guide-epub2.opf")

    @Test
    fun `Guide is rightly computed`() {
        assertThat(guidePub.subcollections).containsExactly(
            entry(
                "landmarks",
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
            )
        )
    }
}
