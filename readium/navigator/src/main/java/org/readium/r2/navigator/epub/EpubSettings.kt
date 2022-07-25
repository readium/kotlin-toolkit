/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import kotlinx.coroutines.flow.update
import org.readium.r2.navigator.Font
import org.readium.r2.navigator.Theme
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation.Overflow

@ExperimentalReadiumApi
data class EpubSettings(
    val columnCount: RangeSetting<Int>,
    val font: EnumSetting<Font>,
    val fontSize: PercentSetting,
    val overflow: EnumSetting<Overflow>,
    val publisherStyles: ToggleSetting,
    val theme: EnumSetting<Theme>,
) : Configurable.Settings {
    constructor(preferences: Preferences, fallback: Preferences, fonts: List<Font>) : this(
        columnCount = RangeSetting(
            key = SettingKey.COLUMN_COUNT,
            valueCandidates = listOf(preferences.columnCount, fallback.columnCount, 1),
            range = 1..2
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
            valueCandidates = listOf(preferences.theme, fallback.theme, Theme.LIGHT),
            values = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
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
                    1 -> ReadiumCss.ColCount.One
                    else -> null
                },
                appearance = when (theme.value) {
                    Theme.LIGHT -> null
                    Theme.DARK -> ReadiumCss.Appearance.Night
                    Theme.SEPIA -> ReadiumCss.Appearance.Sepia
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
