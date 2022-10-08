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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.compose.ColorPicker
import org.readium.r2.testapp.utils.compose.DropdownMenuButton
import org.readium.r2.testapp.utils.compose.ToggleButtonGroup
import java.util.*
import org.readium.r2.navigator.settings.Color as ReadiumColor
import org.readium.r2.navigator.settings.TextAlign as ReadiumTextAlign

/**
 * Closure which updates and applies a set of [Preferences].
 */
typealias EditPreferences = (MutablePreferences.() -> Unit) -> Unit

/**
 * Stateful user settings component paired with a [ReaderViewModel].
 */
@Composable
fun UserSettings(model: UserSettingsViewModel) {
    UserSettings(
        settings = model.settings.collectAsState().value ?: return,
        preferences = model.preferences.collectAsState().value,
        editNavigator = model::editNavigatorPreferences,
        editPublication = model::editPublicationPreferences
    )
}

/**
 * Stateless user settings component displaying the given [settings] and setting user [preferences],
 * using the [editNavigator] and [editPublication] closures.
 */
@Composable
fun UserSettings(
    settings: Configurable.Settings,
    preferences: Preferences,
    editNavigator: EditPreferences,
    editPublication: EditPreferences
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
            PresetsMenuButton(edit = editPublication, presets = settings.presets())

            Button(
                onClick = {
                    editPublication { clear() }
                    editNavigator { clear() }
                },
            ) {
                Text("Reset")
            }
        }

        Divider()

        when (settings) {
            is FixedLayoutSettings ->
                FixedLayoutUserSettings(
                    preferences = preferences,
                    editNavigator = editNavigator,
                    editPublication = editPublication,
                    settings = settings
                )

            is ReflowaleSettings ->
                ReflowableUserSettings(
                    preferences = preferences,
                    editNavigator = editNavigator,
                    editPublication = editPublication,
                    settings = settings
                )
        }
    }
}

/**
 * User settings for a publication with a fixed layout, such as fixed-layout EPUB, PDF or comic book.
 */
@Composable
private fun ColumnScope.FixedLayoutUserSettings(
    preferences: Preferences,
    editNavigator: EditPreferences,
    editPublication: EditPreferences,
    settings: FixedLayoutSettings
) = with(settings) {
    if (language != null || readingProgression != null) {
        fun reset() {
            editPublication {
                remove(language)
                remove(readingProgression)
            }
        }

        language?.let { LanguageItem(it, preferences, editPublication) }

        readingProgression?.let {
            ButtonGroupItem(title = "Reading progression", it, preferences , editPublication) { value ->
                when (value) {
                    ReadingProgression.AUTO -> "Auto"
                    else -> value.name
                }
            }
        }

        // The language settings are specific to a publication. This button resets only the
        // language preferences to the publication's default metadata for convenience.
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = ::reset) {
                Text("Reset to publication", style = MaterialTheme.typography.caption)
            }
        }

        Divider()
    }

    scroll?.let {  SwitchItem(title = "Scroll", it, preferences, editNavigator) }

    scrollAxis?.let {
        ButtonGroupItem("Scroll axis", it, preferences, editNavigator) { value ->
            when (value) {
                Axis.HORIZONTAL-> "Horizontal"
                Axis.VERTICAL -> "Vertical"
            }
        }
    }

    spread?.let {
        ButtonGroupItem("Spread", it, preferences, editNavigator) { value ->
            when (value) {
                Spread.AUTO -> "Auto"
                Spread.NEVER -> "Never"
                Spread.PREFERRED -> "Preferred"
            }
        }

        offset?.let {
            SwitchItem("Offset", it, preferences, editPublication)
        }
    }

    fit?.let {
        ButtonGroupItem("Fit", it, preferences, editNavigator) { value ->
            when (value) {
                Fit.CONTAIN-> "Contain"
                Fit.COVER -> "Cover"
                Fit.WIDTH -> "Width"
                Fit.HEIGHT -> "Height"
            }
        }
    }

    pageSpacing?.let {
        StepperItem("Page spacing", it, preferences, editNavigator)
    }
}

/**
 * User settings for a publication with adjustable fonts and dimensions, such as
 * a reflowable EPUB, HTML document or PDF with reflow mode enabled.
 */
