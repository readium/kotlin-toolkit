/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import android.net.Uri
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
internal data class ReadiumCss(
    val layout: Layout = Layout(language = null, Layout.Stylesheets.Default, ReadingProgression.LTR),
    val rsProperties: RsProperties = RsProperties(),
    val userProperties: UserProperties = UserProperties(),
    val fontFamilyDeclarations: List<FontFamilyDeclaration> = emptyList(),
    val googleFonts: List<FontFamily> = emptyList(),
    val assetsBaseHref: Url,
) {

    /**
     * Injects Readium CSS in the given [html] resource.
     *
     * https://github.com/readium/readium-css/blob/develop/docs/CSS06-stylesheets_order.md
     */
    // FIXME: Replace existing attributes instead of adding new ones
    @Throws
    internal fun injectHtml(html: String): String {
        val document = Jsoup.parse(html)
        val content = StringBuilder(html)
        injectStyles(content)
        injectCssProperties(content)
        injectDir(content)
        injectLang(content, document)
        return content.toString()
    }

    /**
     * Inject the Readium CSS stylesheets and font face declarations.
     */
    private fun injectStyles(content: StringBuilder) {
        val hasStyles = content.hasStyles()

        val headBeforeIndex = content.indexForOpeningTag("head")
        content.insert(
            headBeforeIndex,
            "\n" + buildList {
                addAll(fontsInjectableLinks)

                add(stylesheetLink(beforeCss))

                // Fix Readium CSS issue with the positioning of <audio> elements.
                // https://github.com/readium/readium-css/issues/94
                // https://github.com/readium/r2-navigator-kotlin/issues/193
                add("<style>audio[controls] { width: revert; height: revert; }</style>")

                // Fix broken pagination when a book contains `overflow-x: hidden`.
                // https://github.com/readium/kotlin-toolkit/issues/292
                // Inspired by https://github.com/readium/readium-css/issues/119#issuecomment-1302348238
                add(
                    """
                <style>
                    :root[style], :root { overflow: visible !important; }
                    :root[style] > body, :root > body { overflow: visible !important; }
                </style>
                    """.trimMargin()
                )

                if (!hasStyles) {
                    add(stylesheetLink(defaultCss))
                }
            }.joinToString("\n") + "\n"
        )

        val endHeadIndex = content.indexForClosingTag("head")
        content.insert(
            endHeadIndex,
            "\n" + buildList {
                add(stylesheetLink(afterCss))

                if (fontsInjectableCss.isNotEmpty()) {
                    add(
                        """
                    <style type="text/css">
                    ${fontsInjectableCss.joinToString("\n")}
                    </style>
                        """.trimIndent()
                    )
                }
            }.joinToString("\n") + "\n"
        )
    }

    private val stylesheetsFolder by lazy {
        assetsBaseHref.resolve(
            Url("readium/readium-css/${layout.stylesheets.folder?.plus("/") ?: ""}")!!
        )
    }

    private val beforeCss by lazy {
        stylesheetsFolder.resolve(Url("ReadiumCSS-before.css")!!)
    }

    private val afterCss by lazy {
        stylesheetsFolder.resolve(Url("ReadiumCSS-after.css")!!)
    }

    private val defaultCss by lazy {
        stylesheetsFolder.resolve(Url("ReadiumCSS-default.css")!!)
    }

    /**
     * Generates the font face declarations from the declared font families.
     */
    private val fontsInjectableCss: List<String> by lazy {
        buildList {
            addAll(
                fontFamilyDeclarations
                    .flatMap { it.fontFaces }
                    .map { it.toCss(::normalizeAssetUrl) }
            )

            if (googleFonts.isNotEmpty()) {
                val families = googleFonts.joinToString("|") { it.name }

                val uri = Uri.parse("https://fonts.googleapis.com/css")
                    .buildUpon()
                    .appendQueryParameter("family", families)
                    .build()
                    .toString()

                // @import needs to be at the top of the <style> declaration.
                add(0, "@import url('$uri');")
            }
        }
    }

    private val fontsInjectableLinks: List<String> by lazy {
        fontFamilyDeclarations
            .flatMap { it.fontFaces }
            .flatMap { it.links(::normalizeAssetUrl) }
    }

    private fun normalizeAssetUrl(url: Url): Url =
        assetsBaseHref.resolve(url)

    /**
     * Returns whether the [String] receiver has any CSS styles.
     *
     * https://github.com/readium/readium-css/blob/develop/docs/CSS06-stylesheets_order.md#append-if-there-is-no-authors-styles
     */
    private fun CharSequence.hasStyles(): Boolean {
        return indexOf("<link ", 0, true) != -1 ||
            indexOf(" style=", 0, true) != -1 ||
            Regex("<style.*?>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).containsMatchIn(
                this
            )
    }

    private fun stylesheetLink(href: Url): String =
        """<link rel="stylesheet" type="text/css" href="$href"/>"""

    /**
     * Inject the current Readium CSS properties inline in `html`.
     *
     * We inject them instead of using JavaScript to make sure they are taken into account during
     * the first layout pass.
     */
    private fun injectCssProperties(content: StringBuilder) {
        var css = rsProperties.toCss() + userProperties.toCss()
        if (css.isBlank()) {
            return
        }
        css = css.replace("\"", "&quot;")
        val index = content.indexForTagAttributes("html")
        content.insert(index, " style=\"$css\"")
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

        // Removes any dir attributes in html/body.
        content.replace(0, content.length, content.replace(dirRegex, "$1"))

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

        fun Element.hasLang(): Boolean =
            hasAttr("xml:lang") || hasAttr("lang")

        fun Element.lang(): String? =
            attr("xml:lang").takeIf { it.isNotEmpty() }
                ?: attr("lang").takeIf { it.isNotEmpty() }

        val html = document.selectFirst("html")
        if (html?.hasLang() == true) {
            return
        }

        val body = document.body()
        if (body.hasLang()) {
            content.insert(
                content.indexForTagAttributes("html"),
                " xml:lang=\"${body.lang() ?: language}\""
            )
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

private val dirRegex = Regex(
    """(<(?:html|body)[^\>]*)\s+dir=[\"']\w*[\"']""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
)
