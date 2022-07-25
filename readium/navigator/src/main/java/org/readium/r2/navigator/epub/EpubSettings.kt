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
    val columnCount: EnumSetting<ColumnCount>,
    val font: EnumSetting<Font>,
    val fontSize: PercentSetting,
    val overflow: EnumSetting<Overflow>,
    val publisherStyles: ToggleSetting,
    val theme: EnumSetting<Theme>,
) : Configurable.Settings {
    constructor(preferences: Preferences, fallback: Preferences, fonts: List<Font>) : this(
        columnCount = EnumSetting(
            key = SettingKey.COLUMN_COUNT,
            valueCandidates =
                if (preferences.overflow == Overflow.SCROLLED) listOf(ColumnCount.One)
                else listOf(preferences.columnCount, fallback.columnCount, ColumnCount.Auto),
            values = listOf(ColumnCount.Auto, ColumnCount.One, ColumnCount.Two),
            activator = object : SettingActivator {
                override fun isActiveWithPreferences(preferences: Preferences): Boolean =
                    preferences.overflow != Overflow.SCROLLED

                override fun activateInPreferences(preferences: MutablePreferences) {
                    preferences.overflow = Overflow.PAGINATED
                }
            }
        ),
        font = EnumSetting(
            key = SettingKey.FONT,
            valueCandidates = listOf(preferences.font, fallback.font, Font.ORIGINAL),
            values = listOf(Font.ORIGINAL) + fonts
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
                colCount = when (columnCount.value) {
                    ColumnCount.Auto -> ReadiumCss.ColCount.Auto
                    ColumnCount.One -> ReadiumCss.ColCount.One
                    ColumnCount.Two -> ReadiumCss.ColCount.Two
                },
                appearance = when (theme.value) {
                    Theme.Light -> null
                    Theme.Dark -> ReadiumCss.Appearance.Night
                    Theme.Sepia -> ReadiumCss.Appearance.Sepia
                },
                fontOverride = (font.value != Font.ORIGINAL),
                fontFamily = font.value.name?.let { listOf(it) },
                fontSize = fontSize.value
                    .takeIf { it != 1.0 }
                    ?.let { ReadiumCss.Length.Relative.Percent(it) },
                advancedSettings = !publisherStyles.value
            )
        }
    }
}
