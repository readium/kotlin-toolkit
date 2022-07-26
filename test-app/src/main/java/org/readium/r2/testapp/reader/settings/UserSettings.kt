/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.Update
import org.readium.r2.navigator.ColumnCount
import org.readium.r2.navigator.Font
import org.readium.r2.navigator.Theme
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.compose.DropdownMenuButton
import org.readium.r2.testapp.utils.compose.ToggleButtonGroup

/**
 * Closure which updates and applies a set of [Preferences].
 */
typealias UpdatePreferences = (MutablePreferences.() -> Unit) -> Unit

/**
 * Stateful user settings component paired with a [ReaderViewModel].
 */
@Composable
fun UserSettings(model: UserSettingsViewModel) {
    UserSettings(
        settings = model.settings.collectAsState().value ?: return,
        preferences = model.preferences.collectAsState().value,
        update = model::updatePreferences
    )
}

/**
 * Stateless user settings component displaying the given [settings] and setting user [preferences],
 * using the [update] closure.
 */
@Composable
fun UserSettings(
    settings: Configurable.Settings,
    preferences: Preferences,
    update: UpdatePreferences
) {
    Column(
        modifier = Modifier.padding(vertical = 24.dp)
    ) {
        Text(
            text = "User settings",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetsMenuButton(update = update, presets = settings.presets())

            Button(
                onClick = { update { clear() } },
            ) {
                Text("Reset")
            }
        }

        Divider()

        when (settings) {
            is EpubSettings ->
                ReflowableUserSettings(
                    preferences = preferences,
                    update = update,
                    columnCount = settings.columnCount,
                    font = settings.font,
                    fontSize = settings.fontSize,
                    overflow = settings.overflow,
                    publisherStyles = settings.publisherStyles,
                    wordSpacing = settings.wordSpacing,
                    theme = settings.theme,
                )
        }
    }
}

/**
 * User settings for a publication with adjustable fonts and dimensions, such as
 * a reflowable EPUB, HTML document or PDF with reflow mode enabled.
 */
@Composable
private fun ReflowableUserSettings(
    preferences: Preferences,
    update: UpdatePreferences,
    columnCount: EnumSetting<ColumnCount>? = null,
    font: EnumSetting<Font>? = null,
    fontSize: PercentSetting? = null,
    overflow: EnumSetting<Overflow>? = null,
    publisherStyles: ToggleSetting? = null,
    wordSpacing: PercentSetting? = null,
    theme: EnumSetting<Theme>? = null,
) {
    if (theme != null) {
        ButtonGroupItem("Theme", theme, preferences, update) { value ->
            when (value) {
                Theme.Light -> "Light"
                Theme.Dark -> "Dark"
                Theme.Sepia -> "Sepia"
            }
        }

        Divider()
    }

    if (overflow != null || columnCount != null) {
        if (overflow != null) {
            ButtonGroupItem("Overflow", overflow, preferences, update) { value ->
                when (value) {
                    Overflow.AUTO -> "Auto"
                    Overflow.PAGINATED -> "Paginated"
                    Overflow.SCROLLED -> "Scrolled"
                }
            }
        }

        if (columnCount != null) {
            ButtonGroupItem("Columns", columnCount, preferences, update) { value ->
                when (value) {
                    ColumnCount.Auto -> "Auto"
                    ColumnCount.One -> "1"
                    ColumnCount.Two -> "2"
                }
            }
        }

        Divider()
    }

    if (font != null || fontSize != null) {
        if (font != null) {
            DropdownMenuItem("Font", font, preferences, update) { value ->
                checkNotNull(
                    when (value) {
                        Font.ORIGINAL -> "Original"
                        else -> font.label(value)
                    }
                )
            }
        }

        if (fontSize != null) {
            RangeItem("Font size", fontSize, preferences, update)
        }

        Divider()
    }

    if (publisherStyles != null) {
        SwitchItem("Publisher styles", publisherStyles, preferences, update)
    }

    if (wordSpacing != null) {
        RangeItem("Word spacing", wordSpacing, preferences, update)
    }
}

