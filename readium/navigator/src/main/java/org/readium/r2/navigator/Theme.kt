/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.util.MapWithDefaultCompanion
import org.readium.r2.shared.util.ValueCoder

/**
 * Navigator appearance.
 */
enum class Theme(val value: String) {
    Light("light"),
    Dark("dark"),
    Sepia("sepia");

    companion object : MapWithDefaultCompanion<Theme>(values(), Theme::value, Light)
}

enum class ColumnCount(val value: String) {
    Auto("auto"),
    One("1"),
    Two("2");

    companion object : MapWithDefaultCompanion<ColumnCount>(values(), ColumnCount::value, Auto)
}

data class Font(val id: String, val name: String? = null) {
    companion object {
        val ORIGINAL = Font(id = "original")
        val PT_SERIF = Font(id = "pt-serif", name = "PT Serif")
        val ROBOTO = Font(id = "roboto", name = "Roboto")
        val SOURCE_SANS_PRO = Font(id = "source-sans-pro", name = "Source Sans Pro")
        val VOLLKORN = Font(id = "vollkorn", name = "Vollkorn")
        val OPEN_DYSLEXIC = Font(id = "opendyslexic", name = "OpenDyslexic")
        val ACCESSIBLE_DFA = Font(id = "accessible-dfa", name = "AccessibleDfA")
        val IA_WRITER_DUOSPACE = Font(id = "ia-writer-duospace", name = "IA Writer Duospace")
    }

    class Coder(private val fonts: List<Font>) : ValueCoder<Font?, String?> {
        override fun encode(value: Font?): String? =
            value?.id

        override fun decode(rawValue: Any): Font? {
            val id = rawValue as? String ?: return null
            return fonts.firstOrNull { it.id == id }
        }
    }
}
