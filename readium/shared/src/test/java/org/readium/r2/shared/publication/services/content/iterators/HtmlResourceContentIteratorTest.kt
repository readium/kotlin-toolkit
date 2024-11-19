@file:OptIn(ExperimentalReadiumApi::class, ExperimentalCoroutinesApi::class)

package org.readium.r2.shared.publication.services.content.iterators

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.Content.Attribute
import org.readium.r2.shared.publication.services.content.Content.AttributeKey.Companion.ACCESSIBILITY_LABEL
import org.readium.r2.shared.publication.services.content.Content.AttributeKey.Companion.LANGUAGE
import org.readium.r2.shared.publication.services.content.Content.TextElement
import org.readium.r2.shared.publication.services.content.Content.TextElement.Segment
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.StringResource
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalReadiumApi::class)
@RunWith(RobolectricTestRunner::class)
class HtmlResourceContentIteratorTest {

    private val locator = Locator(href = Url("/dir/res.xhtml")!!, mediaType = MediaType.XHTML)

    private val html = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE html>
        <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="en">
            <head>
                <title>Section IV: FAIRY STORIES—MODERN FANTASTIC TALES</title>
                <link href="css/epub.css" type="text/css" rel="stylesheet" />
            </head>
            <body>
                 <section id="pgepubid00498">
                     <div class="center"><span epub:type="pagebreak" title="171" id="Page_171">171</span></div>
                     <h3>INTRODUCTORY</h3>
                     
