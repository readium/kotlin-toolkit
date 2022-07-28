/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import androidx.annotation.ColorInt
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.ValueEncoder
import java.text.NumberFormat

/**
 * Holds a set of Readium CSS properties applied together.
 */
@ExperimentalReadiumApi
interface Properties: Cssable {
    fun toCssProperties(): Map<String, String?>

    override fun toCss(): String? {
        val props = toCssProperties()
            .filterValues { it != null }

        if (props.isEmpty()) {
            return ""
        }

        return props
            .map { (key, value) -> "$key: $value" }
            .joinToString(";\n") + ";\n"
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
 * @param pageMargins A factor applied to horizontal margins (padding-left and padding-right)
 * the user wants to set. Recommended values: a range from 0.5 to 2. Increments are left to
 * implementers’ judgment. To reset, change the value to 1.
 * @param appearance This flag applies a reading mode (sepia or night).
 * @param darkenImages This will only apply in night mode to darken images and impact img.
 * Requires: appearance = Appearance.Night
 * @param invertImages This will only apply in night mode to invert images and impact img.
 * Requires: appearance = Appearance.Night
 * @param textColor The color for textual contents. It impacts all elements but headings and pre
 * in the DOM. To reset, remove the CSS variable.
 * @param backgroundColor The background-color for the whole screen. To reset, remove the CSS
 * variable.
 * @param fontOverride This flag is required to change the font-family user setting.
 * @param fontFamily The typeface (font-family) the user wants to read with. It impacts body, p,
 * li, div, dt, dd and phrasing elements which don’t have a lang or xml:lang attribute. To reset,
 * remove the required flag. Requires: fontOverride
 * @param fontSize Increasing and decreasing the root font-size. It will serve as a reference
 * for the cascade. To reset, remove the required flag.
 * @param advancedSettings This flag is required to apply the font-size and/or advanced user
 * settings.
 * @param typeScale The type scale the user wants to use for the publication. It impacts
 * headings, p, li, div, pre, dd, small, sub, and sup. Recommended values: a range from 75% to
 * 250%. Increments are left to implementers’ judgment. Requires: advancedSettings
 * @param textAlign The alignment (text-align) the user prefers. It impacts body, li, and p
 * which are not children of blockquote and figcaption. Requires: advancedSettings
 * @param lineHeight Increasing and decreasing leading (line-height). It impacts body, p, li and
 * div. Recommended values: a range from 1 to 2. Increments are left to implementers’ judgment.
 * Requires: advancedSettings
 * @param paraSpacing The vertical margins (margin-top and margin-bottom) for paragraphs.
 * Recommended values: a range from 0 to 2rem. Increments are left to implementers’
 * judgment. Requires: advancedSettings = true
 * @param paraIndent The text-indent for paragraphs. Recommended values: a range from 0 to 3rem.
 * Increments are left to implementers’ judgment. Requires: advancedSettings
 * @param wordSpacing Increasing space between words (word-spacing, related to a11y).
 * Recommended values: a range from 0 to 1rem. Increments are left to implementers’ judgment.
 * Requires: advancedSettings
 * @param letterSpacing Increasing space between letters (letter-spacing, related to a11y).
 * Recommended values: a range from 0 to 0.5rem. Increments are left to implementers’
 * judgment. Requires: advancedSettings
 * @param bodyHyphens Enabling and disabling hyphenation. It impacts body, p, li, div and dd.
 * Requires: advancedSettings
 * @param ligatures Enabling and disabling ligatures in Arabic (related to a11y).
 * Requires: advancedSettings
 * @param a11yNormalize It impacts font style, weight and variant, text decoration, super and
 * subscripts. Requires: fontOverride
 */
@ExperimentalReadiumApi
data class UserProperties(
    // View mode
    val view: View? = null,

    // Pagination
    val colCount: ColCount? = null,
    val pageMargins: Length? = null,

    // Appearance
    val appearance: Appearance? = null,
    val darkenImages: Boolean? = null,
    val invertImages: Boolean? = null,

    // Colors
    val textColor: Color? = null,
    val backgroundColor: Color? = null,

    // Typography
    val fontOverride: Boolean? = null,
    val fontFamily: List<String>? = null,
    val fontSize: Length? = null,

    // Advanced settings
    val advancedSettings: Boolean? = null,
    val typeScale: Length? = null,
    val textAlign: TextAlign? = null,
    val lineHeight: Length? = null,
    val paraSpacing: Length? = null,
    val paraIndent: Length.Relative.Rem? = null,
    val wordSpacing: Length.Relative.Rem? = null,
    val letterSpacing: Length.Relative.Rem? = null,
    val bodyHyphens: Hyphens? = null,
    val ligatures: Ligatures? = null,

    // Accessibility
    val a11yNormalize: Boolean? = null,

    val overrides: Map<String, String?> = emptyMap(),
) : Properties {

    override fun toCssProperties(): Map<String, String?> = buildMap {
        // View mode
        putCss("--USER__view", view)

        // Pagination
        putCss("--USER__colCount", colCount)
        putCss("--USER__pageMargins", pageMargins)

        // Appearance
        putCss("--USER__appearance", appearance)
        putCss("--USER__darkenImages", flag("darken", darkenImages))
        putCss("--USER__invertImages", flag("invert", invertImages))

        // Colors
        putCss("--USER__textColor", textColor)
        putCss("--USER__backgroundColor", backgroundColor)

        // Typography
        putCss("--USER__fontOverride", flag("font", fontOverride))
        putCss("--USER__fontFamily", fontFamily)
        putCss("--USER__fontSize", fontSize)

        // Advanced settings
        putCss("--USER__advancedSettings", flag("advanced", advancedSettings))
        putCss("--USER__typeScale", typeScale)
        putCss("--USER__textAlign", textAlign)
        putCss("--USER__lineHeight", lineHeight)
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
 * @param flowSpacing The default vertical margins for HTML5 flow content e.g. pre, figure,
 * blockquote, etc.
 * @param paraSpacing The default vertical margins for paragraphs.
 * @param paraIndent The default text-indent for paragraphs.
 * @param maxLineLength The optimal line-length. It must be set in rem in order to take :root’s
 * font-size as a reference, whichever the body’s font-size might be.
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
 * @param typeScale The scale to be used for computing all elements’ font-size. Since those font
 * sizes are computed dynamically, you can set a smaller type scale when the user sets one
 * of the largest font sizes.
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
@ExperimentalReadiumApi
class RsProperties(
    // Pagination
    val colWidth: Length? = null,
    val colCount: ColCount? = null,
    val colGap: Length.Absolute? = null,
    val pageGutter: Length.Absolute? = null,

    // Vertical rhythm
    val flowSpacing: Length? = null,
    val paraSpacing: Length? = null,
    val paraIndent: Length? = null,

    // Safeguards
    val maxLineLength: Length.Relative.Rem? = null,
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
    val typeScale: Double? = null,
    val baseFontFamily: List<String>? = null,
    val baseLineHeight: Length? = null,

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
) : Properties {

    override fun toCssProperties(): Map<String, String?> = buildMap {
        // Pagination
        putCss("--RS__colWidth", colWidth)
        putCss("--RS__colCount", colCount)
        putCss("--RS__colGap", colGap)
        putCss("--RS__pageGutter", pageGutter)

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
        putCss("--RS__typeScale", typeScale)
        putCss("--RS__baseFontFamily", baseFontFamily)
        putCss("--RS__baseLineHeight", baseLineHeight)

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
enum class View(private val css: String) : Cssable {
    Paged("readium-paged-on"),
    Scroll("readium-scroll-on");

    override fun toCss(): String = css
}

/** Reading mode. */
@ExperimentalReadiumApi
enum class Appearance(private val css: String?) : Cssable {
    Sepia("readium-sepia-on"),
    Night("readium-night-on");

    override fun toCss(): String? = css

    companion object : ValueEncoder<Appearance, String?> {
        override fun encode(value: Appearance): String? = value.css
    }
}

/** CSS color. */
@ExperimentalReadiumApi
data class Color(private val css: String) : Cssable {
    override fun toCss(): String = css

    companion object {
        fun rgb(red: Int, green: Int, blue: Int): Color {
            require(red in 0..255)
            require(green in 0..255)
            require(blue in 0..255)
            return Color("rgb($red, $green, $blue)")
        }

        fun hex(color: String): Color {
            require(Regex("^#(?:[0-9a-fA-F]{3}){1,2}$").matches(color))
            return Color(color)
        }

        fun int(@ColorInt color: Int): Color =
            Color(String.format("#%06X", 0xFFFFFF and color))
    }
}

/** CSS length dimension. */
@ExperimentalReadiumApi
interface Length : Cssable {
    val value: Double
    val unit: String

    override fun toCss(): String? =
        NumberFormat.getNumberInstance().run {
            maximumFractionDigits = 2
            format(value)
        } + unit

    /** Absolute CSS length. */
    sealed class Absolute(
        override val value: Double,
        override val unit: String
    ) : Length, Cssable {
        /** Centimeters */
        class Cm(value: Double) : Absolute(value, "cm")
        /** Millimeters */
        class Mm(value: Double) : Absolute(value, "mm")
        /** Inches */
        class In(value: Double) : Absolute(value, "in")
        /** Pixels */
        class Px(value: Double) : Absolute(value, "px")
        /** Points */
        class Pt(value: Double) : Absolute(value, "pt")
        /** Picas */
        class Pc(value: Double) : Absolute(value, "pc")
    }

    /** Relative CSS length. */
    sealed class Relative(
        override val value: Double,
        override val unit: String
    ) : Length {
        /** Relative to the font-size of the element. */
        class Em(value: Double) : Relative(value, "em")
        /** Relative to the width of the "0" (zero). */
        class Ch(value: Double) : Relative(value, "ch")
        /** Relative to font-size of the root element. */
        class Rem(value: Double) : Relative(value, "rem")
        /** Relative to 1% of the width of the viewport. */
        class Vw(value: Double) : Relative(value, "vw")
        /** Relative to 1% of the height of the viewport. */
        class Vh(value: Double) : Relative(value, "vh")
        /** Relative to 1% of viewport's smaller dimension. */
        class VMin(value: Double) : Relative(value, "vmin")
        /** Relative to 1% of viewport's larger dimension. */
        class VMax(value: Double) : Relative(value, "vmax")
        /** Relative to the parent element. */
        class Percent(value: Double) : Relative(value * 100, "%")
    }
}

/** Number of CSS columns. */
@ExperimentalReadiumApi
enum class ColCount(private val css: String) : Cssable {
    Auto("auto"),
    One("1"),
    Two("2");

    override fun toCss(): String? = css
}

/** CSS text alignment. */
@ExperimentalReadiumApi
enum class TextAlign(private val css: String) : Cssable {
    Left("left"),
    Right("right"),
    Justify("justify");

    override fun toCss(): String? = css
}

/** CSS hyphenation. */
@ExperimentalReadiumApi
enum class Hyphens(private val css: String) : Cssable {
    None("none"),
    Auto("auto");

    override fun toCss(): String? = css
}

/** CSS ligatures. */
@ExperimentalReadiumApi
enum class Ligatures(private val css: String) : Cssable {
    None("none"),
    Common("common-ligatures");

    override fun toCss(): String? = css
}

/** CSS box sizing. */
@ExperimentalReadiumApi
enum class BoxSizing(private val css: String) : Cssable {
    ContentBox("content-box"),
    BorderBox("border-box");

    override fun toCss(): String? = css
}

@ExperimentalReadiumApi
fun interface Cssable {
    fun toCss(): String?
}

private fun MutableMap<String, String?>.putCss(name: String, value: Cssable?) {
    put(name, value?.toCss())
}

private fun MutableMap<String, String?>.putCss(name: String, double: Double?) {
    put(name, double?.toString())
}

private fun MutableMap<String, String?>.putCss(name: String, string: String?) {
    put(name, string?.toCss())
}

private fun MutableMap<String, String?>.putCss(name: String, strings: List<String>?) {
    val value = strings?.joinToString(", ") { it.toCss() }
    put(name, value)
}

/** Readium CSS boolean flag. */
private fun flag(name: String, value: Boolean?) = Cssable {
    if (value == true) "readium-$name-on"
    else null
}

/**
* Converts a [String] to a CSS literal.
*/
private fun String.toCss(): String =
    '"' + replace("\"", "\\\"") + '"'
