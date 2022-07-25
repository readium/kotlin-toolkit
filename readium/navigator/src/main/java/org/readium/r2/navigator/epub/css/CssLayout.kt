/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

internal enum class CssLayout {
    /** Right to left */
    Rtl,
    /** Left to right */
    Ltr,
    /** Asian language, laid out vertically */
    CjkVertical,
    /** Asian language, laid out horizontally */
    CjkHorizontal;

    val readiumCSSPath: String get() = when (this) {
        Ltr -> ""
        Rtl -> "rtl/"
        CjkVertical -> "cjk-vertical/"
        CjkHorizontal -> "cjk-horizontal/"
    }

    companion object {

        operator fun invoke(metadata: Metadata): CssLayout =
            invoke(
                languages = metadata.languages,
                readingProgression = metadata.effectiveReadingProgression
            )

        /**
         * Determines the [CssLayout] for the given BCP 47 language codes and
         * [readingProgression].
         * Defaults to [Ltr].
         */
        operator fun invoke(languages: List<String>, readingProgression: ReadingProgression): CssLayout {
            val isCjk: Boolean =
                if (languages.size == 1) {
                    val language = languages[0].split("-")[0]  // Remove region
                    listOf("zh", "ja", "ko").contains(language)
                } else {
                    false
                }

            return when (readingProgression) {
                ReadingProgression.RTL, ReadingProgression.BTT ->
                    if (isCjk) CjkVertical
                    else Rtl

                ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
                    if (isCjk) CjkHorizontal
                    else Ltr
            }
        }

    }
}
