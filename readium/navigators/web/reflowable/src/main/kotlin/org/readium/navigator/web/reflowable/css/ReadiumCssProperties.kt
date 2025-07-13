/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable.css

import androidx.annotation.ColorInt
import java.text.NumberFormat
import java.util.*
import kotlin.collections.iterator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either

/**
 * Holds a set of Readium CSS properties applied together.
 */
internal interface ReadiumCssProperties : Cssable {
    fun toCssProperties(): Map<String, String?>

    override fun toCss(): String? {
        val props = toCssProperties()
            .filterValues { it != null }

        if (props.isEmpty()) {
            return ""
        }

        return props
            .map { (key, value) -> "$key: $value !important;" }
            .joinToString("\n") + "\n"
    }
}

/**
 * User settings properties.
 *
 * See https://readium.org/readium-css/docs/CSS19-api.html#user-settings
 *
 * @param view User view: paged or scrolled.
 * @param colCount The number of columns (column-count) the user wants displayed (one-page view
 * or two-page spread). To reset, change the value to auto.
 * @param darkenImages This will only apply in night mode to darken images and impact img.
 * @param invertImages This will only apply in night mode to invert images and impact img.
 * @param textColor The color for textual contents. It impacts all elements but headings and pre
 * in the DOM. To reset, remove the CSS variable.
 * @param backgroundColor The background-color for the whole screen. To reset, remove the CSS
 * variable.
 * @param linkColor The color for links.
 * @param visitedLinkColor The color for visited links.
 * @param fontOverride This flag is required to change the font-family user setting.
 * @param fontFamily The typeface (font-family) the user wants to read with. It impacts body, p,
 * li, div, dt, dd and phrasing elements which don’t have a lang or xml:lang attribute. To reset,
 * remove the required flag. Requires: fontOverride
 * @param fontSize Increasing and decreasing the root font-size. It will serve as a reference
 * for the cascade. To reset, remove the required flag.
 * @param fontWeight  Increasing and decreasing the text boldness. Requires: fontOverride
 * @param lineLength Increasing and decreasing the line length.
 * @param textAlign The alignment (text-align) the user prefers. It impacts body, li, and p
 * which are not children of blockquote and figcaption.
 * @param lineHeight Increasing and decreasing leading (line-height). It impacts body, p, li and
 * div. Recommended values: a range from 1 to 2. Increments are left to implementers’ judgment.
 * @param paraSpacing The vertical margins (margin-top and margin-bottom) for paragraphs.
 * Recommended values: a range from 0 to 2rem. Increments are left to implementers’
 * judgment.
 * @param paraIndent The text-indent for paragraphs. Recommended values: a range from 0 to 3rem.
 * Increments are left to implementers’ judgment. Requires: advancedSettings
 * @param wordSpacing Increasing space between words (word-spacing, related to a11y).
 * Recommended values: a range from 0 to 1rem. Increments are left to implementers’ judgment.
 * @param letterSpacing Increasing space between letters (letter-spacing, related to a11y).
 * Recommended values: a range from 0 to 0.5rem. Increments are left to implementers’
 * judgment.
 * @param bodyHyphens Enabling and disabling hyphenation. It impacts body, p, li, div and dd.
 * @param ligatures Enabling and disabling ligatures in Arabic (related to a11y).
 * @param a11yNormalize It impacts font style, weight and variant, text decoration, super and
 * subscripts. Requires: fontOverride
 */
