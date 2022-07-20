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
@Parcelize
enum class Theme(val value: String) : Parcelable {
    LIGHT("light"),
    DARK("dark"),
    SEPIA("sepia");

    companion object : MapWithDefaultCompanion<String, Theme>(values(), Theme::value, LIGHT)
}

@Parcelize
@JvmInline
value class Font(val name: String?) : Parcelable {
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

        override fun decode(rawValue: String?): Font? =
            rawValue?.let { Font(it) }
    }
}
