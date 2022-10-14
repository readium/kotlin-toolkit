/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import androidx.annotation.ColorInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.readium.r2.shared.ExperimentalReadiumApi
import android.graphics.Color as AndroidColor

// https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-day_mode.css
@ColorInt private val dayContentColor: Int = AndroidColor.parseColor("#121212")
@ColorInt private val dayBackgroundColor: Int = AndroidColor.parseColor("#FFFFFF")
// https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-night_mode.css
@ColorInt private val nightContentColor: Int = AndroidColor.parseColor("#FEFEFE")
@ColorInt private val nightBackgroundColor: Int = AndroidColor.parseColor("#000000")
// https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-sepia_mode.css
@ColorInt private val sepiaContentColor: Int = AndroidColor.parseColor("#121212")
@ColorInt private val sepiaBackgroundColor: Int = AndroidColor.parseColor("#faf4e8")

@ExperimentalReadiumApi
@Serializable
enum class Theme(@ColorInt val contentColor: Int, @ColorInt val backgroundColor: Int) {
    @SerialName("light") LIGHT(contentColor = dayContentColor, backgroundColor = dayBackgroundColor),
    @SerialName("dark") DARK(contentColor = nightContentColor, backgroundColor = nightBackgroundColor),
    @SerialName("sepia") SEPIA(contentColor = sepiaContentColor, backgroundColor = sepiaBackgroundColor);
}

@ExperimentalReadiumApi
@Serializable
enum class TextAlign {
    /** Align the text in the center of the page. */
    @SerialName("center") CENTER,
    /** Stretch lines of text that end with a soft line break to fill the width of the page. */
    @SerialName("justify") JUSTIFY,
    /** Align the text on the leading edge of the page. */
    @SerialName("start") START,
    /** Align the text on the trailing edge of the page. */
    @SerialName("end") END,
    /** Align the text on the left edge of the page. */
    @SerialName("left") LEFT,
    /** Align the text on the right edge of the page. */
    @SerialName("right") RIGHT;
}

@ExperimentalReadiumApi
@Serializable
enum class TextNormalization {
    /** No text normalization. */
    @SerialName("none") NONE,
    /** Force bold text. */
    @SerialName("bold") BOLD,
    /** Normalize text to increase accessibility. */
    @SerialName("a11y") ACCESSIBILITY,
}

@ExperimentalReadiumApi
@Serializable
enum class ColumnCount {
    @SerialName("auto") AUTO,
    @SerialName("1") ONE,
    @SerialName("2") TWO;
}

@ExperimentalReadiumApi
@Serializable
enum class ImageFilter {
    @SerialName("none") NONE,
    @SerialName("darken") DARKEN,
    @SerialName("invert") INVERT;
}

/**
 * Typeface for a publication's text.
 *
 * When not available, the Navigator should use [alternate] as a fallback.
 */
@JvmInline
@ExperimentalReadiumApi
@Serializable
value class FontFamily(val name: String) {

    companion object {
        // Generic font families
        // See https://www.w3.org/TR/css-fonts-4/#generic-font-families
        val SERIF = FontFamily("serif")
        val SANS_SERIF = FontFamily("sans-serif")
        val CURSIVE = FontFamily("cursive")
        val FANTASY = FontFamily("fantasy")
        val MONOSPACE = FontFamily("monospace")

        // Vetted font families
        // See https://readium.org/readium-css/docs/CSS10-libre_fonts

        // Serif
        val CHARIS_SIL = FontFamily("Charis SIL")
        val FAUSTINA = FontFamily("Faustina")
        val IBM_PLEX_SERIF = FontFamily("IBM Plex Serif")
        val LITERATA = FontFamily("Literata")
        val MERRIWEATHER = FontFamily("Merriweather")
        val PT_SERIF = FontFamily("PT Serif")
        val VOLLKORN = FontFamily("Vollkorn")

        // Sans-serif
        val CLEAR_SANS = FontFamily("Clear Sans")
        val FIRA_SANS = FontFamily("Fira Sans")
        val LIBRE_FRANKLIN = FontFamily("Libre Franklin")
        val MERRIWEATHER_SANS = FontFamily("Merriweather Sans")
        val PT_SANS = FontFamily("PT Sans")
        val SOURCE_SANS_PRO = FontFamily("Source Sans Pro")

        // Accessibility
        val ACCESSIBLE_DFA = FontFamily("AccessibleDfA")
        val IA_WRITER_DUOSPACE = FontFamily("IA Writer Duospace")
        val OPEN_DYSLEXIC = FontFamily("OpenDyslexic")

        // System
        val ROBOTO = FontFamily("Roboto")
    }
}

@ExperimentalReadiumApi
@Serializable
@JvmInline
value class Color(@ColorInt val int: Int)

@ExperimentalReadiumApi
@Serializable
enum class Axis(val value: String) {
    @SerialName("horizontal") HORIZONTAL("horizontal"),
    @SerialName("vertical") VERTICAL("vertical");
}

@ExperimentalReadiumApi
@Serializable
enum class Spread(val value: String) {
    @SerialName("auto") AUTO("auto"),
    @SerialName("never") NEVER("never"),
    @SerialName("preferred") PREFERRED("preferred");
}
