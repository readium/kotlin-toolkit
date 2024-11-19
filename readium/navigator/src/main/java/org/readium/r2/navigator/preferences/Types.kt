/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.preferences

import android.graphics.Color as AndroidColor
import androidx.annotation.ColorInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.readium.r2.shared.ExperimentalReadiumApi

// https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-day_mode.css
@ColorInt private val dayContentColor: Int = AndroidColor.parseColor("#121212")

@ColorInt private val dayBackgroundColor: Int = AndroidColor.parseColor("#FFFFFF")

// https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-night_mode.css
@ColorInt private val nightContentColor: Int = AndroidColor.parseColor("#FEFEFE")

@ColorInt private val nightBackgroundColor: Int = AndroidColor.parseColor("#000000")

// https://github.com/readium/readium-css/blob/master/css/src/modules/ReadiumCSS-sepia_mode.css
@ColorInt private val sepiaContentColor: Int = AndroidColor.parseColor("#121212")

@ColorInt private val sepiaBackgroundColor: Int = AndroidColor.parseColor("#faf4e8")

@Serializable
public enum class Theme(
    @ColorInt public val contentColor: Int,
    @ColorInt public val backgroundColor: Int,
) {
    @SerialName("light")
    LIGHT(contentColor = dayContentColor, backgroundColor = dayBackgroundColor),

    @SerialName("dark")
    DARK(contentColor = nightContentColor, backgroundColor = nightBackgroundColor),

    @SerialName("sepia")
    SEPIA(contentColor = sepiaContentColor, backgroundColor = sepiaBackgroundColor),
}

@Serializable
public enum class TextAlign {
    /** Align the text in the center of the page. */
    @SerialName("center")
    CENTER,

    /** Stretch lines of text that end with a soft line break to fill the width of the page. */
    @SerialName("justify")
    JUSTIFY,

    /** Align the text on the leading edge of the page. */
    @SerialName("start")
    START,

    /** Align the text on the trailing edge of the page. */
    @SerialName("end")
    END,

    /** Align the text on the left edge of the page. */
    @SerialName("left")
    LEFT,

    /** Align the text on the right edge of the page. */
    @SerialName("right")
    RIGHT,
}

@ExperimentalReadiumApi
@Serializable
public enum class ColumnCount {
    @SerialName("auto")
    AUTO,

    @SerialName("1")
    ONE,

    @SerialName("2")
    TWO,
}

@Serializable
public enum class ImageFilter {
    @SerialName("darken")
    DARKEN,

    @SerialName("invert")
    INVERT,
}

/**
 * Typeface for a publication's text.
 *
 * For a list of vetted font families, see https://readium.org/readium-css/docs/CSS10-libre_fonts.
 */
@JvmInline
@Serializable
public value class FontFamily(public val name: String) {

    public companion object {
        // Generic font families
        // See https://www.w3.org/TR/css-fonts-4/#generic-font-families
        public val SERIF: FontFamily = FontFamily("serif")
        public val SANS_SERIF: FontFamily = FontFamily("sans-serif")
        public val CURSIVE: FontFamily = FontFamily("cursive")
        public val FANTASY: FontFamily = FontFamily("fantasy")
        public val MONOSPACE: FontFamily = FontFamily("monospace")

        // Accessibility fonts embedded with Readium
        public val ACCESSIBLE_DFA: FontFamily = FontFamily("AccessibleDfA")
        public val IA_WRITER_DUOSPACE: FontFamily = FontFamily("IA Writer Duospace")
        public val OPEN_DYSLEXIC: FontFamily = FontFamily("OpenDyslexic")
    }
}

/**
 * Packed color int.
 */
@Serializable
@JvmInline
public value class Color(@ColorInt public val int: Int)

/**
 * Layout axis.
 */
@Serializable
public enum class Axis(public val value: String) {
    @SerialName("horizontal")
    HORIZONTAL("horizontal"),

    @SerialName("vertical")
    VERTICAL("vertical"),
}

/**
 * Synthetic spread policy.
 */
@Serializable
public enum class Spread(public val value: String) {
    @SerialName("auto")
    AUTO("auto"),

    @SerialName("never")
    NEVER("never"),

    @SerialName("always")
    ALWAYS("always"),
}

/**
 * Direction of the reading progression across resources.
 */
@Serializable
public enum class ReadingProgression(public val value: String) {
    @SerialName("ltr")
    LTR("ltr"),

    @SerialName("rtl")
    RTL("rtl"),
}

/**
 * Method for constraining a resource inside the viewport.
 */
@Serializable
public enum class Fit(public val value: String) {
    @SerialName("cover")
    COVER("cover"),

    @SerialName("contain")
    CONTAIN("contain"),

    @SerialName("width")
    WIDTH("width"),

    @SerialName("height")
    HEIGHT("height"),
}