@Composable
private fun ColumnScope.ReflowableUserSettings(
    preferences: Preferences,
    editNavigator: EditPreferences,
    editPublication: EditPreferences,
    settings: ReflowaleSettings
) = with (settings) {
    if (language != null || readingProgression != null || verticalText != null) {
        fun reset() {
            editPublication {
                remove(language)
                remove(readingProgression)
                remove(verticalText)
            }
        }

        language?.let { LanguageItem(it, preferences, editPublication) }

        readingProgression?.let {
            ButtonGroupItem(title = "Reading progression", it, preferences , editPublication) { value ->
                value.name
            }
        }

        verticalText?.let {
            SwitchItem(title = "Vertical text", it, preferences, editPublication)
        }

        // The language settings are specific to a publication. This button resets only the
        // language preferences to the publication's default metadata for convenience.
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = ::reset) {
                Text("Reset to publication", style = MaterialTheme.typography.caption)
            }
        }

        Divider()
    }

    if (scroll != null || columnCount != null || pageMargins != null) {
        scroll?.let {
            SwitchItem(title = "Scroll", it, preferences, editNavigator)
        }

        columnCount?.let {
            ButtonGroupItem("Columns", it, preferences, editNavigator) { value ->
                when (value) {
                    ColumnCount.AUTO -> "Auto"
                    ColumnCount.ONE -> "1"
                    ColumnCount.TWO -> "2"
                }
            }
        }

        pageMargins?.let {
            StepperItem("Page margins", it, preferences, editNavigator)
        }

        Divider()
    }

    if (theme != null || textColor != null || imageFilter != null) {
        theme?.let {
            ButtonGroupItem("Theme", it, preferences, editNavigator) { value ->
                when (value) {
                    Theme.LIGHT -> "Light"
                    Theme.DARK -> "Dark"
                    Theme.SEPIA -> "Sepia"
                }
            }
        }

        imageFilter?.let {
            ButtonGroupItem("Image filter", it, preferences, editNavigator) { value ->
                when (value) {
                    ImageFilter.NONE -> "None"
                    ImageFilter.DARKEN -> "Darken"
                    ImageFilter.INVERT -> "Invert"
                }
            }
        }

        textColor?.let {
            ColorItem("Text color", it, preferences, editNavigator)
        }

        backgroundColor?.let {
            ColorItem("Background color", it, preferences, editNavigator)
        }

        Divider()
    }

    if (fontFamily != null || fontSize != null || textNormalization != null) {
        fontFamily?.let {
            MenuItem("Typeface", it, preferences, editNavigator) { value ->
                value?.name ?: "Original"
            }
        }

        fontSize?.let {
            StepperItem("Font size", it, preferences, editNavigator)
        }

        textNormalization?.let {
            ButtonGroupItem("Text normalization", it, preferences, editNavigator) { value ->
                when (value) {
                    TextNormalization.NONE -> "None"
                    TextNormalization.BOLD -> "Bold"
                    TextNormalization.ACCESSIBILITY -> "A11y"
                }
            }
        }

        Divider()
    }

    publisherStyles?.let {
        SwitchItem("Publisher styles", it, preferences, editNavigator)
    }

    textAlign?.let {
        ButtonGroupItem("Alignment", it, preferences, editNavigator) { value ->
            when (value) {
                ReadiumTextAlign.CENTER -> "Center"
                ReadiumTextAlign.JUSTIFY -> "Justify"
                ReadiumTextAlign.START -> "Start"
                ReadiumTextAlign.END -> "End"
                ReadiumTextAlign.LEFT -> "Left"
                ReadiumTextAlign.RIGHT -> "Right"
            }
        }
    }

    typeScale?.let {
        StepperItem("Type scale", it, preferences, editNavigator)
    }

    lineHeight?.let {
        StepperItem("Line height", it, preferences, editNavigator)
    }

    paragraphIndent?.let {
        StepperItem("Paragraph indent", it, preferences, editNavigator)
    }

    paragraphSpacing?.let {
        StepperItem("Paragraph spacing", it, preferences, editNavigator)
    }

    wordSpacing?.let {
        StepperItem("Word spacing", it, preferences, editNavigator)
    }

    letterSpacing?.let {
        StepperItem("Letter spacing", it, preferences, editNavigator)
    }

    hyphens?.let {
        SwitchItem("Hyphens", it, preferences, editNavigator)
    }

    ligatures?.let {
        SwitchItem("Ligatures", it, preferences, editNavigator)
    }
}

/**
 * Component for an [EnumSetting] displayed as a group of mutually exclusive buttons.
 * This works best with a small number of enum values.
 */
@Composable
private fun <T> ButtonGroupItem(
    title: String,
    setting: EnumSetting<T>,
    preferences: Preferences,
    edit: EditPreferences,
    formatValue: (T) -> String
) {
    Item(title, isActive = preferences.isActive(setting)) {
        ToggleButtonGroup(
            options = setting.values ?: emptyList(),
            activeOption = setting.value,
            selectedOption = preferences[setting],
            onSelectOption = { option ->
                edit {
                    toggle(setting, option)
                }
            }
        ) { option ->
            Text(
                text = formatValue(option),
                style = MaterialTheme.typography.caption
            )
        }
    }
}

/**
 * Component for an [EnumSetting] displayed as a dropdown menu.
 */