internal data class UserProperties(
    // View mode
    val view: View? = null,

    // Pagination
    val colCount: Int? = null,

    // Appearance
    val darkenImages: Boolean? = null,
    val invertImages: Boolean? = null,

    // Colors
    val textColor: Color? = null,
    val backgroundColor: Color? = null,
    val linkColor: Color? = null,
    val visitedLinkColor: Color? = null,

    // Typography
    val fontOverride: Boolean? = null,
    val fontFamily: List<String>? = null,
    val fontSize: Length? = null,
    val fontWeight: Int? = null,
    val lineLength: Length? = null,

    // Advanced settings
    val textAlign: TextAlign? = null,
    val lineHeight: Either<Length, Double>? = null, // line-height supports unitless numbers
    val paraSpacing: Length? = null,
    val paraIndent: Length.Rem? = null,
    val wordSpacing: Length.Rem? = null,
    val letterSpacing: Length.Rem? = null,
    val bodyHyphens: Hyphens? = null,
    val ligatures: Ligatures? = null,

    // Accessibility
    val a11yNormalize: Boolean? = null,

    val overrides: Map<String, String?> = emptyMap(),
) : ReadiumCssProperties {

    override fun toCssProperties(): Map<String, String?> = buildMap {
        // View mode
        putCss("--USER__view", view)

        // Pagination
        putCss("--USER__colCount", colCount)

        // Appearance
        putCss("--USER__darkenImages", flag("darken", darkenImages))
        putCss("--USER__invertImages", flag("invert", invertImages))

        // Colors
        putCss("--USER__textColor", textColor)
        putCss("--USER__backgroundColor", backgroundColor)
        putCss("--USER__linkColor", textColor)
        putCss("--USER__visitedColor", backgroundColor)

        // Typography
        putCss("--USER__fontOverride", flag("font", fontOverride))
        putCss("--USER__fontFamily", fontFamily)
        putCss("--USER__fontSize", fontSize)
        putCss("--USER__fontWeight", fontWeight)
        putCss("--USER__lineLength", lineLength)
        putCss("--USER__textAlign", textAlign)
        lineHeight
            ?.onLeft { putCss("--USER__lineHeight", it) }
            ?.onRight { putCss("--USER__lineHeight", it) }
            ?: run { put("--USER__lineHeight", null) }
        putCss("--USER__paraSpacing", paraSpacing)
        putCss("--USER__paraIndent", paraIndent)
        putCss("--USER__wordSpacing", wordSpacing)
        putCss("--USER__letterSpacing", letterSpacing)
        putCss("--USER__bodyHyphens", bodyHyphens)
        putCss("--USER__ligatures", ligatures)

        // Accessibility
        putCss("--USER__a11yNormalize", flag("a11y", a11yNormalize))

        for ((key, value) in overrides) {
            put(key, value)
        }
    }
}

/**
 * Reading System properties.
 *
 * See https://readium.org/readium-css/docs/CSS19-api.html#reading-system-styles
 *
 * @param colWidth The optimal column’s width. It serves as a floor in our design.
 * @param colCount The optimal number of columns (depending on the columns’ width).
 * @param colGap The gap between columns. You must account for this gap when scrolling.
 * @param pageGutter The horizontal page margins.
 * @param disableVerticalPagination If pagination should be disabled with vertical text.
 * @param flowSpacing The default vertical margins for HTML5 flow content e.g. pre, figure,
 * blockquote, etc.
 * @param paraSpacing The default vertical margins for paragraphs.
 * @param paraIndent The default text-indent for paragraphs.
 * @param maxLineLength The optimal line-length. It must be set in rem in order to take :root’s
 * font-size as a reference, whichever the body’s    font-size might be.
 * @param maxMediaWidth The max-width for media elements i.e. img, svg, audio and video.
 * @param maxMediaHeight The max-height for media elements i.e. img, svg, audio and video.
 * @param boxSizingMedia The box model (box-sizing) you want to use for media elements.
 * @param boxSizingTable The box model (box-sizing) you want to use for tables.
 * @param textColor The default color for body copy’s text.
 * @param backgroundColor The default background-color for pages.
 * @param selectionTextColor The color for selected text.
 * @param selectionBackgroundColor The background-color for selected text.
 * @param linkColor The default color for hyperlinks.
 * @param visitedColor The default color for visited hyperlinks.
 * @param primaryColor An optional primary accentuation color you could use for headings or any
 * other element of your choice.
 * @param secondaryColor An optional secondary accentuation color you could use for any element
 * of your choice.
 * @param baseFontFamily The default typeface for body copy in case the ebook doesn’t have one
 * declared. Please note some languages have a specific font-stack (japanese, hindi, etc.)
 * @param baseLineHeight The default line-height for body copy in case the ebook doesn’t have
 * one declared.
 * @param oldStyleTf An old style serif font-stack relying on pre-installed fonts.
 * @param modernTf A modern serif font-stack relying on pre-installed fonts.
 * @param sansTf A neutral sans-serif font-stack relying on pre-installed fonts.
 * @param humanistTf A humanist sans-serif font-stack relying on pre-installed fonts.
 * @param monospaceTf A monospace font-stack relying on pre-installed fonts.
 * @param serifJa A Mincho font-stack whose fonts with proportional latin characters are
 * prioritized for horizontal writing.
 * @param sansSerifJa A Gothic font-stack whose fonts with proportional latin characters are
 * prioritized for horizontal writing.
 * @param serifJaV A Mincho font-stack whose fonts with fixed-width latin characters are
 * prioritized for vertical writing.
 * @param sansSerifJaV A Gothic font-stack whose fonts with fixed-width latin characters are
 * prioritized for vertical writing.
 * @param compFontFamily The typeface for headings.
 * The value can be another variable e.g. var(-RS__humanistTf).
 * @param codeFontFamily The typeface for code snippets.
 * The value can be another variable e.g. var(-RS__monospaceTf).
 */
