/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.readium.r2.shared.ExperimentalReadiumApi

// FIXME: Custom Fonts
@ExperimentalReadiumApi
data class ReadiumCss(
    val layout: Layout,
    val rsProperties: RsProperties = RsProperties(),
    val userProperties: UserProperties = UserProperties(),
) {
    /**
     * Injects Readium CSS in the given [html] resource.
     *
     * https://github.com/readium/readium-css/blob/develop/docs/CSS06-stylesheets_order.md
     */
    // FIXME: Replace existing attributes instead of adding new ones
    @Throws
    fun injectHtml(html: String, baseHref: String = "/assets"): String {
        val document = Jsoup.parse(html)
        val content = StringBuilder(html)
        injectStyles(content, baseHref = baseHref)
        injectCssProperties(content)
        injectDir(content)
        injectLang(content, document)
        return content.toString()
    }

    /**
     * Inject the Readium CSS stylesheets and font face declarations.
     */
    private fun injectStyles(content: StringBuilder, baseHref: String) {
        val hasStyles = content.hasStyles()
        val stylesheetsFolder = baseHref + "/readium-css/" + (layout.stylesheets.folder?.plus("/") ?: "")

        val headBeforeIndex = content.indexForOpeningTag("head")
        content.insert(headBeforeIndex, "\n" + buildList {
            add(stylesheetLink(stylesheetsFolder + "ReadiumCSS-before.css"))

            // Fix Readium CSS issue with the positioning of <audio> elements.
            // https://github.com/readium/readium-css/issues/94
            // https://github.com/readium/r2-navigator-kotlin/issues/193
            add("<style>audio[controls] { width: revert; height: revert; }</style>")

            if (!hasStyles) {
                add(stylesheetLink(stylesheetsFolder + "ReadiumCSS-default.css"))
            }
        }.joinToString("\n") + "\n")

        val endHeadIndex = content.indexForClosingTag("head")
        content.insert(endHeadIndex, "\n" + buildList {
            add(stylesheetLink(stylesheetsFolder + "ReadiumCSS-after.css"))
            add(fontFace(fontFamily = "OpenDyslexic", href = "$baseHref/fonts/OpenDyslexic-Regular.otf"))
            add("<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>")
        }.joinToString("\n") + "\n")
    }

    /**
     * Returns whether the [String] receiver has any CSS styles.
     *
     * https://github.com/readium/readium-css/blob/develop/docs/CSS06-stylesheets_order.md#append-if-there-is-no-authors-styles
     */
    private fun CharSequence.hasStyles(): Boolean {
        return indexOf("<link ", 0, true) != -1
            || indexOf(" style=", 0, true) != -1
            || Regex("<style.*?>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).containsMatchIn(this)
    }

    private fun stylesheetLink(href: String): String =
        """
            <link rel="stylesheet" type="text/css" href="$href"/>
        """.trimIndent()

    private fun fontFace(fontFamily: String, href: String): String =
        """
            <style type="text/css">@font-face { font-family: "$fontFamily"; src: url("$href") format('truetype'); }</style>
        """.trimIndent()

    /**
     * Inject the current Readium CSS properties inline in `html`.
     *
     * We inject them instead of using JavaScript to make sure they are taken into account during
     * the first layout pass.
     */
    private fun injectCssProperties(content: StringBuilder) {
        val css = rsProperties.toCss() + userProperties.toCss()
        if (css.isBlank()) {
            return
        }
        val index = content.indexForTagAttributes("html")
        content.insert(index, " style=\"${css.replace("\"", "\\\"")}\"")
    }

    /**
     * Inject the `dir` attribute in `html` and `body`.
     *
     * https://github.com/readium/readium-css/blob/develop/docs/CSS16-internationalization.md#direction
     */
    private fun injectDir(content: StringBuilder) {
        val dir = when (layout.stylesheets.htmlDir) {
            Layout.HtmlDir.Unspecified -> null
            Layout.HtmlDir.Ltr -> "ltr"
            Layout.HtmlDir.Rtl -> "rtl"
        } ?: return

        val injectable = " dir=\"$dir\""
        content.insert(content.indexForTagAttributes("html"), injectable)
        content.insert(content.indexForTagAttributes("body"), injectable)
    }

    /**
     * Inject the `xml:lang` attribute in `html` and `body`.
     *
     * https://github.com/readium/readium-css/blob/develop/docs/CSS16-internationalization.md#language
     */
    private fun injectLang(content: StringBuilder, document: Document) {
        val language = layout.language?.code ?: return

        fun Element.lang(): String? =
            attr("xml:lang").takeIf { it.isNotEmpty() }
                ?: attr("lang").takeIf { it.isNotEmpty() }

        if (document.lang() != null) {
            return
        }

        val bodyLang = document.body().lang()
        if (bodyLang != null) {
            content.insert(content.indexForTagAttributes("html"), " xml:lang=\"$bodyLang\"")
        } else {
            val injectable = " xml:lang=\"$language\""
            content.insert(content.indexForTagAttributes("html"), injectable)
            content.insert(content.indexForTagAttributes("body"), injectable)
        }
    }

    private fun CharSequence.indexForOpeningTag(tag: String): Int =
        (
            Regex("""<$tag.*?>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(this, 0)
                ?: throw IllegalArgumentException("No <$tag> opening tag found in this resource")
        ).range.last + 1

    private fun CharSequence.indexForClosingTag(tag: String): Int =
        indexOf("</$tag>", 0, true)
            .takeIf { it != -1 }
            ?: throw IllegalArgumentException("No </head> closing tag found in this resource")

    private fun CharSequence.indexForTagAttributes(tag: String): Int =
        (
            indexOf("<$tag", 0, true)
                .takeIf { it != -1 }
                ?: throw IllegalArgumentException("No <$tag> opening tag found in this resource")
        ) + tag.length + 1
}