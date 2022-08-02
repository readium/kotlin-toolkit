/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@Serializable
enum class Theme {
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
    @SerialName("sepia") SEPIA;
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
enum class ColumnCount {
    @SerialName("auto") AUTO,
    @SerialName("1") ONE,
    @SerialName("2") TWO;
}

@ExperimentalReadiumApi
@JvmInline
value class Font(val name: String? = null) {
    companion object {
        val ORIGINAL = Font(null)
        val PT_SERIF = Font("PT Serif")
        val ROBOTO = Font("Roboto")
        val SOURCE_SANS_PRO = Font("Source Sans Pro")
        val VOLLKORN = Font("Vollkorn")
        val OPEN_DYSLEXIC = Font("OpenDyslexic")
        val ACCESSIBLE_DFA = Font("AccessibleDfA")
        val IA_WRITER_DUOSPACE = Font("IA Writer Duospace")
    }

    class Coder(private val fonts: List<Font>) : SettingCoder<Font> {
        override fun decode(json: JsonElement): Font? {
            val name = json.jsonPrimitive.contentOrNull ?: return ORIGINAL
            return fonts.firstOrNull { it.name == name }
        }

        override fun encode(value: Font): JsonElement =
            JsonPrimitive(value.name)
    }
}
