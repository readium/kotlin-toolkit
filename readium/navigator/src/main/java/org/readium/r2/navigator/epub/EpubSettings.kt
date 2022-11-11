/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign
import org.readium.r2.navigator.epub.css.Color as CssColor
import org.readium.r2.navigator.preferences.*
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language

/**
 * EPUB navigator settings values.
 *
 * @see EpubPreferences
 */
@ExperimentalReadiumApi
data class EpubSettings(
    val backgroundColor: Color,
    val columnCount: ColumnCount,
    val fontFamily: FontFamily?,
    val fontSize: Double,
    val hyphens: Boolean,
    val imageFilter: ImageFilter,
    val language: Language?,
    val letterSpacing: Double,
    val ligatures: Boolean,
    val lineHeight: Double,
    val pageMargins: Double,
    val paragraphIndent: Double,
    val paragraphSpacing: Double,
    val publisherStyles: Boolean,
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val spread: Spread,
    val textAlign: TextAlign,
    val textColor: Color,
    val textNormalization: TextNormalization,
    val theme: Theme,
    val typeScale: Double,
    val verticalText: Boolean,
    val wordSpacing: Double
) : Configurable.Settings

@OptIn(ExperimentalReadiumApi::class)
internal fun ReadiumCss.update(settings: EpubSettings): ReadiumCss {

    fun FontFamily.toCss(): List<String> = buildList {
        add(name)
        val alternateChain = alternate?.toCss()
        alternateChain?.let {  addAll(it) }
    }

    fun Color.toCss(): CssColor =
        CssColor.Int(int)

    return with (settings) {
        copy(
            layout = Layout.from(settings),
            userProperties = userProperties.copy(
                view = when (scroll) {
                    false -> View.PAGED
                    true -> View.SCROLL
                },
                colCount = when (columnCount) {
                    ColumnCount.ONE -> ColCount.ONE
                    ColumnCount.TWO -> ColCount.TWO
                    else -> ColCount.AUTO
                },
                pageMargins = pageMargins,
                appearance = when (theme) {
                    Theme.LIGHT -> null
                    Theme.DARK -> Appearance.NIGHT
                    Theme.SEPIA -> Appearance.SEPIA
                },
                darkenImages = imageFilter == ImageFilter.DARKEN,
                invertImages = imageFilter == ImageFilter.INVERT,
                textColor = textColor.toCss(),
                backgroundColor = backgroundColor.toCss(),
                fontOverride = (fontFamily != null || (textNormalization == TextNormalization.ACCESSIBILITY)),
                fontFamily = fontFamily?.toCss(),
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { Length.Percent(it) },
                advancedSettings = !publisherStyles,
                typeScale = typeScale,
                textAlign = when (textAlign) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                },
                lineHeight = Either(lineHeight),
                paraSpacing = Length.Rem(paragraphSpacing),
                paraIndent = Length.Rem(paragraphIndent),
                wordSpacing = Length.Rem(wordSpacing),
                letterSpacing = Length.Rem(letterSpacing / 2),
                bodyHyphens = if (hyphens) Hyphens.AUTO else Hyphens.NONE,
                ligatures = if (ligatures) Ligatures.COMMON else Ligatures.NONE,
                a11yNormalize = textNormalization == TextNormalization.ACCESSIBILITY,
                overrides = mapOf(
                    "font-weight" to if (textNormalization == TextNormalization.BOLD) "bold" else null
                )
            )
        )
    }
}
