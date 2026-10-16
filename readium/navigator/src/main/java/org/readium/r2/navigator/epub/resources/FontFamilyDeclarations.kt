/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.resources

import android.content.Context
import androidx.annotation.FontRes
import kotlin.collections.plus
import org.readium.r2.navigator.epub.EpubNavigatorFragment.Configuration
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.epub.css.FontWeight
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

/**
 * Declares a navigator font family from an Android `res/font` resource.
 *
 * This helper supports both direct font files and XML font-family resources. Each discovered
 * variant is translated into a `@font-face` rule and the generated resource URLs are appended to
 * [Configuration.servedAssets] so the WebView can resolve them at runtime.
 */
@ExperimentalReadiumApi
public fun Configuration.addFontFamilyDeclaration(
    fontFamily: FontFamily,
    context: Context,
    @FontRes fontFamilyResId: Int,
    preload: Boolean = false,
    alternates: List<FontFamily> = emptyList(),
) {
    val resourceAssets = arrayListOf<String>()

    addFontFamilyDeclaration(
        fontFamily = fontFamily,
        alternates = alternates
    ) {
        forEachFontInResource(
            context = context,
            fontResId = fontFamilyResId
        ) builder@{ fontResId: Int, fontWeight: FontWeight, fontStyle: FontStyle ->
            val href = context.resources.resourceUrl(fontResId)

            if (href == null) {
                Timber.d("Can't resolve URL for font resource id")
                return@builder
            }

            resourceAssets += href.toString()

            addFontFace {
                addSource(
                    href = href,
                    preload = preload,
                )

                setFontWeight(fontWeight)
                setFontStyle(fontStyle)
            }
        }
    }

    if (resourceAssets.isNotEmpty()) {
        servedAssets = servedAssets + resourceAssets
    }
}
