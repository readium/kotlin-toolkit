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
    val columnCount: EnumSetting<ColumnCount>?,
    val font: EnumSetting<Font>,
    val fontSize: PercentSetting,
    val overflow: EnumSetting<Overflow>,
    val publisherStyles: ToggleSetting,
    val theme: EnumSetting<Theme>,
    val wordSpacing: PercentSetting,
) : Configurable.Settings {
    companion object {
        operator fun invoke(preferences: Preferences, fallback: Preferences, fonts: List<Font>): EpubSettings {
            return EpubSettings(
                columnCount = if (preferences[overflow] == Overflow.SCROLLED) null
                    else columnCount.copyFirstValidValueFrom(preferences, fallback),
                font = font.copyFirstValidValueFrom(preferences, fallback),
                fontSize = fontSize.copyFirstValidValueFrom(preferences, fallback),
                overflow = overflow.copyFirstValidValueFrom(preferences, fallback),
                publisherStyles = publisherStyles.copyFirstValidValueFrom(preferences, fallback),
                theme = theme.copyFirstValidValueFrom(preferences, fallback),
                wordSpacing = wordSpacing.copyFirstValidValueFrom(preferences, fallback),
            )
        }

        private val columnCount: EnumSetting<ColumnCount> = EnumSetting(
            key = Setting.COLUMN_COUNT,
            coder = ColumnCount,
            value = ColumnCount.Auto,
            values = listOf(ColumnCount.Auto, ColumnCount.One, ColumnCount.Two),
        )

        private val font: EnumSetting<Font> = EnumSetting(
            key = Setting.FONT,
            coder = Font,
            value = Font.ORIGINAL,
            values = listOf(Font.ORIGINAL),
            label = { it.name }
        )

        private val fontSize: PercentSetting = PercentSetting(
            key = Setting.FONT_SIZE,
            value = 1.0,
            range = 0.4..5.0
        )

        private val overflow: EnumSetting<Overflow> = EnumSetting(
            key = Setting.OVERFLOW,
            coder = Overflow,
            value = Overflow.PAGINATED,
            values = listOf(Overflow.PAGINATED, Overflow.SCROLLED),
        )

        private val publisherStyles: ToggleSetting = ToggleSetting(
            key = Setting.PUBLISHER_STYLES,
            value = true,
        )

        private val theme: EnumSetting<Theme> = EnumSetting(
            key = Setting.THEME,
            coder = Theme,
            value = Theme.Light,
            values = listOf(Theme.Light, Theme.Dark, Theme.Sepia)
        )

        private val wordSpacing: PercentSetting = PercentSetting(
            key = Setting.WORD_SPACING,
            value = 0.0,
            activator = object : SettingActivator {
                override fun isActiveWithPreferences(preferences: Preferences): Boolean =
                    preferences[publisherStyles] == false

                override fun activateInPreferences(preferences: MutablePreferences) {
                    preferences[publisherStyles] = false
                }
            }
        )
    }
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
