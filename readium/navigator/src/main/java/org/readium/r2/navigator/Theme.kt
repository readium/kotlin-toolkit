/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.readium.r2.navigator.settings.SettingCoder
import org.readium.r2.shared.util.ValueCoder

/**
 * Navigator appearance.
 */
@Serializable
enum class Theme {
    @SerialName("light") Light,
    @SerialName("dark") Dark,
    @SerialName("sepia") Sepia;
}

@Serializable
enum class ColumnCount {
    @SerialName("auto") Auto,
    @SerialName("1") One,
    @SerialName("2") Two;
}

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
