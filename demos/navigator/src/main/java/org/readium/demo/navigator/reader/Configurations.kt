/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.reader

import kotlin.collections.set
import kotlinx.collections.immutable.persistentListOf
import org.readium.demo.navigator.decorations.DecorationStyleAnnotationMark
import org.readium.demo.navigator.decorations.DecorationStylePageNumber
import org.readium.demo.navigator.decorations.annotationMarkTemplate
import org.readium.demo.navigator.decorations.pageNumberTemplate
import org.readium.navigator.web.common.FontFamilyDeclarations
import org.readium.navigator.web.common.FontStyle
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.navigator.web.fixedlayout.FixedWebConfiguration
import org.readium.navigator.web.reflowable.ReflowableWebConfiguration
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi

val FontFamily.Companion.LITERATA: FontFamily get() = FontFamily("Literata")

val reflowableConfig: ReflowableWebConfiguration =
    ReflowableWebConfiguration(
        // App assets which will be accessible from the EPUB resources.
        // You can use simple glob patterns, such as "images/.*" to allow several
        // assets in one go.
        servedAssets = persistentListOf(
            // For the custom font Literata.
            "fonts/.*",
            // Icon for the annotation side mark, see [annotationMarkTemplate].
            "annotation-icon.svg"
        ),
        // Register the templates for our custom decoration styles.
        decorationTemplates = WebDecorationTemplates {
            set(DecorationStyleAnnotationMark::class, annotationMarkTemplate())
            set(DecorationStylePageNumber::class, pageNumberTemplate())
        },
        fontFamilyDeclarations = FontFamilyDeclarations {
            // Declare a custom font family for reflowable EPUBs.
            addFontFamilyDeclaration(FontFamily.LITERATA) {
                addFontFace {
                    addSource("fonts/Literata-VariableFont_opsz,wght.ttf")
                    setFontStyle(FontStyle.NORMAL)
                    // Literata is a variable font family, so we can provide a font weight range.
                    setFontWeight(200..900)
                }
                addFontFace {
                    addSource("fonts/Literata-Italic-VariableFont_opsz,wght.ttf")
                    setFontStyle(FontStyle.ITALIC)
                    setFontWeight(200..900)
                }
            }
        }

    )

val fixedConfig = FixedWebConfiguration(
    // App assets which will be accessible from the EPUB resources.
    // You can use simple glob patterns, such as "images/.*" to allow several
    // assets in one go.
    servedAssets = persistentListOf(
        // Icon for the annotation side mark, see [annotationMarkTemplate].
        "annotation-icon.svg"
    ),
    // Register the templates for our custom decoration styles.
    decorationTemplates = WebDecorationTemplates {
        set(DecorationStyleAnnotationMark::class, annotationMarkTemplate())
    }
)
