/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import androidx.annotation.ColorInt
import org.readium.r2.shared.util.ValueEncoder

class ReadiumCss {

    /**
     * https://readium.org/readium-css/docs/CSS19-api.html
     */
    data class Property<V>(
        val name: String,
        private val encoder: ValueEncoder<V, String>
    ) : ValueEncoder<V, String> by encoder {

        /**
         * Properties for user settings.
         */
        object User {
            /**
             * The number of columns (column-count) the user wants displayed (one-page view or
             * two-page spread).
             *
             * To reset, change the value to auto.
             */
            val COL_COUNT = Property("--USER__colCount", ColCount)

            /**
             * A factor applied to horizontal margins (padding-left and padding-right) the user
             * wants to set.
             *
             * Recommended values: a range from 0.5 to 2. Increments are left to implementers’
             * judgment. To reset, change the value to 1.
             */
            val PAGE_MARGINS = Property("--USER__pageMargins", Length)

            /**
             * The background-color for the whole screen.
             *
             * To reset, remove the CSS variable.
             */
            val BACKGROUND_COLOR = Property("--USER__backgroundColor", Color)

            /**
             * The color for textual contents.
             *
             * It impacts all elements but headings and pre in the DOM.
             * To reset, remove the CSS variable.
             */
            val TEXT_COLOR = Property("--USER__textColor", Color)

            /**
             * The alignment (text-align) the user prefers.
             *
             * It impacts body, li, and p which are not children of blockquote and figcaption.
             *
             * Required flag: :--advancedSettings
             */
            val TEXT_ALIGN = Property("--USER__textAlign", TextAlign)

            /**
             * Enabling and disabling hyphenation.
             *
             * It impacts body, p, li, div and dd.
             *
             * Required flag: :--advancedSettings
             */
            val BODY_HYPHENS = Property("--USER__bodyHyphens", Hyphens)

            /**
             * The typeface (font-family) the user wants to read with.
             *
             * It impacts body, p, li, div, dt, dd and phrasing elements which don’t have a lang or
             * xml:lang attribute.
             *
             * To reset, remove the required flag.
             *
             * Required flag: :--fontOverride
             */
            val FONT_FAMILY = Property("--USER__fontFamily", Literal.StringList)

            /**
             * Increasing and decreasing the root font-size. It will serve as a reference for the
             * cascade.
             *
             * To reset, remove the required flag.
             */
            val FONT_SIZE = Property("--USER__fontSize", Length)

            /**
             * The type scale the user wants to use for the publication.
             *
             * It impacts headings, p, li, div, pre, dd, small, sub, and sup.
             * Recommended values: a range from 75% to 250%. Increments are left to implementers’
             * judgment.
             *
             * Required flag: :--advancedSettings
             */
            val TYPE_SCALE = Property("--USER__typeScale", Length)

            /**
             * Increasing and decreasing leading (line-height).
             *
             * It impacts body, p, li and div
             * Recommended values: a range from 1 to 2. Increments are left to implementers’ judgment.
             *
             * Required flag: :--advancedSettings
             */
            val LINE_HEIGHT = Property("--USER__lineHeight", Length)

            /**
             * The vertical margins (margin-top and margin-bottom) for paragraphs.
             *
             * Recommended values: a range from 0 to 2rem. Increments are left to implementers’
             * judgment.
             *
             * Required flag: :--advancedSettings
             */
            val PARA_SPACING = Property("--USER__paraSpacing", Length)

            /**
             * The text-indent for paragraphs.
             *
             * Recommended values: a range from 0 to 3rem. Increments are left to implementers’
             * judgment.
             *
             * Required flag: :--advancedSettings
             */
            val PARA_INDENT = Property("--USER__paraIndent", Length.Relative.Rem)

            /**
             * Increasing space between words (word-spacing, related to a11y).
             *
             * Recommended values: a range from 0 to 1rem. Increments are left to implementers’
             * judgment.
             *
             * Required flag: :--advancedSettings
             */
            val WORD_SPACING = Property("--USER__wordSpacing", Length.Relative.Rem)

            /**
             * Increasing space between letters (letter-spacing, related to a11y).
             *
             * Recommended values: a range from 0 to 0.5rem. Increments are left to implementers’
             * judgment.
             *
             * Required flag: :--advancedSettings
             */
            val LETTER_SPACING = Property("--USER__letterSpacing", Length.Relative.Rem)

            /**
             * Enabling and disabling ligatures in Arabic (related to a11y).
             *
             * Required flag: :--advancedSettings
             */
            val LIGATURES = Property("--USER__ligatures", Ligatures)
        }

        /**
         * Properties for the Reading System.
         */
        object RS {

            // Pagination

            /** The optimal column’s width. It serves as a floor in our design. */
            val COL_WIDTH = Property("--RS__colWidth", Length)

            /** The optimal number of columns (depending on the columns’ width). */
            val COL_COUNT = Property("--RS__colCount", ColCount)

            /** The gap between columns. You must account for this gap when scrolling. */
            val COL_GAP = Property("--RS__colGap", Length.Absolute)

            /** The horizontal page margins. */
            val PAGE_GUTTER = Property("--RS__pageGutter", Length.Absolute)

            /**
             * The optimal line-length. It must be set in rem in order to take :root’s font-size as
             * a reference, whichever the body’s font-size might be.
             */
            val MAX_LINE_LENGTH = Property("--RS__maxLineLength", Length.Relative.Rem)


            // Safeguards

            /** The max-width for media elements i.e. img, svg, audio and video. */
            val MAX_MEDIA_WIDTH = Property("--RS__maxMediaWidth", Length)

            /** The max-height for media elements i.e. img, svg, audio and video. */
            val MAX_MEDIA_HEIGHT = Property("--RS__maxMediaHeight", Length)

            /** The box model (box-sizing) you want to use for media elements. */
            val BOX_SIZING_MEDIA = Property("--RS__boxSizingMedia", BoxSizing)

            /** The box model (box-sizing) you want to use for tables. */
            val BOX_SIZING_TABLE = Property("--RS__boxSizingTable", BoxSizing)


            // Default font-stacks

            /** An old style serif font-stack relying on pre-installed fonts. */
            val OLD_STYLE_TF = Property("--RS__oldStyleTf", Literal.StringList)
            /** A modern serif font-stack relying on pre-installed fonts. */
            val MODERN_TF = Property("--RS__modernTf", Literal.StringList)
            /** A neutral sans-serif font-stack relying on pre-installed fonts. */
            val SANS_TF = Property("--RS__sansTf", Literal.StringList)
            /** A humanist sans-serif font-stack relying on pre-installed fonts. */
            val HUMANIST_TF = Property("--RS__humanistTf", Literal.StringList)
            /** A monospace font-stack relying on pre-installed fonts. */
            val MONOSPACE_TF = Property("--RS__monospaceTf", Literal.StringList)


            // Default font-stacks for Japanese publications

            /**
             * A Mincho font-stack whose fonts with proportional latin characters are prioritized
             * for horizontal writing.
             */
            val SERIF_JA = Property("--RS__serif-ja", Literal.StringList)

            /**
             * A Gothic font-stack whose fonts with proportional latin characters are prioritized
             * for horizontal writing.
             */
            val SANS_SERIF_JA = Property("--RS__sans-serif-ja", Literal.StringList)

            /**
             * A Mincho font-stack whose fonts with fixed-width latin characters are prioritized for
             * vertical writing.
             */
            val SERIF_JA_V = Property("--RS__serif-ja-v", Literal.StringList)

            /**
             * A Gothic font-stack whose fonts with fixed-width latin characters are prioritized for
             * vertical writing.
             */
            val SANS_SERIF_JA_V = Property("--RS__sans-serif-ja-v", Literal.StringList)


            // Default colors for all ebooks

            /** The default color for body copy’s text. */
            val TEXT_COLOR = Property("--RS__textColor", Color)

            /** The default background-color for pages. */
            val BACKGROUND_COLOR = Property("--RS__backgroundColor", Color)

            /** The background-color for selected text. */
            val SELECTION_BACKGROUND_COLOR = Property("--RS__selectionBackgroundColor", Color)

            /** The color for selected text. */
            val SELECTION_TEXT_COLOR = Property("--RS__selectionTextColor", Color)


            // Default styles for unstyled publications

            /**
             * The typeface for headings.
             * The value can be another variable e.g. var(-RS__humanistTf).
             */
            val COMP_FONT_FAMILY = Property("--RS__compFontFamily", Literal.StringList)

            /**
             * The typeface for code snippets.
             * The value can be another variable e.g. var(-RS__monospaceTf).
             */
            val CODE_FONT_FAMILY = Property("--RS__codeFontFamily", Literal.StringList)


            // Typography

            /**
             * The scale to be used for computing all elements’ font-size. Since those font sizes
             * are computed dynamically, you can set a smaller type scale when the user sets one
             * of the largest font sizes.
             */
            val TYPE_SCALE = Property("--RS__typeScale", Literal.Number)

            /**
             * The default typeface for body copy in case the ebook doesn’t have one declared.
             * Please note some languages have a specific font-stack (japanese, chinese, hindi, etc.)
             */
            val BASE_FONT_FAMILY = Property("--RS__baseFontFamily", Literal.StringList)

            /**
             * The default line-height for body copy in case the ebook doesn’t have one declared.
             */
            val BASE_LINE_HEIGHT = Property("--RS__baseLineHeight", Length)


            // Vertical rhythm

            /**
             * The default vertical margins for HTML5 flow content e.g. pre, figure, blockquote, etc.
             */
            val FLOW_SPACING = Property("--RS__flowSpacing", Length)

            /**
             * The default vertical margins for paragraphs.
             */
            val PARA_SPACING = Property("--RS__paraSpacing", Length)

            /**
             * The default text-indent for paragraphs.
             */
            val PARA_INDENT = Property("--RS__paraIndent", Length)


            // Hyperlinks

            /** The default color for hyperlinks. */
            val LINK_COLOR = Property("--RS__linkColor", Color)

            /** The default color for visited hyperlinks. */
            val VISITED_COLOR = Property("--RS__visitedColor", Color)


            // Accentuation colors

            /**
             * An optional primary accentuation color you could use for headings or any other
             * element of your choice.
             */
            val PRIMARY_COLOR = Property("--RS__primaryColor", Color)

            /**
             * An optional secondary accentuation color you could use for any element of your
             * choice.
             */
            val SECONDARY_COLOR = Property("--RS__secondaryColor", Color)
        }
    }

