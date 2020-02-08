/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.contains
import org.readium.r2.shared.publication.epub.layout
import org.readium.r2.shared.publication.presentation.*

fun parsePackageDocument(path: String): Publication {
    val pub = PackageDocument::class.java.getResourceAsStream(path)
        ?.let { XmlParser().parse(it) }
        ?.let { PackageDocument.parse(it, "OEBPS/content.opf") }
        ?.let { Epub(it) }
        ?.toPublication()
    checkNotNull(pub)
    return pub
}

const val PARSE_PUB_TIMEOUT = 1000L // milliseconds

class ReadingProgressionTest {
    @Test
    fun `No page progression direction is mapped to default`() {
        assertThat(parsePackageDocument("package/progression-none.opf").metadata.readingProgression)
            .isEqualTo(ReadingProgression.AUTO)
    }

    @Test
    fun `Default page progression direction is rightly parsed`() {
        assertThat(parsePackageDocument("package/progression-default.opf").metadata.readingProgression)
            .isEqualTo(ReadingProgression.AUTO)
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

class PackageMiscTest {
    @Test
    fun `Version is rightly parsed`() {
        assertThat(parsePackageDocument("package/version-epub2.opf").version).isEqualTo(2.0)
        assertThat(parsePackageDocument("package/version-epub3.opf").version).isEqualTo(3.0)
        assertThat(parsePackageDocument("package/version-default.opf").version).isEqualTo(1.2)
    }
}

class LinkPropertyTest {
    private val propertiesPub = parsePackageDocument("package/links-properties.opf")

    @Test
    fun `contains is rightly filled`() {
        with(propertiesPub) {
            assertThat(readingOrder[0].properties.contains).containsExactlyInAnyOrder("mathml")
            assertThat(readingOrder[1].properties.contains).containsExactlyInAnyOrder("remote-resources")
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
            assertThat(readingOrder[0].properties.orientation).isEqualTo(Presentation.Orientation.AUTO)
            assertThat(readingOrder[0].properties.page).isEqualTo(Presentation.Page.RIGHT)
            assertThat(readingOrder[0].properties.spread).isNull()

            assertThat(readingOrder[1].properties.layout).isEqualTo(EpubLayout.REFLOWABLE)
            assertThat(readingOrder[1].properties.overflow).isEqualTo(Presentation.Overflow.PAGINATED)
            assertThat(readingOrder[1].properties.orientation).isEqualTo(Presentation.Orientation.LANDSCAPE)
            assertThat(readingOrder[1].properties.page).isEqualTo(Presentation.Page.LEFT)
            assertThat(readingOrder[0].properties.spread).isNull()

            assertThat(readingOrder[2].properties.layout).isNull()
            assertThat(readingOrder[2].properties.overflow).isEqualTo(Presentation.Overflow.SCROLLED)
            assertThat(readingOrder[2].properties.orientation).isEqualTo(Presentation.Orientation.PORTRAIT)
            assertThat(readingOrder[2].properties.page).isEqualTo(Presentation.Page.CENTER)
            assertThat(readingOrder[2].properties.spread).isNull()

            assertThat(readingOrder[3].properties.layout).isNull()
            assertThat(readingOrder[3].properties.overflow).isEqualTo(Presentation.Overflow.SCROLLED)
            assertThat(readingOrder[3].properties.orientation).isNull()
            assertThat(readingOrder[3].properties.page).isNull()
            assertThat(readingOrder[3].properties.spread).isEqualTo(Presentation.Spread.AUTO)
        }
    }
}

class LinkTest {
    private val resourcesPub = parsePackageDocument("package/links.opf")

    @Test
    fun `readingOrder is rightly computed`() {
        assertThat(resourcesPub.readingOrder).containsExactly(
            Link(
                href = "/titlepage.xhtml",
                title = "titlepage",
                type = "application/xhtml+xml"
            ),
            Link(
                href = "/OEBPS/chapter01.xhtml",
                title = "chapter01",
                type = "application/xhtml+xml"
            )
        )
    }

    @Test
    fun `resources are rightly computed`() {
        assertThat(resourcesPub.resources).containsExactlyInAnyOrder(
            Link(
                href = "/OEBPS/fonts/MinionPro.otf",
                title = "font0",
                type = "application/vnd.ms-opentype"
            ),
            Link(
                href = "/OEBPS/nav.xhtml",
                title = "nav",
                type = "application/xhtml+xml",
                rels = listOf("contents")
            ),
            Link(
                href = "/style.css",
                title = "css",
                type = "text/css"
            ),
            Link(
                href = "/OEBPS/chapter01.smil",
                title = "chapter01_smil",
                type = "application/smil+xml"
            ),
            Link(
                href = "/OEBPS/chapter02.smil",
                title = "chapter02_smil",
                type = "application/smil+xml",
                duration = 1949.0
            ),
            Link(
                href = "/OEBPS/images/alice01a.png",
                title = "img01a",
                type = "image/png",
                rels = listOf("cover")
            ),
            Link(
                href = "/OEBPS/images/alice02a.gif",
                title = "img02a",
                type = "image/gif"
            ),
            Link(
                href = "/OEBPS/chapter02.xhtml",
                title = "chapter02",
                type = "application/xhtml+xml"
            ),
            Link(
                href = "/OEBPS/nomediatype.txt",
                title = "nomediatype"
            )
        )
    }
}

class LinkMiscTest {
    fun `Fallbacks are mapped to alternates`() {
        assertThat(parsePackageDocument("package/fallbacks.opf")).isEqualTo(
            Link(
                href = "/OEBPS/chap1_docbook.xml",
                title = "item1",
                type = "application/docbook+xml",
                alternates = listOf(
                    Link(
                        href = "/OEBPS/chap1.xml",
                        title = "fall1",
                        type = "application/z3998-auth+xml",
                        alternates = listOf(
                            Link(
                                href = "/OEBPS/chap1.xhtml",
                                title = "fall2",
                                type = "application/xhtml+xml"
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