@Composable
private fun <T> MenuItem(
    title: String,
    setting: EnumSetting<T>,
    preferences: Preferences,
    edit: EditPreferences,
    formatValue: (T) -> String
) {
    MenuItem(
        title = title, setting, preferences, edit,
        values = setting.values ?: emptyList(),
        formatValue = formatValue
    )
}

/**
 * Component displayed as a dropdown menu.
 */
@Composable
private fun <T> MenuItem(
    title: String,
    setting: Setting<T>,
    preferences: Preferences,
    edit: EditPreferences,
    values: List<T>,
    formatValue: (T) -> String
) {
    Item(title, isActive = preferences.isActive(setting)) {
        DropdownMenuButton(
            text = {
                Text(
                    text = formatValue(preferences[setting] ?: setting.value),
                    style = MaterialTheme.typography.caption
                )
            }
        ) { dismiss ->
            for (value in values) {
                DropdownMenuItem(
                    onClick = {
                        dismiss()
                        edit { set(setting, value) }
                    }
                ) {
                    Text(formatValue(value))
                }
            }
        }
    }
}

/**
 * Component for a [RangeSetting] with decrement and increment buttons.
 */
@Composable
private fun StepperItem(
    title: String,
    setting: RangeSetting<Double>,
    preferences: Preferences,
    edit: EditPreferences,
) {
    Item(title, isActive = preferences.isActive(setting)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    edit {
                        decrement(setting)
                    }
                },
                content = {
                    Icon(Icons.Default.Remove, contentDescription = "Less")
                }
            )

            Text(
                text = setting.formatValue((preferences[setting] ?: setting.value)),
                modifier = Modifier.widthIn(min = 30.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = {
                    edit {
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
 * Component for a switchable [Setting<Boolean>].
 */
@Composable
private fun SwitchItem(
    title: String,
    setting: Setting<Boolean>,
    preferences: Preferences,
    edit: EditPreferences
) {
    Item(
        title = title,
        isActive = preferences.isActive(setting),
        onClick = { edit { toggle(setting)} }
    ) {
        Switch(
            checked = preferences[setting] ?: setting.value,
            onCheckedChange = { value ->
                edit { set(setting, value) }
            }
        )
    }
}

/**
 * Component for a [Setting<ReadiumColor>].
 */
@Composable
private fun ColorItem(
    title: String,
    setting: Setting<ReadiumColor>,
    preferences: Preferences,
    edit: EditPreferences
) {
    var isPicking by remember { mutableStateOf(false) }

    Item(
        title = title,
        isActive = preferences.isActive(setting),
        onClick = { isPicking = true }
    ) {
        val color = Color(preferences[setting]?.int ?: setting.value.int)

        OutlinedButton(
            onClick = { isPicking = true },
            colors = ButtonDefaults.buttonColors(backgroundColor = color)
        ) {
            if (preferences[setting] == null) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Change color",
                    tint = if (color.luminance() > 0.5) Color.Black else Color.White
                )
            }
        }

        if (isPicking) {
            Dialog(
                onDismissRequest = { isPicking = false }
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    ColorPicker { color ->
                        isPicking = false
                        edit {
                            set(setting, ReadiumColor(color))
                        }
                    }
                    Button(
                        onClick = {
                            isPicking = false
                            edit {
                                remove(setting)
                            }
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

/**
 * Component for a `Setting<Language?>`.
 */
@Composable
fun LanguageItem(
     setting: Setting<Language?>,
     preferences: Preferences,
     edit: EditPreferences
) {
    val languages = remember {
        Locale.getAvailableLocales()
            .map { Language(it).removeRegion() }
            .distinct()
            .sortedBy { it.locale.displayName }
    }

    MenuItem(
        title = "Language", setting, preferences, edit,
        values = listOf(null) + languages,
        formatValue = { it?.locale?.displayName ?: "Default" }
    )
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
        is EpubSettings.Reflowable -> listOf(
            Preset("Increase legibility") {
                set(settings.wordSpacing, 0.6)
                set(settings.fontSize, 1.4)
                set(settings.textNormalization, TextNormalization.ACCESSIBILITY)
            },
            Preset("Document") {
                set(settings.scroll, true)
            },
            Preset("Ebook") {
                set(settings.scroll, false)
            },
            Preset("Manga") {
                set(settings.scroll, false)
                set(settings.readingProgression, ReadingProgression.RTL)
            }
        )
        else -> emptyList()
    }

@Composable
private fun PresetsMenuButton(edit: EditPreferences, presets: List<Preset>) {
    if (presets.isEmpty()) return

    DropdownMenuButton(
        text = { Text("Presets") }
    ) { dismiss ->

        for (preset in presets) {
            DropdownMenuItem(
                onClick = {
                    dismiss()
                    edit {
                        clear()
                        preset.changes(this)
                    }
                }
            ) {
                Text(preset.title)
            }
        }
    }
}
