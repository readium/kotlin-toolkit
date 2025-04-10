/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.css

import androidx.core.net.toUri
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.readium.navigator.web.css.Color as CssColor
import org.readium.navigator.web.css.TextAlign as CssTextAlign
import org.readium.navigator.web.preferences.ReflowableWebSettings
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

internal data class ReadiumCss(
    val layout: Layout = Layout(language = null, Layout.Stylesheets.Default, ReadingProgression.LTR),
    val rsProperties: RsProperties = RsProperties(),
    val userProperties: UserProperties = UserProperties(),
    val fontFamilyDeclarations: List<FontFamilyDeclaration> = emptyList(),
    val googleFonts: List<FontFamily> = emptyList(),
    val assetsBaseHref: AbsoluteUrl,
    val readiumCssAssets: RelativeUrl,
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
        val readiumCSS = assetsBaseHref.resolve(readiumCssAssets)
        if (layout.stylesheets.folder == null) {
            readiumCSS
        } else {
            readiumCSS.resolve(layout.stylesheets.folder)
        }
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

                val uri = "https://fonts.googleapis.com/css".toUri()
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

@OptIn(ExperimentalReadiumApi::class)
internal fun ReadiumCss.update(settings: ReflowableWebSettings, useReadiumCssFontSize: Boolean): ReadiumCss {
    fun resolveFontStack(fontFamily: String): List<String> = buildList {
        add(fontFamily)

        val alternates = fontFamilyDeclarations
            .firstOrNull { it.fontFamily == fontFamily }
            ?.alternates
            ?: emptyList()

        addAll(alternates.flatMap(::resolveFontStack))
    }

    fun FontFamily.toCss(): List<String> =
        resolveFontStack(name)

    fun Color.toCss(): CssColor =
        CssColor.Int(int)

    return with(settings) {
        copy(
            layout = Layout.from(settings),
            rsProperties = rsProperties.copy(
                pageGutter = Length.Px((rsProperties.pageGutter?.value ?: 20.0) * horizontalMargins)
            ),
            userProperties = userProperties.copy(
                view = when (scroll) {
                    false -> View.PAGED
                    true -> View.SCROLL
                },
                colCount = columnCount,
                appearance = when (theme) {
                    Theme.LIGHT -> null
                    Theme.DARK -> Appearance.NIGHT
                    Theme.SEPIA -> Appearance.SEPIA
                },
                darkenImages = imageFilter == ImageFilter.DARKEN,
                invertImages = imageFilter == ImageFilter.INVERT,
                textColor = textColor?.toCss(),
                backgroundColor = backgroundColor?.toCss(),
                fontOverride = (fontFamily != null || textNormalization),
                fontFamily = fontFamily?.toCss(),
                fontSize = if (useReadiumCssFontSize) {
                    Length.Percent(fontSize)
                } else {
                    null
                },
                textAlign = when (textAlign) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                    null -> null
                },
                lineHeight = lineHeight?.let { Either(it) },
                paraSpacing = paragraphSpacing?.let { Length.Rem(it) },
                paraIndent = paragraphIndent?.let { Length.Rem(it) },
                wordSpacing = wordSpacing?.let { Length.Rem(it) },
                letterSpacing = letterSpacing?.let { Length.Rem(it / 2) },
                bodyHyphens = hyphens?.let { if (it) Hyphens.AUTO else Hyphens.NONE },
                ligatures = ligatures?.let { if (it) Ligatures.COMMON else Ligatures.NONE },
                a11yNormalize = textNormalization,
                overrides = mapOf(
                    "font-weight" to
                        if (fontWeight != null) {
                            (FontWeight.NORMAL.value * fontWeight).toInt().coerceIn(1, 1000).toString()
                        } else {
                            ""
                        }
                )
            )
        )
    }
}