                     <p>The difficulties of classification are very apparent here, and once more it must be noted that illustrative and practical purposes rather than logical ones are served by the arrangement adopted. The modern fanciful story is here placed next to the real folk story instead of after all the groups of folk products. The Hebrew stories at the beginning belong quite as well, perhaps even better, in Section V, while the stories at the end of Section VI shade off into the more modern types of short tales.</p>
                     <p><span>The child's natural literature.</span> The world has lost certain secrets as the price of an advancing civilization.</p>
                     <p>Without discussing the limits of the culture-epoch theory of human development as a complete guide in education, it is clear that the young child passes through a period when his mind looks out upon the world in a manner analogous to that of the folk as expressed in their literature.</p>
                </section>
            </body>
        </html>
        """

    private val elements: List<Content.Element> = listOf(
        TextElement(
            locator = locator(
                progression = 0.0,
                selector = "html > body > section > div.center",
                before = null,
                highlight = "171"
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        progression = 0.0,
                        selector = "html > body > section > div.center",
                        before = null,
                        highlight = "171"
                    ),
                    text = "171",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                )
            )
        ),
        TextElement(
            locator = locator(
                progression = 0.2,
                selector = "html > body > section > h3",
                before = "171",
                highlight = "INTRODUCTORY"
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        progression = 0.2,
                        selector = "html > body > section > h3",
                        before = "171",
                        highlight = "INTRODUCTORY"
                    ),
                    text = "INTRODUCTORY",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                )
            )
        ),
        TextElement(
            locator = locator(
                progression = 0.4,
                selector = "html > body > section > p:nth-child(3)",
                before = "171INTRODUCTORY",
                highlight = "The difficulties of classification are very apparent here, and once more it must be noted that illustrative and practical purposes rather than logical ones are served by the arrangement adopted. The modern fanciful story is here placed next to the real folk story instead of after all the groups of folk products. The Hebrew stories at the beginning belong quite as well, perhaps even better, in Section V, while the stories at the end of Section VI shade off into the more modern types of short tales."
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        progression = 0.4,
                        selector = "html > body > section > p:nth-child(3)",
                        before = "171INTRODUCTORY",
                        highlight = "The difficulties of classification are very apparent here, and once more it must be noted that illustrative and practical purposes rather than logical ones are served by the arrangement adopted. The modern fanciful story is here placed next to the real folk story instead of after all the groups of folk products. The Hebrew stories at the beginning belong quite as well, perhaps even better, in Section V, while the stories at the end of Section VI shade off into the more modern types of short tales."
                    ),
                    text = "The difficulties of classification are very apparent here, and once more it must be noted that illustrative and practical purposes rather than logical ones are served by the arrangement adopted. The modern fanciful story is here placed next to the real folk story instead of after all the groups of folk products. The Hebrew stories at the beginning belong quite as well, perhaps even better, in Section V, while the stories at the end of Section VI shade off into the more modern types of short tales.",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                )
            )
        ),
        TextElement(
            locator = locator(
                progression = 0.6,
                selector = "html > body > section > p:nth-child(4)",
                before = "ade off into the more modern types of short tales.",
                highlight = "The child's natural literature. The world has lost certain secrets as the price of an advancing civilization."
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        progression = 0.6,
                        selector = "html > body > section > p:nth-child(4)",
                        before = "ade off into the more modern types of short tales.",
                        highlight = "The child's natural literature. The world has lost certain secrets as the price of an advancing civilization."
                    ),
                    text = "The child's natural literature. The world has lost certain secrets as the price of an advancing civilization.",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                )
            )
        ),
        TextElement(
            locator = locator(
                progression = 0.8,
                selector = "html > body > section > p:nth-child(5)",
                before = "secrets as the price of an advancing civilization.",
                highlight = "Without discussing the limits of the culture-epoch theory of human development as a complete guide in education, it is clear that the young child passes through a period when his mind looks out upon the world in a manner analogous to that of the folk as expressed in their literature."
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        progression = 0.8,
                        selector = "html > body > section > p:nth-child(5)",
                        before = "secrets as the price of an advancing civilization.",
                        highlight = "Without discussing the limits of the culture-epoch theory of human development as a complete guide in education, it is clear that the young child passes through a period when his mind looks out upon the world in a manner analogous to that of the folk as expressed in their literature."
                    ),
                    text = "Without discussing the limits of the culture-epoch theory of human development as a complete guide in education, it is clear that the young child passes through a period when his mind looks out upon the world in a manner analogous to that of the folk as expressed in their literature.",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                )
            )
        )
    )

    private fun locator(
        progression: Double? = null,
        selector: String? = null,
        before: String? = null,
        highlight: String? = null,
        after: String? = null,
    ): Locator =
        locator.copy(
            locations = Locator.Locations(
                progression = progression,
                otherLocations = buildMap {
                    selector?.let { put("cssSelector", it) }
                }
            ),
            text = Locator.Text(before = before, highlight = highlight, after = after)
        )

    private fun iterator(
        html: String,
        startLocator: Locator = locator,
        totalProgressionRange: ClosedRange<Double>? = null,
    ): HtmlResourceContentIterator =
        HtmlResourceContentIterator(
            StringResource(html),
            totalProgressionRange = totalProgressionRange,
            startLocator
        )

    private suspend fun HtmlResourceContentIterator.elements(): List<Content.Element> =
        buildList {
            while (hasNext()) {
                add(next())
            }
        }

    @Test
    fun `cannot call previous() without first hasPrevious()`() = runTest {
        val iter = iterator(html)
        iter.hasNext()
        iter.next()
        iter.hasNext()
        iter.next()

        assertThrows(IllegalStateException::class.java) { iter.previous() }
        iter.hasPrevious()
        iter.previous()
    }

    @Test
    fun `cannot call next() without first hasNext()`() = runTest {
        val iter = iterator(html)
        assertThrows(IllegalStateException::class.java) { iter.next() }
        iter.hasNext()
        iter.next()
    }

    @Test
    fun `iterate from start to finish`() = runTest {
        assertEquals(elements, iterator(html).elements())
    }

    @Test
    fun `previous() is null from the beginning`() = runTest {
        val iter = iterator(html)
        assertFalse(iter.hasPrevious())
    }

    @Test
    fun `next() returns the first element from the beginning`() = runTest {
        val iter = iterator(html)
        assertTrue(iter.hasNext())
        assertEquals(elements[0], iter.next())
    }

    @Test
    fun `next() then previous() returns null`() = runTest {
        val iter = iterator(html)
        assertTrue(iter.hasNext())
        assertEquals(elements[0], iter.next())
        assertFalse(iter.hasPrevious())
    }

    @Test
    fun `next() twice then previous() returns the first element`() = runTest {
        val iter = iterator(html)
        assertTrue(iter.hasNext())
        assertEquals(elements[0], iter.next())
        assertTrue(iter.hasNext())
        assertEquals(elements[1], iter.next())
        assertTrue(iter.hasPrevious())
        assertEquals(elements[0], iter.previous())
    }

    @Test
    fun `calling hasPrevious() several times doesn't move the index`() = runTest {
        val iter = iterator(html)
        iter.hasNext()
        iter.next()
        iter.hasNext()
        iter.next()
        assertTrue(iter.hasPrevious())
        assertTrue(iter.hasPrevious())
        assertTrue(iter.hasPrevious())
        assertEquals(elements[0], iter.previous())
    }

    @Test
    fun `calling hasNext() several times doesn't move the index`() = runTest {
        val iter = iterator(html)
        assertTrue(iter.hasNext())
        assertTrue(iter.hasNext())
        assertTrue(iter.hasNext())
        assertEquals(elements[0], iter.next())
    }

    @Test
    fun `starting from a CSS selector`() = runTest {
        val iter = iterator(html, locator(selector = "html > body > section > p:nth-child(3)"))
        assertEquals(elements.subList(2, elements.size), iter.elements())
    }

    @Test
    fun `calling previous() when starting from a CSS selector`() = runTest {
        val iter = iterator(html, locator(selector = "html > body > section > p:nth-child(3)"))
        assertTrue(iter.hasPrevious())
        assertEquals(elements[1], iter.previous())
    }

    @Test
    fun `starting from a CSS selector to a block element containing an inline element`() = runTest {
        val nbspHtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="fr">
            <body>
                <p>Tout au loin sur la chaussée, aussi loin qu’on pouvait voir</p>
                <p>Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient <span>[...]</span> On buvait de la bière sucrée.</p>
            </body>
            </html>
            """

