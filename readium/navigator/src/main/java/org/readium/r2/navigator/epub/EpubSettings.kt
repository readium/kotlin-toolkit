/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import kotlinx.coroutines.flow.update
import org.readium.r2.navigator.ColumnCount
import org.readium.r2.navigator.Font
import org.readium.r2.navigator.Theme
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation.Overflow

@ExperimentalReadiumApi
data class EpubSettings(
    val columnCount: EnumSetting<ColumnCount>? = COLUMN_COUNT,
    val font: EnumSetting<Font> = FONT,
    val fontSize: PercentSetting = FONT_SIZE,
    val overflow: EnumSetting<Overflow> = OVERFLOW,
    val publisherStyles: ToggleSetting = PUBLISHER_STYLES,
    val theme: EnumSetting<Theme> = THEME,
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
            coder = ColumnCount,
            value = ColumnCount.Auto,
            values = listOf(ColumnCount.Auto, ColumnCount.One, ColumnCount.Two),
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

        val OVERFLOW: EnumSetting<Overflow> = EnumSetting(
            key = Setting.OVERFLOW,
            coder = Overflow,
            value = Overflow.PAGINATED,
            values = listOf(Overflow.PAGINATED, Overflow.SCROLLED),
        )

        val PUBLISHER_STYLES: ToggleSetting = ToggleSetting(
            key = Setting.PUBLISHER_STYLES,
            value = true,
        )

        val THEME: EnumSetting<Theme> = EnumSetting(
            key = Setting.THEME,
            coder = Theme,
            value = Theme.Light,
            values = listOf(Theme.Light, Theme.Dark, Theme.Sepia)
        )

        val WORD_SPACING: PercentSetting = PercentSetting(
            key = Setting.WORD_SPACING,
            value = 0.0,
            activator = object : SettingActivator {
                override fun isActiveWithPreferences(preferences: Preferences): Boolean =
                    preferences[PUBLISHER_STYLES] == false

                override fun activateInPreferences(preferences: MutablePreferences) {
                    preferences[PUBLISHER_STYLES] = false
                }
            }
        )
    }

    internal fun update(preferences: Preferences, defaults: Preferences): EpubSettings =
        copy(
            columnCount = if (preferences[overflow] == Overflow.SCROLLED) null
                else (columnCount ?: COLUMN_COUNT).copyFirstValidValueFrom(preferences, defaults),
            font = font.copyFirstValidValueFrom(preferences, defaults, fallback = FONT.value),
            fontSize = fontSize.copyFirstValidValueFrom(preferences, defaults),
            overflow = overflow.copyFirstValidValueFrom(preferences, defaults, fallback = OVERFLOW.value),
            publisherStyles = publisherStyles.copyFirstValidValueFrom(preferences, defaults),
            theme = theme.copyFirstValidValueFrom(preferences, defaults, fallback = THEME.value),
            wordSpacing = wordSpacing.copyFirstValidValueFrom(preferences, defaults),
        )
}

@ExperimentalReadiumApi
fun ReadiumCss.update(settings: EpubSettings) {
    with(settings) {
        userProperties.update { props ->
            props.copy(
                view = when (overflow.value) {
                    Overflow.AUTO -> null
                    Overflow.PAGINATED -> ReadiumCss.View.Paged
                    Overflow.SCROLLED -> ReadiumCss.View.Scroll
                },
                colCount = when (columnCount?.value) {
                    ColumnCount.One -> ReadiumCss.ColCount.One
                    ColumnCount.Two -> ReadiumCss.ColCount.Two
                    else -> ReadiumCss.ColCount.Auto
                },
                appearance = when (theme.value) {
                    Theme.Light -> null
                    Theme.Dark -> ReadiumCss.Appearance.Night
                    Theme.Sepia -> ReadiumCss.Appearance.Sepia
                },
                fontOverride = (font.value != Font.ORIGINAL),
                fontFamily = font.value.name?.let { listOf(it) },
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { ReadiumCss.Length.Relative.Percent(it) },
                advancedSettings = !publisherStyles.value,
                wordSpacing = ReadiumCss.Length.Relative.Rem(wordSpacing.value),
            )
        }
    }
}
