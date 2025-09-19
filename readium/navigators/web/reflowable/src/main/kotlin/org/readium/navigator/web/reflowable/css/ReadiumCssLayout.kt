/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.css

import org.readium.navigator.web.internals.util.isCjk
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.RelativeUrl

/**
 * Readium CSS layout variant to use.
 *
 * See https://github.com/readium/readium-css/tree/master/css/dist
 */
internal data class ReadiumCssLayout(
    val language: Language? = null,
    val stylesheets: Stylesheets = Stylesheets.Default,
    val readingProgression: ReadingProgression = ReadingProgression.LTR,
) {
    /**
     * Readium CSS stylesheet variants.
     */
    enum class Stylesheets(val folder: RelativeUrl?, val htmlDir: HtmlDir) {
        /** Left to right */
        Default(null, HtmlDir.Ltr),

        /** Right to left */
        Rtl(RelativeUrl("rtl/")!!, HtmlDir.Rtl),

        /**
         * Asian language, laid out vertically.
         *
         * The HTML `dir` attribute must not be modified with vertical CJK:
         * https://github.com/readium/readium-css/tree/master/css/dist#vertical
         */
        CjkVertical(RelativeUrl("cjk-vertical/")!!, HtmlDir.Unspecified),

        /** Asian language, laid out horizontally */
        CjkHorizontal(RelativeUrl("cjk-horizontal/")!!, HtmlDir.Ltr),
    }

    enum class HtmlDir {
        Unspecified,
        Ltr,
        Rtl,
    }

    companion object {

        @OptIn(ExperimentalReadiumApi::class)
        internal fun from(settingsValues: ReflowableWebSettings): ReadiumCssLayout {
            val stylesheets = when {
                settingsValues.verticalText -> Stylesheets.CjkVertical
                settingsValues.language?.isCjk == true -> Stylesheets.CjkHorizontal
                settingsValues.readingProgression == ReadingProgression.RTL -> Stylesheets.Rtl
                else -> Stylesheets.Default
            }

            return ReadiumCssLayout(
                language = settingsValues.language,
                readingProgression = settingsValues.readingProgression,
                stylesheets = stylesheets
            )
        }
    }
}