        val iter = iterator(nbspHtml, locator(selector = ":root > :nth-child(2) > :nth-child(2)"))
        assertTrue(iter.hasNext())
        assertEquals(
            TextElement(
                locator = locator(
                    progression = 0.5,
                    selector = "html > body > p:nth-child(2)",
                    before = "oin sur la chaussée, aussi loin qu’on pouvait voir",
                    highlight = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée."
                ),
                role = TextElement.Role.Body,
                segments = listOf(
                    Segment(
                        locator = locator(
                            progression = 0.5,
                            selector = "html > body > p:nth-child(2)",
                            before = "oin sur la chaussée, aussi loin qu’on pouvait voir",
                            highlight = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée."
                        ),
                        text = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée.",
                        attributes = listOf(Attribute(LANGUAGE, Language("fr")))
                    )
                )
            ),
            iter.next()
        )
    }

    @Test
    fun `starting from a CSS selector using the root selector`() = runTest {
        val nbspHtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="fr">
            <head></head>
            <body>
                <p>Tout au loin sur la chaussée, aussi loin qu’on pouvait voir</p>
                <p>Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient <span>[...]</span> On buvait de la bière sucrée.</p>
            </body>
            </html>
            """

        val iter = iterator(nbspHtml, locator(selector = ":root > :nth-child(2) > :nth-child(2)"))
        assertTrue(iter.hasNext())
        assertEquals(
            TextElement(
                locator = locator(
                    progression = 0.5,
                    selector = "html > body > p:nth-child(2)",
                    before = "oin sur la chaussée, aussi loin qu’on pouvait voir",
                    highlight = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée."
                ),
                role = TextElement.Role.Body,
                segments = listOf(
                    Segment(
                        locator = locator(
                            progression = 0.5,
                            selector = "html > body > p:nth-child(2)",
                            before = "oin sur la chaussée, aussi loin qu’on pouvait voir",
                            highlight = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée."
                        ),
                        text = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée.",
                        attributes = listOf(Attribute(LANGUAGE, Language("fr")))
                    )
                )
            ),
            iter.next()
        )
    }

    @Test
    fun `iterating over image elements`() = runTest {
        val html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
                <img src="image.png"/>
                <img src="../cover.jpg" alt="Accessibility description" />
            </body>
            </html>
            """

        assertEquals(
            listOf(
                Content.ImageElement(
                    locator = locator(
                        progression = 0.0,
                        selector = "html > body > img:nth-child(1)"
                    ),
                    embeddedLink = Link(href = Href("/dir/image.png")!!),
                    caption = null,
                    attributes = emptyList()
                ),
                Content.ImageElement(
                    locator = locator(
                        progression = 0.5,
                        selector = "html > body > img:nth-child(2)"
                    ),
                    embeddedLink = Link(href = Href("/cover.jpg")!!),
                    caption = null,
                    attributes = listOf(Attribute(ACCESSIBILITY_LABEL, "Accessibility description"))
                )
            ),
            iterator(html).elements()
        )
    }

    @Test
    fun `iterating over audio elements`() = runTest {
        val html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
                <audio src="audio.mp3" />
                <audio>
                    <source src="audio.mp3" type="audio/mpeg" />
                    <source src="audio.ogg" type="audio/ogg" />
                </audio>
            </body>
            </html>
            """

        assertEquals(
            listOf(
                Content.AudioElement(
                    locator = locator(
                        progression = 0.0,
                        selector = "html > body > audio:nth-child(1)"
                    ),
                    embeddedLink = Link(href = Href("/dir/audio.mp3")!!),
                    attributes = emptyList()
                ),
                Content.AudioElement(
                    locator = locator(
                        progression = 0.5,
                        selector = "html > body > audio:nth-child(2)"
                    ),
                    embeddedLink = Link(
                        href = Href("/dir/audio.mp3")!!,
                        mediaType = MediaType.MP3,
                        alternates = listOf(
                            Link(href = Href("/dir/audio.ogg")!!, mediaType = MediaType.OGG)
                        )
                    ),
                    attributes = emptyList()
                )
            ),
            iterator(html).elements()
        )
    }

    @Test
    fun `iterating over video elements`() = runTest {
        val html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
                <video src="video.mp4" />
                <video>
                    <source src="video.mp4" type="video/mp4" />
                    <source src="video.m4v" type="video/x-m4v" />
                </video>
            </body>
            </html>
            """

        assertEquals(
            listOf(
                Content.VideoElement(
                    locator = locator(
                        progression = 0.0,
                        selector = "html > body > video:nth-child(1)"
                    ),
                    embeddedLink = Link(href = Href("/dir/video.mp4")!!),
                    attributes = emptyList()
                ),
                Content.VideoElement(
                    locator = locator(
                        progression = 0.5,
                        selector = "html > body > video:nth-child(2)"
                    ),
                    embeddedLink = Link(
                        href = Href("/dir/video.mp4")!!,
                        mediaType = MediaType("video/mp4")!!,
                        alternates = listOf(
                            Link(
                                href = Href("/dir/video.m4v")!!,
                                mediaType = MediaType("video/x-m4v")!!
                            )
                        )
                    ),
                    attributes = emptyList()
                )
            ),
            iterator(html).elements()
        )
    }

    @Test
    fun `iterating over an element containing both a text node and child elements`() = runTest {
        val html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
                <ol class="decimal" id="c06-list-0001">
                    <li id="c06-li-0001">Let&#39;s start at the top&#8212;the <i>source of ideas</i>.
                        <aside><div class="top hr"><hr/></div>
                        <section class="feature1">
                            <p id="c06-para-0019"><i>While almost everyone today claims to be Agile, what I&#39;ve just described is very much a <i>waterfall</i> process.</i></p>
                        </section>
                        Trailing text
                    </li>
                </ol>
            </body>
            </html>
            """

        assertEquals(
            listOf(
                TextElement(
                    locator = locator(
                        progression = 0.0,
                        selector = "#c06-li-0001",
                        highlight = "Let's start at the top—the source of ideas."
                    ),
                    role = TextElement.Role.Body,
                    segments = listOf(
                        Segment(
                            locator = locator(
                                progression = 0.0,
                                selector = "#c06-li-0001",
                                highlight = "Let's start at the top—the source of ideas."
                            ),
                            text = "Let's start at the top—the source of ideas.",
                            attributes = emptyList()
                        )
                    ),
                    attributes = emptyList()
                ),
                TextElement(
                    locator = locator(
                        progression = 1 / 3.0,
                        selector = "#c06-para-0019",
                        before = " top—the source of ideas.\n                        ",
                        highlight = "While almost everyone today claims to be Agile, what I've just described is very much a waterfall process."
                    ),
                    role = TextElement.Role.Body,
                    segments = listOf(
                        Segment(
                            locator = locator(
                                progression = 1 / 3.0,
                                selector = "#c06-para-0019",
                                before = " top—the source of ideas.\n                        ",
                                highlight = "While almost everyone today claims to be Agile, what I've just described is very much a waterfall process."
                            ),
                            text = "While almost everyone today claims to be Agile, what I've just described is very much a waterfall process.",
                            attributes = emptyList()
                        )
                    ),
                    attributes = emptyList()
                ),
                TextElement(
                    locator = locator(
                        progression = 2 / 3.0,
                        selector = "html > body > ol.decimal > li > aside",
                        before = "e just described is very much a waterfall process.\n                        \n                        ",
                        highlight = "Trailing text"
                    ),
                    role = TextElement.Role.Body,
                    segments = listOf(
                        Segment(
                            locator = locator(
                                progression = 2 / 3.0,
                                selector = "html > body > ol.decimal > li > aside",
                                before = "e just described is very much a waterfall process.\n                        ",
                                highlight = "Trailing text"
                            ),
                            text = "Trailing text",
                            attributes = emptyList()
                        )
                    ),
                    attributes = emptyList()
                )
            ),
            iterator(html).elements()
        )
    }

    @Test
    fun `iterating over text nodes located around a nested block element`() = runTest {
        val html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
                <div id="a">begin a <div id="b">in b</div> end a</div>
                <div id="c">in c</div>
            </body>
            </html>
            """

        assertEquals(
            listOf(
                TextElement(
                    locator = locator(
                        progression = 0.0,
                        selector = "#a",
                        highlight = "begin a"
                    ),
                    role = TextElement.Role.Body,
                    segments = listOf(
                        Segment(
                            locator = locator(
                                progression = 0.0,
                                selector = "#a",
                                highlight = "begin a"
                            ),
                            text = "begin a",
                            attributes = emptyList()
                        )
                    ),
                    attributes = emptyList()
                ),
                TextElement(
                    locator = locator(
                        progression = 0.25,
                        selector = "#b",
                        before = "begin a ",
                        highlight = "in b"
                    ),
                    role = TextElement.Role.Body,
                    segments = listOf(
                        Segment(
                            locator = locator(
                                progression = 0.25,
                                selector = "#b",
                                before = "begin a ",
                                highlight = "in b"
                            ),
                            text = "in b",
                            attributes = emptyList()
                        )
                    ),
                    attributes = emptyList()
                ),
                TextElement(
                    locator = locator(
                        progression = 0.5,
                        selector = "#a",
                        before = "begin a in b  ",
                        highlight = "end a"
                    ),
                    role = TextElement.Role.Body,
                    segments = listOf(
                        Segment(
                            locator = locator(
                                progression = 0.5,
                                selector = "#a",
                                before = "begin a in b ",
                                highlight = "end a"
                            ),
                            text = "end a",
                            attributes = emptyList()
                        )
                    ),
                    attributes = emptyList()
                ),
                TextElement(
                    locator = locator(
                        progression = 0.75,
                        selector = "#c",
                        before = "begin a in b end a",
                        highlight = "in c"
                    ),
                    role = TextElement.Role.Body,
                    segments = listOf(
                        Segment(
                            locator = locator(
                                progression = 0.75,
                                selector = "#c",
                                before = "begin a in b end a",
                                highlight = "in c"
                            ),
                            text = "in c",
                            attributes = emptyList()
                        )
                    ),
                    attributes = emptyList()
                )
            ),
            iterator(html).elements()
        )
    }
}
