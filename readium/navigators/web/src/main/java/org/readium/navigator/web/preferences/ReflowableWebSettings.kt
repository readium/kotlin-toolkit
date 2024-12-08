/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import org.readium.navigator.web.css.Appearance
import org.readium.navigator.web.css.Color as CssColor
import org.readium.navigator.web.css.FontWeight
import org.readium.navigator.web.css.Hyphens
import org.readium.navigator.web.css.Layout
import org.readium.navigator.web.css.Length
import org.readium.navigator.web.css.Ligatures
import org.readium.navigator.web.css.ReadiumCss
import org.readium.navigator.web.css.TextAlign as CssTextAlign
import org.readium.navigator.web.css.View
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Either.Companion.invoke
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
public data class ReflowableWebSettings(
    val backgroundColor: Color?,
    val columnCount: Int,
    val fontFamily: FontFamily?,
    val fontSize: Double,
    val fontWeight: Double?,
    val hyphens: Boolean?,
    val imageFilter: ImageFilter?,
    val language: Language?,
    val letterSpacing: Double?,
    val ligatures: Boolean?,
    val lineHeight: Double?,
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
    val verticalText: Boolean,
    val wordSpacing: Double?,
) : Configurable.Settings

@OptIn(ExperimentalReadiumApi::class)
internal fun ReadiumCss.update(settings: ReflowableWebSettings, useReadiumCssFontSize: Boolean): ReadiumCss {
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
                colCount = columnCount,
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
