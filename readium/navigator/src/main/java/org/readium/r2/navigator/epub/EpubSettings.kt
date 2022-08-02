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
import org.readium.r2.navigator.epub.css.TextAlign as CssTextAlign

@ExperimentalReadiumApi
data class EpubSettings(
    val columnCount: EnumSetting<ColumnCount>? = COLUMN_COUNT,
    val font: EnumSetting<Font> = FONT,
    val fontSize: PercentSetting = FONT_SIZE,
    val overflow: EnumSetting<Overflow> = OVERFLOW,
    val publisherStyles: ToggleSetting = PUBLISHER_STYLES,
    val textAlign: EnumSetting<TextAlign> = TEXT_ALIGN,
    val theme: EnumSetting<Theme> = THEME,
    val wordSpacing: PercentSetting = WORD_SPACING,
    val letterSpacing: PercentSetting = LETTER_SPACING,
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

        val LETTER_SPACING: PercentSetting = PercentSetting(
            key = Setting.LETTER_SPACING,
            value = 0.0,
            activator = RequiresPublisherStylesDisabled
        )

        val OVERFLOW: EnumSetting<Overflow> = EnumSetting(
            key = Setting.OVERFLOW,
            value = Overflow.PAGINATED,
            values = listOf(Overflow.PAGINATED, Overflow.SCROLLED),
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
            overflow = overflow.copyFirstValidValueFrom(preferences, defaults, fallback = OVERFLOW.value),
            publisherStyles = publisherStyles.copyFirstValidValueFrom(preferences, defaults),
            textAlign = textAlign.copyFirstValidValueFrom(preferences, defaults, fallback = TextAlign.START),
            theme = theme.copyFirstValidValueFrom(preferences, defaults, fallback = THEME.value),
            wordSpacing = wordSpacing.copyFirstValidValueFrom(preferences, defaults),
            letterSpacing = letterSpacing.copyFirstValidValueFrom(preferences, defaults),
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
                appearance = when (theme.value) {
                    Theme.LIGHT -> null
                    Theme.DARK -> Appearance.NIGHT
                    Theme.SEPIA -> Appearance.SEPIA
                },
                fontOverride = (font.value != Font.ORIGINAL),
                fontFamily = font.value.name?.let { listOf(it) },
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { Length.Relative.Percent(it) },
                advancedSettings = !publisherStyles.value,
                textAlign = when (textAlign.value) {
                    TextAlign.JUSTIFY -> CssTextAlign.JUSTIFY
                    TextAlign.LEFT -> CssTextAlign.LEFT
                    TextAlign.RIGHT -> CssTextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> CssTextAlign.START
                },
                wordSpacing = Length.Relative.Rem(wordSpacing.value),
                letterSpacing = Length.Relative.Rem(letterSpacing.value / 2),
            )
        )
    }
