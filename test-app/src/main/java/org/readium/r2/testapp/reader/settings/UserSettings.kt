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
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.Font
import org.readium.r2.navigator.Theme
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation.Overflow
import org.readium.r2.testapp.reader.ReaderViewModel
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
        theme = settings.theme,
    )
}

@Composable
fun UserSettings(
    preferences: Preferences,
    update: UpdatePreferences,
    columnCount: RangeSetting<Int>? = null,
    font: EnumSetting<Font>? = null,
    fontSize: PercentSetting? = null,
    overflow: EnumSetting<Overflow>? = null,
    publisherStyles: ToggleSetting? = null,
    theme: EnumSetting<Theme>? = null,
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User settings",
            style = MaterialTheme.typography.subtitle1,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            PresetsMenuButton(onCommit = onCommit)
            Button(
                onClick = {
                    update { clear() }
                },
            ) {
                Text("Reset")
            }
        }

        EnumSettingView("Overflow", overflow, preferences, update) { value ->
            when (value) {
                Overflow.AUTO -> "Auto"
                Overflow.PAGINATED -> "Paginated"
                Overflow.SCROLLED -> "Scrolled"
            }
        }

        RangeSection("Font size", fontSize, preferences, update)

        EnumSettingView("Theme", theme, preferences, update) { value ->
            when (value) {
                Theme.LIGHT -> "Light"
                Theme.DARK -> "Dark"
                Theme.SEPIA -> "Sepia"
            }
        }
    }
}

@Composable
inline fun <reified T> EnumSettingView(
    title: String,
    setting: EnumSetting<T>?,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
    crossinline label: (T) -> String
) {
    setting ?: return

    Section(title, isActive = preferences.isActive(setting)) {
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

@Composable
inline fun RangeSection(
    title: String,
    setting: RangeSetting<Double>?,
    preferences: Preferences,
    crossinline update: UpdatePreferences,
) {
    setting ?: return

    Section(title, isActive = preferences.isActive(setting)) {
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

@Composable
fun Section(title: String, isActive: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val alpha = if (isActive) 1.0f else ContentAlpha.disabled
    CompositionLocalProvider(LocalContentAlpha provides alpha) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
            )
            content()
        }
    }
}