    /** CSS color. */
    data class Color(val css: String) {
        companion object : ValueEncoder<Color, String> {
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

            override fun encode(value: Color): String = value.css
        }
    }

    /** CSS literal. */
    object Literal {
        /** CSS integer number. */
        object Integer : ValueEncoder<Int, kotlin.String> {
            override fun encode(value: Int): kotlin.String =
                value.toString()
        }

        /** CSS floating point number. */
        object Number : ValueEncoder<Double, kotlin.String> {
            override fun encode(value: Double): kotlin.String =
                value.toString()
        }

        /** CSS string literal. */
        object String : ValueEncoder<kotlin.String, kotlin.String> {
            override fun encode(value: kotlin.String): kotlin.String =
                value.toCss()
        }

        /** CSS list of string literals. */
        object StringList : ValueEncoder<List<kotlin.String>, kotlin.String> {
            override fun encode(value: List<kotlin.String>): kotlin.String =
                value.joinToString(", ") { it.toCss() }
        }
    }

    /** CSS length dimension. */
    interface Length {
        val value: Double
        val unit: String

        open class Encoder<T : Length> : ValueEncoder<T, String> {
            override fun encode(value: T): String =
                "${value.value}.${value.unit}"
        }

        companion object : Encoder<Length>()

        /** Absolute CSS length. */
        sealed class Absolute(
            override val value: Double,
            override val unit: String
        ) : Length {
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

            companion object : Encoder<Absolute>()
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
            class Rem(value: Double) : Relative(value, "rem") {
                companion object : Encoder<Rem>()
            }
            /** Relative to 1% of the width of the viewport. */
            class Vw(value: Double) : Relative(value, "vw")
            /** Relative to 1% of the height of the viewport. */
            class Vh(value: Double) : Relative(value, "vh")
            /** Relative to 1% of viewport's smaller dimension. */
            class VMin(value: Double) : Relative(value, "vmin")
            /** Relative to 1% of viewport's larger dimension. */
            class VMax(value: Double) : Relative(value, "vmax")
            /** Relative to the parent element. */
            class Percent(value: Double) : Relative(value, "%")

            companion object : Encoder<Relative>()
        }
    }

