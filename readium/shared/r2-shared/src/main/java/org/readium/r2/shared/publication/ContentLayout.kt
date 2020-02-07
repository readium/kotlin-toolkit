/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.readium.r2.shared.util.KeyMapperWithDefault
import java.util.*

/**
 * The [ContentLayout] defines how a [Publication] should be laid out, based on the declared
 * [ReadingProgression] and its language.
 *
 * Since [ReadingProgression] might be declared as [AUTO] or undefined in the Readium Web
 * Publication Manifest, [ContentLayout] is useful to determine the actual layout.
 *
 * @param key Identifier for this layout style, shared with ReadiumCSS.
 */
@Parcelize
enum class ContentLayout(val key: String) : Parcelable {
    // Right to left
    RTL("rtl"),
    // Left to right
    LTR("ltr"),
    // Asian language, vertically laid out
    CJK_VERTICAL("cjk-vertical"),
    // Asian language, horizontally laid out
    CJK_HORIZONTAL("cjk-horizontal");

    /**
     * Returns the matching [ReadingProgression].
     */
    val readingProgression: ReadingProgression
        get() = when(this) {
            RTL, CJK_VERTICAL -> ReadingProgression.RTL
            LTR, CJK_HORIZONTAL -> ReadingProgression.LTR
        }

    companion object : KeyMapperWithDefault<String, ContentLayout>(values(), ContentLayout::key, LTR) {

        /**
         * Determines the [ContentLayout] for the given BCP 47 language code and
         * [readingProgression].
         * Defaults to [LTR].
         */
        fun from(language: String, readingProgression: ReadingProgression? = null): ContentLayout {
            // Removes the region from the BCP 47 tag
            @Suppress("NAME_SHADOWING")
            val language = language.split("-").firstOrNull() ?: language

            val isRTLProgression = (readingProgression == ReadingProgression.RTL)

            return when (language.toLowerCase(Locale.ROOT)) {
                "ar", "fa", "he" -> RTL
                "zh", "ja", "ko" -> if (isRTLProgression) CJK_VERTICAL else CJK_HORIZONTAL
                else -> if (isRTLProgression) RTL else LTR
            }
        }

        @Deprecated("Renamed to [RTL]", ReplaceWith("ContentLayout.RTL"))
        val rtl: ContentLayout = RTL
        @Deprecated("Renamed to [LTR]", ReplaceWith("ContentLayout.LTR"))
        val ltr: ContentLayout = LTR
        @Deprecated("Renamed to [CJK_VERTICAL]", ReplaceWith("ContentLayout.CJK_VERTICAL"))
        val cjkv: ContentLayout = CJK_VERTICAL
        @Deprecated("Renamed to [CJK_HORIZONTAL]", ReplaceWith("ContentLayout.CJK_HORIZONTAL"))
        val cjkh: ContentLayout = CJK_HORIZONTAL

    }

}
