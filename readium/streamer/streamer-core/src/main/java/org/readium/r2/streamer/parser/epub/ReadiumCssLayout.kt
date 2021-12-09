/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

internal enum class ReadiumCssLayout(val cssId: String) {
    // Right to left
    RTL("rtl"),
    // Left to right
    LTR("ltr"),
    // Asian language, vertically laid out
    CJK_VERTICAL("cjk-vertical"),
    // Asian language, horizontally laid out
    CJK_HORIZONTAL("cjk-horizontal");

    val readiumCSSPath: String get() = when (this) {
        LTR -> ""
        RTL -> "rtl/"
        CJK_VERTICAL -> "cjk-vertical/"
        CJK_HORIZONTAL -> "cjk-horizontal/"
    }

    companion object {

        operator fun invoke(metadata: Metadata): ReadiumCssLayout =
            invoke(languages = metadata.languages, readingProgression = metadata.effectiveReadingProgression)

        /**
         * Determines the [ReadiumCssLayout] for the given BCP 47 language codes and
         * [readingProgression].
         * Defaults to [LTR].
         */
        operator fun invoke(languages: List<String>, readingProgression: ReadingProgression): ReadiumCssLayout {
            val isCjk: Boolean =
                if (languages.size == 1) {
                    val language = languages[0].split("-")[0]  // Remove region
                    listOf("zh", "ja", "ko").contains(language)
                } else {
                    false
                }

            return when (readingProgression) {
                ReadingProgression.RTL, ReadingProgression.BTT ->
                    if (isCjk) CJK_VERTICAL
                    else RTL

                ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
                    if (isCjk) CJK_HORIZONTAL
                    else LTR
            }
        }

    }
}
