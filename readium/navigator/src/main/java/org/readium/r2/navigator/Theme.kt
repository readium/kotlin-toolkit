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

@JvmInline
value class Font(val name: String?) {
    companion object : ValueCoder<Font?, String?> {
        val ORIGINAL = Font(null)
        val PT_SERIF = Font("PT Serif")
        val ROBOTO = Font("Roboto")
        val SOURCE_SANS_PRO = Font("Source Sans Pro")
        val VOLLKORN = Font("Vollkorn")
        val OPEN_DYSLEXIC = Font("OpenDyslexic")
        val ACCESSIBLE_DFA = Font("AccessibleDfA")
        val IA_WRITER_DUOSPACE = Font("IA Writer Duospace")

        override fun encode(value: Font?): String? =
            value?.name

        override fun decode(rawValue: Any): Font? =
            (rawValue as? String)?.let { Font(it) }
    }
}
