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
internal data class Layout(
    val language: Language? = null,
    val stylesheets: Stylesheets = Stylesheets.Default,
    val readingProgression: ReadingProgression = ReadingProgression.LTR,
) {
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
