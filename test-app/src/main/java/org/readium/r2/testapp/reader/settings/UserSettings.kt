/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

typealias UpdatePreferences = (MutablePreferences.() -> Unit) -> Unit

@Composable
fun UserSettings(model: ReaderViewModel) {
    val settings = model.settings.collectAsState().value
    val preferences = model.preferences.collectAsState().value

    when (settings) {
        is EpubSettings ->
            UserSettings(preferences, update = model::updatePreferences, settings)
        null -> {}
    }
}

@Composable
fun UserSettings(
    preferences: Preferences,
    update: UpdatePreferences,
    settings: EpubSettings,
) {
    UserSettings(
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

@Composable
fun UserSettings(
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
    Column(
        modifier = Modifier
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User settings",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .fillMaxWidth()
        )

        ButtonGroupItem("Theme", theme, preferences, update) { value ->
            when (value) {
                Theme.Light -> "Light"
                Theme.Dark -> "Dark"
                Theme.Sepia -> "Sepia"
            }
        }

        Divider()

        ButtonGroupItem("Overflow", overflow, preferences, update) { value ->
            when (value) {
                Overflow.AUTO -> "Auto"
                Overflow.PAGINATED -> "Paginated"
                Overflow.SCROLLED -> "Scrolled"
            }
        }

        ButtonGroupItem("Columns", columnCount, preferences, update) { value ->
            when (value) {
                ColumnCount.Auto -> "Auto"
                ColumnCount.One -> "1"
                ColumnCount.Two -> "2"
            }
        }

        Divider()

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

        RangeItem("Font size", fontSize, preferences, update)

        Divider()

        SwitchItem("Publisher styles", publisherStyles, preferences, update)
        RangeItem("Word spacing", wordSpacing, preferences, update)

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    update { clear() }
                },
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
inline fun <reified T> ButtonGroupItem(
    title: String,
    setting: EnumSetting<T>?,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
    crossinline label: (T) -> String
) {
    setting ?: return

    Item(title, isActive = preferences.isActive(setting)) {
        ToggleButtonGroup(
            options = setting.values,
            activeOption = setting.value,
            selectedOption = preferences[setting],
            onSelectOption = { option ->
                update {
                    activate(setting)
                    toggle(setting, option)
                }
            }) { option ->
            Text(label(option))
        }
    }
}

@Composable
inline fun <reified T> DropdownMenuItem(
    title: String,
    setting: EnumSetting<T>?,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
    crossinline label: (T) -> String
) {
    setting ?: return

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

@Composable
inline fun RangeItem(
    title: String,
    setting: RangeSetting<Double>?,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
) {
    setting ?: return

    Item(title, isActive = preferences.isActive(setting)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    update {
                        activate(setting)
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
                        activate(setting)
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

@Composable
inline fun SwitchItem(
    title: String,
    setting: ToggleSetting?,
    preferences: Preferences,
    crossinline update: UpdatePreferences
) {
    setting ?: return

    Item(title, isActive = preferences.isActive(setting)) {
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
fun Item(title: String, isActive: Boolean = true, content: @Composable () -> Unit) {
    ListItem(
        text = {
            val alpha = if (isActive) 1.0f else ContentAlpha.disabled
            CompositionLocalProvider(LocalContentAlpha provides alpha) {
                Text(title)
            }
        },
        trailing = content
    )
}