    /** Number of CSS columns. */
    enum class ColCount(val css: String) {
        AUTO("auto"),
        ONE("1"),
        TWO("2");

        companion object : ValueEncoder<ColCount, String> {
            override fun encode(value: ColCount): String = value.css
        }
    }

    /** CSS text alignment. */
    enum class TextAlign(val css: String) {
        LEFT("left"),
        RIGHT("right"),
        JUSTIFY("justify");

        companion object : ValueEncoder<TextAlign, String> {
            override fun encode(value: TextAlign): String = value.css
        }
    }

    /** CSS hyphenation. */
    enum class Hyphens(val css: String) {
        NONE("none"),
        AUTO("auto");

        companion object : ValueEncoder<Hyphens, String> {
            override fun encode(value: Hyphens): String = value.css
        }
    }

    /** CSS ligatures. */
    enum class Ligatures(val css: String) {
        NONE("none"),
        COMMON("common-ligatures");

        companion object : ValueEncoder<Ligatures, String> {
            override fun encode(value: Ligatures): String = value.css
        }
    }

    /** CSS box sizing. */
    enum class BoxSizing(val css: String) {
        CONTENT_BOX("content-box"),
        BORDER_BOX("border-box");

        companion object : ValueEncoder<BoxSizing, String> {
            override fun encode(value: BoxSizing): String = value.css
        }
    }
}

/**
 * Converts a [String] to a CSS literal.
 */
private fun String.toCss(): String =
    '"' + replace("\"", "\\\"") + '"'
