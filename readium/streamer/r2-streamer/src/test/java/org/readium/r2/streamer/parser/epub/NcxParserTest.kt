package org.readium.r2.streamer.parser.epub

import org.assertj.core.api.Assertions
import org.junit.Test
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Link
import kotlin.test.assertNotNull

class NcxParserTest {
    private fun parseNavigationDocument(path: String) : Ncx {
        val res = NcxParser::class.java.getResourceAsStream(path)
        check(res != null)
        val document = XmlParser().parse(res)
        val ncx = NcxParser.parse(document, "OEBPS/ncx.ncx")
        assertNotNull(ncx)
        return ncx
    }

    private val ncxComplex = parseNavigationDocument("ncx/ncx-complex.ncx")
    private val ncxTitles = parseNavigationDocument("ncx/ncx-titles.ncx")
    private val ncxChildren = parseNavigationDocument("ncx/ncx-children.ncx")
    private val ncxEmpty = parseNavigationDocument("ncx/ncx-empty.ncx")

    @Test
    fun `Newlines are trimmed from title`() {
        Assertions.assertThat(ncxTitles.toc).contains(
                Link(title = "A link with new lines splitting the text", href = "/OEBPS/xhtml/chapter1.xhtml")
        )
    }

    @Test
    fun `Spaces are trimmed from title`() {
        Assertions.assertThat(ncxTitles.toc).contains(
                Link(title = "A link with ignorable spaces", href = "/OEBPS/xhtml/chapter2.xhtml")
        )
    }

    @Test
    fun `Entries with a zero-length title and no children are ignored`() {
        Assertions.assertThat(ncxTitles.toc).doesNotContain(
                Link(title = "", href = "/OEBPS/xhtml/chapter3.xhtml")
        )
    }

    @Test
    fun `Unlinked entries without children are ignored`() {
        Assertions.assertThat(ncxTitles.toc).doesNotContain(
                Link(title = "An unlinked element without children must be ignored", href = "#")
        )
    }

    @Test
    fun `Hierarchical items are allowed`() {
        Assertions.assertThat(ncxChildren.toc).containsExactly(
                Link(title = "Introduction", href = "/OEBPS/xhtml/introduction.xhtml"),
                Link(title = "Part I", href = "#", children = listOf(
                        Link(title = "Chapter 1", href = "/OEBPS/xhtml/part1/chapter1.xhtml"),
                        Link(title = "Chapter 2", href = "/OEBPS/xhtml/part1/chapter2.xhtml")
                )),
                Link(title = "Part II", href = "/OEBPS/xhtml/part2/chapter1.xhtml", children = listOf(
                        Link(title = "Chapter 1", href = "/OEBPS/xhtml/part2/chapter1.xhtml"),
                        Link(title = "Chapter 2", href = "/OEBPS/xhtml/part2/chapter2.xhtml")
                ))
        )
    }

    @Test
    fun `Empty Ncx is accepted`() {
        Assertions.assertThat(ncxEmpty.toc).isEmpty()
    }

    @Test
    fun `toc is rightly parsed`() {
        Assertions.assertThat(ncxComplex.toc).containsExactly(
                Link(title = "Chapter 1", href = "/OEBPS/xhtml/chapter1.xhtml"),
                Link(title = "Chapter 2", href = "/OEBPS/xhtml/chapter2.xhtml")
        )
    }

    @Test
    fun `page list is rightly parsed`() {
        Assertions.assertThat(ncxComplex.pageList).containsExactly(
                Link(title = "1", href = "/OEBPS/xhtml/chapter1.xhtml#page1"),
                Link(title = "2", href = "/OEBPS/xhtml/chapter1.xhtml#page2")
        )
    }
}