internal data class RsProperties(
    // Pagination
    val colWidth: Length? = null,
    val colCount: Int? = null,
    val colGap: Length.Absolute? = null,
    val pageGutter: Length.Px? = null,
    val disableVerticalPagination: Boolean? = null,

    // Vertical rhythm
    val flowSpacing: Length? = null,
    val paraSpacing: Length? = null,
    val paraIndent: Length? = null,

    // Safeguards
    val maxLineLength: Length.Rem? = null,
    val maxMediaWidth: Length? = null,
    val maxMediaHeight: Length? = null,
    val boxSizingMedia: BoxSizing? = null,
    val boxSizingTable: BoxSizing? = null,

    // Colors
    val textColor: Color? = null,
    val backgroundColor: Color? = null,
    val selectionTextColor: Color? = null,
    val selectionBackgroundColor: Color? = null,
    val linkColor: Color? = null,
    val visitedColor: Color? = null,
    val primaryColor: Color? = null,
    val secondaryColor: Color? = null,

    // Typography
    val baseFontFamily: List<String>? = null,
    val baseLineHeight: Either<Length, Double>? = null, // line-height supports unitless numbers

    // Default font-stacks
    val oldStyleTf: List<String>? = null,
    val modernTf: List<String>? = null,
    val sansTf: List<String>? = null,
    val humanistTf: List<String>? = null,
    val monospaceTf: List<String>? = null,

    // Default font-stacks for Japanese publications
    val serifJa: List<String>? = null,
    val sansSerifJa: List<String>? = null,
    val serifJaV: List<String>? = null,
    val sansSerifJaV: List<String>? = null,

    // Default styles for unstyled publications
    val compFontFamily: List<String>? = null,
    val codeFontFamily: List<String>? = null,

    val overrides: Map<String, String?> = emptyMap(),
) : ReadiumCssProperties {

    override fun toCssProperties(): Map<String, String?> = buildMap {
        // Pagination
        putCss("--RS__colWidth", colWidth)
        putCss("--RS__colCount", colCount)
        putCss("--RS__colGap", colGap)
        putCss("--RS__pageGutter", pageGutter)
        putCss("--RS__disablePagination", flag("noVerticalPagination", disableVerticalPagination))

        // Vertical rhythm
        putCss("--RS__flowSpacing", flowSpacing)
        putCss("--RS__paraSpacing", paraSpacing)
        putCss("--RS__paraIndent", paraIndent)

        // Safeguards
        putCss("--RS__maxLineLength", maxLineLength)
        putCss("--RS__maxMediaWidth", maxMediaWidth)
        putCss("--RS__maxMediaHeight", maxMediaHeight)
        putCss("--RS__boxSizingMedia", boxSizingMedia)
        putCss("--RS__boxSizingTable", boxSizingTable)

        // Colors
        putCss("--RS__textColor", textColor)
        putCss("--RS__backgroundColor", backgroundColor)
        putCss("--RS__selectionTextColor", selectionTextColor)
        putCss("--RS__selectionBackgroundColor", selectionBackgroundColor)
        putCss("--RS__linkColor", linkColor)
        putCss("--RS__visitedColor", visitedColor)
        putCss("--RS__primaryColor", primaryColor)
        putCss("--RS__secondaryColor", secondaryColor)

        // Typography
        putCss("--RS__baseFontFamily", baseFontFamily)
        baseLineHeight
            ?.onLeft { putCss("--RS__baseLineHeight", it) }
            ?.onRight { putCss("--RS__baseLineHeight", it) }
            ?: run { put("--RS__baseLineHeight", null) }

        // Default font-stacks
        putCss("--RS__oldStyleTf", oldStyleTf)
        putCss("--RS__modernTf", modernTf)
        putCss("--RS__sansTf", sansTf)
        putCss("--RS__humanistTf", humanistTf)
        putCss("--RS__monospaceTf", monospaceTf)

        // Default font-stacks for Japanese publications
        putCss("--RS__serif-ja", serifJa)
        putCss("--RS__sans-serif-ja", sansSerifJa)
        putCss("--RS__serif-ja-v", serifJaV)
        putCss("--RS__sans-serif-ja-v", sansSerifJaV)

        // Default styles for unstyled publications
        putCss("--RS__compFontFamily", compFontFamily)
        putCss("--RS__codeFontFamily", codeFontFamily)

        for ((key, value) in overrides) {
            put(key, value)
        }
    }
}

