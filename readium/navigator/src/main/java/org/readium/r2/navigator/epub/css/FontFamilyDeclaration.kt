/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Declares an additional font available with Readium CSS.
 */
@ExperimentalReadiumApi
sealed class FontFamilyDeclaration {

    abstract val fontFamily: FontFamily
}

/**
 * A typeface embedded in the app assets.
 *
 * @param path Path to the font file, relative to the assets folder.
 */
@ExperimentalReadiumApi
data class FontAsset(override val fontFamily: FontFamily, val path: String) : FontFamilyDeclaration()

/**
 * A typeface hosted by Google Fonts.
 *
 * Warning: the navigator requires an Internet connection to use these fonts.
 *
 * See https://fonts.google.com/ for the list of available fonts.
 */
@ExperimentalReadiumApi
data class GoogleFont(override val fontFamily: FontFamily) : FontFamilyDeclaration()
