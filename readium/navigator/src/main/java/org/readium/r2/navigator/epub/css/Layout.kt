/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

/**
 * Readium CSS layout variant to use.
 *
 * See https://github.com/readium/readium-css/tree/master/css/dist
 */
data class Layout(
    val stylesheets: Stylesheets,
    val readingProgression: ReadingProgression,
) {
    companion object {
        operator fun invoke(language: Language?, hasMultipleLanguages: Boolean, readingProgression: ReadingProgression): Layout {
            // https://github.com/readium/readium-css/blob/master/docs/CSS16-internationalization.md#missing-page-progression-direction
            var rp = when {
                readingProgression != ReadingProgression.AUTO ->
                    readingProgression

                !hasMultipleLanguages && language != null && language.isRtl ->
                    ReadingProgression.RTL

                else ->
                    ReadingProgression.LTR
            }

            val stylesheets =
                if (language != null && language.isCjk) {
                    if (rp == ReadingProgression.RTL)
                        Stylesheets.CjkVertical
                    else
                        Stylesheets.CjkHorizontal
                } else if (rp == ReadingProgression.RTL) {
                    Stylesheets.Rtl
                } else {
                    Stylesheets.Default
                }

            if (stylesheets == Stylesheets.CjkVertical) {
                rp = ReadingProgression.TTB
            }

            return Layout(stylesheets, rp)
        }
    }

    /**
     * Readium CSS stylesheet variants.
     */
    enum class Stylesheets(val folder: String?, val htmlDir: HtmlDir) {
        /** Left to right */
        Default(null, HtmlDir.Ltr),
        /** Right to left */
        Rtl("rtl", HtmlDir.Rtl),
        /**
         * Asian language, laid out vertically.
         *
         * The HTML `dir` attribute must not be modified with vertical CJK:
         * https://github.com/readium/readium-css/tree/master/css/dist#vertical
         */
        CjkVertical("cjk-vertical", HtmlDir.Unspecified),
        /** Asian language, laid out horizontally */
        CjkHorizontal("cjk-horizontal", HtmlDir.Ltr);
    }

    enum class HtmlDir {
        Unspecified, Ltr, Rtl;
    }
}

private val Language.isRtl: Boolean get() {
    val c = code.lowercase()
    return c == "ar"
        || c == "fa"
        || c == "he"
        || c == "zh-hant"
        || c == "zh-tw"
}

private val Language.isCjk: Boolean get() {
    val c = code.lowercase()
    return c == "ja"
        || c == "ko"
        || removeRegion().code.lowercase() == "zh"
}
