/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.settings.*
import org.readium.r2.navigator.settings.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.shared.util.Either
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign

@ExperimentalReadiumApi
data class EpubSettings(
    val columnCount: EnumSetting<ColumnCount>? = COLUMN_COUNT,
    val font: EnumSetting<Font> = FONT,
    val fontSize: PercentSetting = FONT_SIZE,
    val hyphens: ToggleSetting = HYPHENS,
    val letterSpacing: PercentSetting = LETTER_SPACING,
    val ligatures: ToggleSetting = LIGATURES,
    val lineHeight: RangeSetting<Double> = LINE_HEIGHT,
    val normalizedText: ToggleSetting = NORMALIZED_TEXT,
    val overflow: EnumSetting<Overflow> = OVERFLOW,
    val pageMargins: RangeSetting<Double> = PAGE_MARGINS,
    val paragraphIndent: PercentSetting = PARAGRAPH_INDENT,
    val paragraphSpacing: PercentSetting = PARAGRAPH_SPACING,
    val publisherStyles: ToggleSetting = PUBLISHER_STYLES,
    val textAlign: EnumSetting<TextAlign> = TEXT_ALIGN,
    val theme: EnumSetting<Theme> = THEME,
    val typeScale: RangeSetting<Double> = TYPE_SCALE,
    val wordSpacing: PercentSetting = WORD_SPACING,
) : Configurable.Settings {
    constructor(fonts: List<Font>) : this(
        font = FONT.copy(
            coder = Font.Coder(listOf(Font.ORIGINAL) + fonts),
            values = listOf(Font.ORIGINAL) + fonts
        )
    )

    companion object {
        val COLUMN_COUNT: EnumSetting<ColumnCount> = EnumSetting(
            key = Setting.COLUMN_COUNT,
            value = ColumnCount.AUTO,
            values = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
        )

        val FONT: EnumSetting<Font> = EnumSetting(
            key = Setting.FONT,
            coder = Font.Coder(listOf(Font.ORIGINAL)),
            value = Font.ORIGINAL,
            values = listOf(Font.ORIGINAL),
            label = { it.name }
        )

        val FONT_SIZE: PercentSetting = PercentSetting(
            key = Setting.FONT_SIZE,
            value = 1.0,
            range = 0.4..5.0
        )

        val HYPHENS: ToggleSetting = ToggleSetting(
            key = Setting.HYPHENS,
            value = true,
            activator = RequiresPublisherStylesDisabled
        )

        val LETTER_SPACING: PercentSetting = PercentSetting(
            key = Setting.LETTER_SPACING,
            value = 0.0,
            activator = RequiresPublisherStylesDisabled
        )

        val LIGATURES: ToggleSetting = ToggleSetting(
            key = Setting.LIGATURES,
            value = true,
            activator = RequiresPublisherStylesDisabled
        )

        val LINE_HEIGHT: RangeSetting<Double> = RangeSetting(
            key = Setting.LINE_HEIGHT,
            value = 1.2,
            range = 1.0..2.0,
            activator = RequiresPublisherStylesDisabled
        )

        val NORMALIZED_TEXT: ToggleSetting = ToggleSetting(
            key = Setting.NORMALIZED_TEXT,
            value = false,
        )

        val OVERFLOW: EnumSetting<Overflow> = EnumSetting(
            key = Setting.OVERFLOW,
            value = Overflow.PAGINATED,
            values = listOf(Overflow.PAGINATED, Overflow.SCROLLED)
        )

        val PAGE_MARGINS: RangeSetting<Double> = RangeSetting(
            key = Setting.PAGE_MARGINS,
            value = 1.0,
            range = 0.5..2.0
        )

        val PARAGRAPH_INDENT: PercentSetting = PercentSetting(
            key = Setting.PARAGRAPH_INDENT,
            value = 0.0,
            range = 0.0..3.0,
            suggestedIncrement = 0.2,
            activator = RequiresPublisherStylesDisabled
        )

        val PARAGRAPH_SPACING: PercentSetting = PercentSetting(
            key = Setting.PARAGRAPH_SPACING,
            value = 0.0,
            range = 0.0..2.0,
            activator = RequiresPublisherStylesDisabled
        )

        val PUBLISHER_STYLES: ToggleSetting = ToggleSetting(
            key = Setting.PUBLISHER_STYLES,
            value = true,
        )

        val TEXT_ALIGN: EnumSetting<TextAlign> = EnumSetting(
            key = Setting.TEXT_ALIGN,
            value = TextAlign.START,
            values = listOf(TextAlign.START, TextAlign.LEFT, TextAlign.RIGHT, TextAlign.JUSTIFY),
            activator = RequiresPublisherStylesDisabled
        )

        val THEME: EnumSetting<Theme> = EnumSetting(
            key = Setting.THEME,
            value = Theme.LIGHT,
            values = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
        )

        // https://readium.org/readium-css/docs/CSS19-api.html#typography
        val TYPE_SCALE: RangeSetting<Double> = RangeSetting(
            key = Setting.TYPE_SCALE,
            value = 1.2,
            range = 1.0..2.0,
            suggestedSteps = listOf(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
            activator = RequiresPublisherStylesDisabled
        )

        val WORD_SPACING: PercentSetting = PercentSetting(
            key = Setting.WORD_SPACING,
            value = 0.0,
            activator = RequiresPublisherStylesDisabled
        )

        private object RequiresPublisherStylesDisabled : SettingActivator {
            override fun isActiveWithPreferences(preferences: Preferences): Boolean =
                preferences[PUBLISHER_STYLES] == false

            override fun activateInPreferences(preferences: MutablePreferences) {
                preferences[PUBLISHER_STYLES] = false
            }
        }
    }

    internal fun update(preferences: Preferences, defaults: Preferences = Preferences()): EpubSettings =
        copy(
            columnCount = if (preferences[overflow] == Overflow.SCROLLED) null
                else (columnCount ?: COLUMN_COUNT).copyFirstValidValueFrom(preferences, defaults),
            font = font.copyFirstValidValueFrom(preferences, defaults, fallback = FONT.value),
            fontSize = fontSize.copyFirstValidValueFrom(preferences, defaults),
            hyphens = hyphens.copyFirstValidValueFrom(preferences, defaults),
            letterSpacing = letterSpacing.copyFirstValidValueFrom(preferences, defaults),
            ligatures = ligatures.copyFirstValidValueFrom(preferences, defaults),
            lineHeight = lineHeight.copyFirstValidValueFrom(preferences, defaults),
            normalizedText = normalizedText.copyFirstValidValueFrom(preferences, defaults),
            overflow = overflow.copyFirstValidValueFrom(preferences, defaults, fallback = OVERFLOW.value),
            pageMargins = pageMargins.copyFirstValidValueFrom(preferences, defaults),
            paragraphIndent = paragraphIndent.copyFirstValidValueFrom(preferences, defaults),
            paragraphSpacing = paragraphSpacing.copyFirstValidValueFrom(preferences, defaults),
            publisherStyles = publisherStyles.copyFirstValidValueFrom(preferences, defaults),
            textAlign = textAlign.copyFirstValidValueFrom(preferences, defaults, fallback = TextAlign.START),
            theme = theme.copyFirstValidValueFrom(preferences, defaults, fallback = THEME.value),
            typeScale = typeScale.copyFirstValidValueFrom(preferences, defaults),
            wordSpacing = wordSpacing.copyFirstValidValueFrom(preferences, defaults),
        )
}

@ExperimentalReadiumApi
fun ReadiumCss.update(settings: EpubSettings): ReadiumCss =
    with(settings) {
        copy(
            userProperties = userProperties.copy(
                view = when (overflow.value) {
                    Overflow.AUTO -> null
                    Overflow.PAGINATED -> View.PAGED
                    Overflow.SCROLLED -> View.SCROLL
                },
                colCount = when (columnCount?.value) {
                    ColumnCount.ONE -> ColCount.ONE
                    ColumnCount.TWO -> ColCount.TWO
                    else -> ColCount.AUTO
                },
                pageMargins = pageMargins.value,
                appearance = when (theme.value) {
                    Theme.LIGHT -> null
                    Theme.DARK -> Appearance.NIGHT
                    Theme.SEPIA -> Appearance.SEPIA
                },
                fontOverride = (font.value != Font.ORIGINAL || normalizedText.value),
                fontFamily = font.value.name?.let { listOf(it) },
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { Length.Relative.Percent(it) },
                advancedSettings = !publisherStyles.value,
                typeScale = typeScale.value,
                textAlign = when (textAlign.value) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                },
                lineHeight = Either(lineHeight.value),
                paraSpacing = Length.Relative.Rem(paragraphSpacing.value),
                paraIndent = Length.Relative.Rem(paragraphIndent.value),
                wordSpacing = Length.Relative.Rem(wordSpacing.value),
                letterSpacing = Length.Relative.Rem(letterSpacing.value / 2),
                bodyHyphens = if (hyphens.value) Hyphens.AUTO else Hyphens.NONE,
                ligatures = if (ligatures.value) Ligatures.COMMON else Ligatures.NONE,
                a11yNormalize = normalizedText.value,
            )
        )
    }
