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
    constructor(preferences: Preferences, fallback: Preferences, fonts: List<Font>) : this(
        columnCount =
            if (preferences.overflow == Overflow.SCROLLED) null
            else EnumSetting(
                key = SettingKey.COLUMN_COUNT,
                valueCandidates = listOf(preferences.columnCount, fallback.columnCount, ColumnCount.Auto),
                values = listOf(ColumnCount.Auto, ColumnCount.One, ColumnCount.Two),
            ),
        font = EnumSetting(
            key = SettingKey.FONT,
            valueCandidates = listOf(preferences.font, fallback.font, Font.ORIGINAL),
            values = listOf(Font.ORIGINAL) + fonts,
            label = { it.name }
        ),
        fontSize = PercentSetting(
            key = SettingKey.FONT_SIZE,
            valueCandidates = listOf(preferences.fontSize, fallback.fontSize, 1.0),
            range = 0.4..5.0
        ),
        overflow = EnumSetting(
            key = SettingKey.OVERFLOW,
            valueCandidates = listOf(preferences.overflow, fallback.overflow, Overflow.PAGINATED),
            values = listOf(Overflow.PAGINATED, Overflow.SCROLLED),
        ),
        publisherStyles = ToggleSetting(
            key = SettingKey.PUBLISHER_STYLES,
            valueCandidates = listOf(preferences.publisherStyles, fallback.publisherStyles, true)
        ),
        theme = EnumSetting(
            key = SettingKey.THEME,
            valueCandidates = listOf(preferences.theme, fallback.theme, Theme.Light),
            values = listOf(Theme.Light, Theme.Dark, Theme.Sepia)
        ),
        wordSpacing = PercentSetting(
            key = SettingKey.WORD_SPACING,
            valueCandidates = listOf(preferences.wordSpacing, fallback.wordSpacing, 0.0),
            activator = object : SettingActivator {
                override fun isActiveWithPreferences(preferences: Preferences): Boolean =
                    preferences.publisherStyles == false

                override fun activateInPreferences(preferences: MutablePreferences) {
                    preferences.publisherStyles = false
                }
            }
        )
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
