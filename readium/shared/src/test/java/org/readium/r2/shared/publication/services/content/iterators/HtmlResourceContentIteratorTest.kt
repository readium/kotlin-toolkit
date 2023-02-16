@file:OptIn(ExperimentalReadiumApi::class, ExperimentalCoroutinesApi::class)

package org.readium.r2.shared.publication.services.content.iterators

import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.Content.Attribute
import org.readium.r2.shared.publication.services.content.Content.AttributeKey.Companion.LANGUAGE
import org.readium.r2.shared.publication.services.content.Content.TextElement
import org.readium.r2.shared.publication.services.content.Content.TextElement.Segment
import org.readium.r2.shared.util.Language
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HtmlResourceContentIteratorTest {

    private val link = Link(href = "res.xhtml", type = "application/xhtml+xml")
    private val locator = Locator(href = "res.xhtml", type = "application/xhtml+xml")

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
                selector = "#pgepubid00498 > div.center",
                highlight = "171"
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        selector = "#pgepubid00498 > div.center",
                        before = "",
                        highlight = "171"
                    ),
                    text = "171",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                )
            )
        ),
        TextElement(
            locator = locator(
                selector = "#pgepubid00498 > h3",
                highlight = "INTRODUCTORY"
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        selector = "#pgepubid00498 > h3",
                        before = "171",
                        highlight = "INTRODUCTORY"
                    ),
                    text = "INTRODUCTORY",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                ),
            )
        ),
        TextElement(
            locator = locator(
                selector = "#pgepubid00498 > p:nth-child(3)",
                highlight = "The difficulties of classification are very apparent here, and once more it must be noted that illustrative and practical purposes rather than logical ones are served by the arrangement adopted. The modern fanciful story is here placed next to the real folk story instead of after all the groups of folk products. The Hebrew stories at the beginning belong quite as well, perhaps even better, in Section V, while the stories at the end of Section VI shade off into the more modern types of short tales."
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        selector = "#pgepubid00498 > p:nth-child(3)",
                        before = "171INTRODUCTORY",
                        highlight = "The difficulties of classification are very apparent here, and once more it must be noted that illustrative and practical purposes rather than logical ones are served by the arrangement adopted. The modern fanciful story is here placed next to the real folk story instead of after all the groups of folk products. The Hebrew stories at the beginning belong quite as well, perhaps even better, in Section V, while the stories at the end of Section VI shade off into the more modern types of short tales."
                    ),
                    text = "The difficulties of classification are very apparent here, and once more it must be noted that illustrative and practical purposes rather than logical ones are served by the arrangement adopted. The modern fanciful story is here placed next to the real folk story instead of after all the groups of folk products. The Hebrew stories at the beginning belong quite as well, perhaps even better, in Section V, while the stories at the end of Section VI shade off into the more modern types of short tales.",
                    attributes = listOf(Attribute(LANGUAGE, Language("en"))),
                ),
            )
        ),
        TextElement(
            locator = locator(
                selector = "#pgepubid00498 > p:nth-child(4)",
                highlight = "The child's natural literature. The world has lost certain secrets as the price of an advancing civilization."
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        selector = "#pgepubid00498 > p:nth-child(4)",
                        before = "ade off into the more modern types of short tales.",
                        highlight = "The child's natural literature. The world has lost certain secrets as the price of an advancing civilization."
                    ),
                    text = "The child's natural literature. The world has lost certain secrets as the price of an advancing civilization.",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                ),
            )
        ),
        TextElement(
            locator = locator(
                selector = "#pgepubid00498 > p:nth-child(5)",
                highlight = "Without discussing the limits of the culture-epoch theory of human development as a complete guide in education, it is clear that the young child passes through a period when his mind looks out upon the world in a manner analogous to that of the folk as expressed in their literature."
            ),
            role = TextElement.Role.Body,
            segments = listOf(
                Segment(
                    locator = locator(
                        selector = "#pgepubid00498 > p:nth-child(5)",
                        before = "secrets as the price of an advancing civilization.",
                        highlight = "Without discussing the limits of the culture-epoch theory of human development as a complete guide in education, it is clear that the young child passes through a period when his mind looks out upon the world in a manner analogous to that of the folk as expressed in their literature."
                    ),
                    text = "Without discussing the limits of the culture-epoch theory of human development as a complete guide in education, it is clear that the young child passes through a period when his mind looks out upon the world in a manner analogous to that of the folk as expressed in their literature.",
                    attributes = listOf(Attribute(LANGUAGE, Language("en")))
                ),
            )
        )
    )

    private fun locator(
        selector: String? = null,
        before: String? = null,
        highlight: String? = null,
        after: String? = null
    ): Locator =
        locator.copy(
            locations = Locator.Locations(otherLocations = buildMap {
                selector?.let { put("cssSelector", it) }
            }),
            text = Locator.Text(before = before, highlight = highlight, after = after)
        )

    private fun iterator(html: String, startLocator: Locator = locator): HtmlResourceContentIterator =
        HtmlResourceContentIterator(StringResource(link, html), startLocator)

    @Test
    fun `cannot call previous() without first hasPrevious()`() = runTest {
        val iter = iterator(html)
        iter.hasNext(); iter.next()

        assertFailsWith(IllegalStateException::class) { iter.previous() }
        iter.hasPrevious()
        iter.previous()
    }

    @Test
    fun `cannot call next() without first hasNext()`() = runTest {
        val iter = iterator(html)
        assertFailsWith(IllegalStateException::class) { iter.next() }
        iter.hasNext()
        iter.next()
    }

    @Test
    fun `iterate from start to finish`() = runTest {
        val iter = iterator(html)
        val res = mutableListOf<Content.Element>()
        while (iter.hasNext()) {
            res.add(iter.next())
        }
        assertContentEquals(elements, res)
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
    fun `next() then previous() returns the first element`() = runTest {
        val iter = iterator(html)
        assertTrue(iter.hasNext())
        assertEquals(elements[0], iter.next())
        assertTrue(iter.hasPrevious())
        assertEquals(elements[0], iter.previous())
    }

    @Test
    fun `calling hasPrevious() several times doesn't move the index`() = runTest {
        val iter = iterator(html)
        iter.hasNext(); iter.next()
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
        val iter = iterator(html, locator(selector = "#pgepubid00498 > p:nth-child(3)"))
        val res = mutableListOf<Content.Element>()
        while (iter.hasNext()) {
            res.add(iter.next())
        }
        assertContentEquals(elements.subList(2, elements.size), res)
    }

    @Test
    fun `calling previous() when starting from a CSS selector`() = runTest {
        val iter = iterator(html, locator(selector = "#pgepubid00498 > p:nth-child(3)"))
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
                    selector = "html > body > p:nth-child(2)",
                    highlight = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée."
                ),
                role = TextElement.Role.Body,
                segments = listOf(
                    Segment(
                        locator = locator(
                            selector = "html > body > p:nth-child(2)",
                            before = "oin sur la chaussée, aussi loin qu’on pouvait voir",
                            highlight = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée.",
                        ),
                        text = "Lui, notre colonel, savait peut-être pourquoi ces deux gens-là tiraient [...] On buvait de la bière sucrée.",
                        attributes = listOf(Attribute(LANGUAGE, Language("fr")))
                    )
                )
            ),
            iter.next()
        )
    }
}