/** User view. */
@ExperimentalReadiumApi
public enum class View(private val css: String) : Cssable {
    PAGED("readium-paged-on"),
    SCROLL("readium-scroll-on"),
    ;

    override fun toCss(): String = css
}

/** CSS color. */
@ExperimentalReadiumApi
public interface Color : Cssable {

    public data class Rgb(val red: kotlin.Int, val green: kotlin.Int, val blue: kotlin.Int) : Color {
        init {
            require(red in 0..255)
            require(green in 0..255)
            require(blue in 0..255)
        }

        override fun toCss(): String = "rgb($red, $green, $blue)"
    }

    @JvmInline
    public value class Hex(public val color: String) : Color {
        init {
            require(Regex("^#(?:[0-9a-fA-F]{3}){1,2}$").matches(color))
        }

        override fun toCss(): String = color
    }

    @JvmInline
    public value class Int(@ColorInt public val color: kotlin.Int) : Color {
        override fun toCss(): String =
            String.format("#%06X", 0xFFFFFF and color)
    }
}

/** CSS length dimension. */
@ExperimentalReadiumApi
public interface Length : Cssable {

    /** Absolute CSS length. */
    public interface Absolute : Length

    /** Centimeters */
    @JvmInline
    public value class Cm(public val value: Double) : Absolute {
        override fun toCss(): String = value.toCss("cm")
    }

    /** Millimeters */
    @JvmInline
    public value class Mm(public val value: Double) : Absolute {
        override fun toCss(): String = value.toCss("mm")
    }

    /** Inches */
    @JvmInline
    public value class In(public val value: Double) : Absolute {
        override fun toCss(): String = value.toCss("in")
    }

    /** Pixels */
    @JvmInline
    public value class Px(public val value: Double) : Absolute {
        override fun toCss(): String = value.toCss("px")
    }

    /** Points */
    @JvmInline
    public value class Pt(public val value: Double) : Absolute {
        override fun toCss(): String = value.toCss("pt")
    }

    /** Picas */
    @JvmInline
    public value class Pc(public val value: Double) : Absolute {
        override fun toCss(): String = value.toCss("pc")
    }

    /** Relative CSS length. */
    public interface Relative : Length