/**
 * Component for an [EnumSetting] displayed as a group of mutually exclusive buttons.
 * This works best with a small number of enum values.
 */
@Composable
private inline fun <reified T> ButtonGroupItem(
    title: String,
    setting: EnumSetting<T>,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
    crossinline label: (T) -> String
) {
    Item(title, isActive = preferences.isActive(setting)) {
        ToggleButtonGroup(
            options = setting.values,
            activeOption = setting.value,
            selectedOption = preferences[setting],
            onSelectOption = { option ->
                update {
                    toggle(setting, option)
                }
            }) { option ->
            Text(label(option))
        }
    }
}

/**
 * Component for an [EnumSetting] displayed as a dropdown menu.
 */
@Composable
private inline fun <reified T> DropdownMenuItem(
    title: String,
    setting: EnumSetting<T>,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
    crossinline label: (T) -> String
) {
    Item(title, isActive = preferences.isActive(setting)) {
        DropdownMenuButton(
            text = { Text(label(setting.value)) }
        ) {
            for (value in setting.values) {
                DropdownMenuItem(
                    onClick = {
                        update { set(setting, value) }
                    }
                ) {
                    Text(label(value))
                }
            }
        }
    }
}

/**
 * Component for a [RangeSetting] with decrement and increment buttons.
 */
@Composable
private inline fun RangeItem(
    title: String,
    setting: RangeSetting<Double>,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
) {
    Item(title, isActive = preferences.isActive(setting)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    update {
                        decrement(setting)
                    }
                },
                content = {
                    Icon(Icons.Default.Remove, contentDescription = "Less")
                }
            )

            Text(setting.label((preferences[setting] ?: setting.value)))

            IconButton(
                onClick = {
                    update {
                        increment(setting)
                    }
                },
                content = {
                    Icon(Icons.Default.Add, contentDescription = "More")
                }
            )
        }
    }
}

/**
 * Component for a switchable [ToggleSetting].
 */
@Composable
private inline fun SwitchItem(
    title: String,
    setting: ToggleSetting,
    preferences: Preferences,
    crossinline update: UpdatePreferences
) {
    Item(
        title = title,
        isActive = preferences.isActive(setting),
        onClick = { update { toggle(setting)} }
    ) {
        Switch(
            checked = setting.value,
            onCheckedChange = { value ->
                update { set(setting, value) }
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Item(title: String, isActive: Boolean = true, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    ListItem(
        modifier =
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier,
        text = {
            val alpha = if (isActive) 1.0f else ContentAlpha.disabled
            CompositionLocalProvider(LocalContentAlpha provides alpha) {
                Text(title)
            }
        },
        trailing = content
    )
}

@Composable
private fun Divider() {
    Divider(modifier = Modifier.padding(vertical = 16.dp))
}

/**
 * A preset is a named group of settings applied together.
 */
private class Preset(
    val title: String,
    val changes: MutablePreferences.() -> Unit
)

/**
 * Returns the presets associated with the [Configurable.Settings] receiver.
 */
private fun Configurable.Settings.presets(): List<Preset> =
    when (val settings = this) {
        is EpubSettings -> listOf(
            Preset("Increase legibility") {
                set(settings.wordSpacing, 0.6)
                set(settings.fontSize, 1.4)
            },
            Preset("Document") {
                set(settings.overflow, Overflow.SCROLLED)
            },
            Preset("Ebook") {
                set(settings.overflow, Overflow.PAGINATED)
            },
            Preset("Manga") {
                // TODO
//            set(settings.readingProgression, ReadingProgression.RTL)
                set(settings.overflow, Overflow.PAGINATED)
            }
        )
        else -> emptyList()
    }

@Composable
private fun PresetsMenuButton(update: UpdatePreferences, presets: List<Preset>) {
    if (presets.isEmpty()) return

    DropdownMenuButton(
        text = { Text("Presets") }
    ) { dismiss ->

        for (preset in presets) {
            DropdownMenuItem(
                onClick = {
                    update(preset.changes)
                    dismiss()
                }
            ) {
                Text(preset.title)
            }
        }
    }
}
