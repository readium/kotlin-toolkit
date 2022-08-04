/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.navigator.settings.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Declares a font available with Readium CSS.
 */
@ExperimentalReadiumApi
data class FontFamilyDeclaration(
    val fontFamily: FontFamily,
    val source: FontFamilySource
)

/**
 * Source for typefaces.
 */
sealed class FontFamilySource {
    /**
     * A typeface embedded with Readium CSS, under the fonts/ directory.
     */
    object ReadiumCss : FontFamilySource()

    /**
     * A typeface downloaded from Google Fonts.
     *
     * See https://fonts.google.com/ for the list of available fonts.
     */
    object GoogleFonts : FontFamilySource()
}

/**
 * Creates a font family declaration for the [FontFamily] receiver from the given [source].
 */
@ExperimentalReadiumApi
fun FontFamily.from(source: FontFamilySource): FontFamilyDeclaration =
    FontFamilyDeclaration(this, source)