    /** Relative to the font-size of the element. */
    @JvmInline
    public value class Em(public val value: Double) : Relative {
        override fun toCss(): String = value.toCss("em")
    }

    /** Relative to the width of the "0" (zero). */
    @JvmInline
    public value class Ch(public val value: Double) : Relative {
        override fun toCss(): String = value.toCss("ch")
    }

    /** Relative to font-size of the root element. */
    @JvmInline
    public value class Rem(public val value: Double) : Relative {
        override fun toCss(): String = value.toCss("rem")
    }

    /** Relative to 1% of the width of the viewport. */
    @JvmInline
    public value class Vw(public val value: Double) : Relative {
        override fun toCss(): String = value.toCss("vw")
    }

    /** Relative to 1% of the height of the viewport. */
    @JvmInline
    public value class Vh(public val value: Double) : Relative {
        override fun toCss(): String = value.toCss("vh")
    }

    /** Relative to 1% of viewport's smaller dimension. */
    @JvmInline
    public value class VMin(public val value: Double) : Relative {
        override fun toCss(): String = value.toCss("vmin")
    }

    /** Relative to 1% of viewport's larger dimension. */
    @JvmInline
    public value class VMax(public val value: Double) : Relative {
        override fun toCss(): String = value.toCss("vmax")
    }

    /** Relative to the parent element. */
    @JvmInline
    public value class Percent(public val value: Double) : Relative {
        override fun toCss(): String = (value * 100).toCss("%")
    }
}

/** Number of CSS columns. */
@ExperimentalReadiumApi
public enum class ColCount(private val css: String) : Cssable {
    AUTO("auto"),
    ONE("1"),
    TWO("2"),
    ;

    override fun toCss(): String? = css
}

/** CSS text alignment. */
@ExperimentalReadiumApi
public enum class TextAlign(private val css: String) : Cssable {
    START("start"),
    LEFT("left"),
    RIGHT("right"),
    JUSTIFY("justify"),
    ;

    override fun toCss(): String? = css
}

/** CSS hyphenation. */
@ExperimentalReadiumApi
public enum class Hyphens(private val css: String) : Cssable {
    NONE("none"),
    AUTO("auto"),
    ;

    override fun toCss(): String? = css
}

/** CSS ligatures. */
@ExperimentalReadiumApi
public enum class Ligatures(private val css: String) : Cssable {
    NONE("none"),
    COMMON("common-ligatures"),
    ;

    override fun toCss(): String? = css
}

/** CSS box sizing. */
@ExperimentalReadiumApi
public enum class BoxSizing(private val css: String) : Cssable {
    CONTENT_BOX("content-box"),
    BORDER_BOX("border-box"),
    ;

    override fun toCss(): String? = css
}

@ExperimentalReadiumApi
public fun interface Cssable {
    public fun toCss(): String?
}

private fun MutableMap<String, String?>.putCss(name: String, value: Cssable?) {
    put(name, value?.toCss())
}

private fun MutableMap<String, String?>.putCss(name: String, value: Double?) {
    put(name, value?.toString())
}

private fun MutableMap<String, String?>.putCss(name: String, value: Int?) {
    put(name, value?.toString())
}

@Suppress("unused")
private fun MutableMap<String, String?>.putCss(name: String, value: String?) {
    put(name, value?.toCss())
}

private fun MutableMap<String, String?>.putCss(name: String, values: List<String>?) {
    val value = values?.joinToString(", ") { it.toCss() }
    put(name, value)
}

/** Readium CSS boolean flag. */
private fun flag(name: String, value: Boolean?) = Cssable {
    if (value == true) {
        "readium-$name-on"
    } else {
        null
    }
}

/**
* Converts a [String] to a CSS literal.
*/
private fun String.toCss(): String =
    "\\\"" + replace("\"", "\\\"") + "\\\""

/**
 * Converts a [Double] to a string literal with the given [unit].
 */
private fun Double.toCss(unit: String): String =
    NumberFormat.getNumberInstance(Locale.ROOT).run {
        maximumFractionDigits = 2
        format(this@toCss)
    } + unit
