/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.Appearance
import org.readium.r2.navigator.epub.css.ColCount
import org.readium.r2.navigator.epub.css.Color as CssColor
import org.readium.r2.navigator.epub.css.FontWeight
import org.readium.r2.navigator.epub.css.Hyphens
import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.epub.css.Length
import org.readium.r2.navigator.epub.css.Ligatures
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign
import org.readium.r2.navigator.epub.css.View
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language

/**
 * EPUB navigator settings values.
 *
 * @see EpubPreferences
 */
public data class EpubSettings @ExperimentalReadiumApi constructor(
    val backgroundColor: Color?,
    val columnCount: ColumnCount,
    val fontFamily: FontFamily?,
    val fontSize: Double,
    val fontWeight: Double?,
    val hyphens: Boolean?,
    val imageFilter: ImageFilter?,
    val language: Language?,
    val letterSpacing: Double?,
    val ligatures: Boolean?,
    val lineHeight: Double?,
    val pageMargins: Double,
    val paragraphIndent: Double?,
    val paragraphSpacing: Double?,
    val publisherStyles: Boolean,
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val spread: Spread,
    val textAlign: TextAlign?,
    val textColor: Color?,
    val textNormalization: Boolean,
    val theme: Theme,
    val typeScale: Double?,
    val verticalText: Boolean,
    val wordSpacing: Double?,
) : Configurable.Settings

@OptIn(ExperimentalReadiumApi::class)
internal fun ReadiumCss.update(settings: EpubSettings, useReadiumCssFontSize: Boolean): ReadiumCss {
    fun resolveFontStack(fontFamily: String): List<String> = buildList {
        add(fontFamily)

        val alternates = fontFamilyDeclarations
            .firstOrNull { it.fontFamily == fontFamily }
            ?.alternates
            ?: emptyList()

        addAll(alternates.flatMap(::resolveFontStack))
    }

    fun FontFamily.toCss(): List<String> =
        resolveFontStack(name)

    fun Color.toCss(): CssColor =
        CssColor.Int(int)

    return with(settings) {
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
                textColor = textColor?.toCss(),
                backgroundColor = backgroundColor?.toCss(),
                fontOverride = (fontFamily != null || textNormalization),
                fontFamily = fontFamily?.toCss(),
                fontSize = if (useReadiumCssFontSize) {
                    Length.Percent(fontSize)
                } else {
                    null
                },
                advancedSettings = !publisherStyles,
                typeScale = typeScale,
                textAlign = when (textAlign) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                    null -> null
                },
                lineHeight = lineHeight?.let { Either(it) },
                paraSpacing = paragraphSpacing?.let { Length.Rem(it) },
                paraIndent = paragraphIndent?.let { Length.Rem(it) },
                wordSpacing = wordSpacing?.let { Length.Rem(it) },
                letterSpacing = letterSpacing?.let { Length.Rem(it / 2) },
                bodyHyphens = hyphens?.let { if (it) Hyphens.AUTO else Hyphens.NONE },
                ligatures = ligatures?.let { if (it) Ligatures.COMMON else Ligatures.NONE },
                a11yNormalize = textNormalization,
                overrides = mapOf(
                    "font-weight" to
                        if (fontWeight != null) {
                            (FontWeight.NORMAL.value * fontWeight).toInt().coerceIn(1, 1000).toString()
                        } else {
                            ""
                        }
                )
            )
        )
    }
}
