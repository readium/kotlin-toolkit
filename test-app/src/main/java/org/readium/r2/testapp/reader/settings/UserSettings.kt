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
import org.readium.adapters.pdfium.navigator.PdfiumSettings
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.Fit
import org.readium.r2.shared.publication.presentation.Presentation.Spread
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
            is EpubSettings.FixedLayout ->
                FixedLayoutUserSettings(
                    preferences = preferences,
                    editNavigator = editNavigator,
                    editPublication = editPublication,
                    language = settings.language,
                    readingProgression = settings.readingProgression,
                    spread = settings.spread,
                    fit = null,
                    scroll = null,
                    scrollAxis = null
                )

            is PdfiumSettings ->
                FixedLayoutUserSettings(
                    preferences = preferences,
                    editNavigator = editNavigator,
                    editPublication = editPublication,
                    language = null,
                    readingProgression = settings.readingProgression,
                    scrollAxis = settings.scrollAxis,
                    fit = settings.fit
                )

            is EpubSettings.Reflowable ->
                ReflowableUserSettings(
                    preferences = preferences,
                    editNavigator = editNavigator,
                    editPublication = editPublication,
                    backgroundColor = settings.backgroundColor,
                    columnCount = settings.columnCount,
                    fontFamily = settings.fontFamily,
                    fontSize = settings.fontSize,
                    hyphens = settings.hyphens,
                    imageFilter = settings.imageFilter,
                    language = settings.language,
                    letterSpacing = settings.letterSpacing,
                    ligatures = settings.ligatures,
                    lineHeight = settings.lineHeight,
                    pageMargins = settings.pageMargins,
                    paragraphIndent = settings.paragraphIndent,
                    paragraphSpacing = settings.paragraphSpacing,
                    publisherStyles = settings.publisherStyles,
                    readingProgression = settings.readingProgression,
                    scroll = settings.scroll,
                    textAlign = settings.textAlign,
                    textColor = settings.textColor,
                    textNormalization = settings.textNormalization,
                    theme = settings.theme,
                    typeScale = settings.typeScale,
                    verticalText = settings.verticalText,
                    wordSpacing = settings.wordSpacing,
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
    spread: EnumSetting<Spread>? = null,
    fit: EnumSetting<Fit>? = null,
    language: Setting<Language?>? = null,
    readingProgression: EnumSetting<ReadingProgression>? = null,
    scroll: Setting<Boolean>? = null,
    scrollAxis: EnumSetting<Axis>? = null
) {
    if (language != null || readingProgression != null) {
        fun reset() {
            editPublication {
                remove(language)
                remove(readingProgression)
            }
        }

        if (language != null) {
            LanguageItem(language, preferences, editPublication)
        }

        if (readingProgression != null) {
            ButtonGroupItem(title = "Reading progression", readingProgression, preferences , editPublication) { value ->
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

    if (scroll != null) {
        SwitchItem(title = "Scroll", scroll, preferences, editNavigator)
    }

    if (scrollAxis != null) {
        ButtonGroupItem("Scroll axis", scrollAxis, preferences, editNavigator) { value ->
            when (value) {
                Axis.HORIZONTAL-> "Horizontal"
                Axis.VERTICAL -> "Vertical"
            }
        }
    }

    if (spread != null) {
        ButtonGroupItem("Spread", spread, preferences, editNavigator) { value ->
            when (value) {
                Spread.AUTO -> "Auto"
                Spread.BOTH -> "Both"
                Spread.NONE -> "None"
                Spread.LANDSCAPE -> "Landscape"
            }
        }
    }

    if (fit != null) {
        ButtonGroupItem("Fit", fit, preferences, editNavigator) { value ->
            when (value) {
                Fit.CONTAIN-> "Contain"
                Fit.COVER -> "Cover"
                Fit.WIDTH -> "Width"
                Fit.HEIGHT -> "Height"
            }
        }
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
    backgroundColor: Setting<ReadiumColor>? = null,
    columnCount: EnumSetting<ColumnCount>? = null,
    fontFamily: EnumSetting<FontFamily?>? = null,
    fontSize: PercentSetting? = null,
    hyphens: Setting<Boolean>? = null,
    imageFilter: EnumSetting<ImageFilter>? = null,
    language: Setting<Language?>? = null,
    letterSpacing: PercentSetting? = null,
    ligatures: Setting<Boolean>? = null,
    lineHeight: RangeSetting<Double>? = null,
    pageMargins: RangeSetting<Double>? = null,
    paragraphIndent: PercentSetting? = null,
    paragraphSpacing: PercentSetting? = null,
    publisherStyles: Setting<Boolean>? = null,
    readingProgression: EnumSetting<ReadingProgression>? = null,
    scroll: Setting<Boolean>? = null,
    textAlign: EnumSetting<ReadiumTextAlign>? = null,
    textColor: Setting<ReadiumColor>? = null,
    textNormalization: EnumSetting<TextNormalization>? = null,
    theme: EnumSetting<Theme>? = null,
    typeScale: RangeSetting<Double>? = null,
    verticalText: Setting<Boolean>? = null,
    wordSpacing: PercentSetting? = null,
) {
    if (language != null || readingProgression != null || verticalText != null) {
        fun reset() {
            editPublication {
                remove(language)
                remove(readingProgression)
                remove(verticalText)
            }
        }

        if (language != null) {
            LanguageItem(language, preferences, editPublication)
        }

        if (readingProgression != null) {
            ButtonGroupItem(title = "Reading progression", readingProgression, preferences , editPublication) { value ->
                value.name
            }
        }

        if (verticalText != null) {
            SwitchItem(title = "Vertical text", verticalText, preferences, editPublication)
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
        if (scroll != null) {
            SwitchItem(title = "Scroll", scroll, preferences, editNavigator)
        }

        if (columnCount != null) {
            ButtonGroupItem("Columns", columnCount, preferences, editNavigator) { value ->
                when (value) {
                    ColumnCount.AUTO -> "Auto"
                    ColumnCount.ONE -> "1"
                    ColumnCount.TWO -> "2"
                }
            }
        }

        if (pageMargins != null) {
            StepperItem("Page margins", pageMargins, preferences, editNavigator)
        }

        Divider()
    }

    if (theme != null || textColor != null || imageFilter != null) {
        if (theme != null) {
            ButtonGroupItem("Theme", theme, preferences, editNavigator) { value ->
                when (value) {
                    Theme.LIGHT -> "Light"
                    Theme.DARK -> "Dark"
                    Theme.SEPIA -> "Sepia"
                }
            }
        }

        if (imageFilter != null) {
            ButtonGroupItem("Image filter", imageFilter, preferences, editNavigator) { value ->
                when (value) {
                    ImageFilter.NONE -> "None"
                    ImageFilter.DARKEN -> "Darken"
                    ImageFilter.INVERT -> "Invert"
                }
            }
        }

        if (textColor != null) {
            ColorItem("Text color", textColor, preferences, editNavigator)
        }

        if (backgroundColor != null) {
            ColorItem("Background color", backgroundColor, preferences, editNavigator)
        }

        Divider()
    }

    if (fontFamily != null || fontSize != null || textNormalization != null) {
        if (fontFamily != null) {
            MenuItem("Typeface", fontFamily, preferences, editNavigator) { value ->
                value?.name ?: "Original"
            }
        }

        if (fontSize != null) {
            StepperItem("Font size", fontSize, preferences, editNavigator)
        }

        if (textNormalization != null) {
            ButtonGroupItem("Text normalization", textNormalization, preferences, editNavigator) { value ->
                when (value) {
                    TextNormalization.NONE -> "None"
                    TextNormalization.BOLD -> "Bold"
                    TextNormalization.ACCESSIBILITY -> "A11y"
                }
            }
        }

        Divider()
    }

    if (publisherStyles != null) {
        SwitchItem("Publisher styles", publisherStyles, preferences, editNavigator)
    }

    if (textAlign != null) {
        ButtonGroupItem("Alignment", textAlign, preferences, editNavigator) { value ->
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

    if (typeScale != null) {
        StepperItem("Type scale", typeScale, preferences, editNavigator)
    }

    if (lineHeight != null) {
        StepperItem("Line height", lineHeight, preferences, editNavigator)
    }

    if (paragraphIndent != null) {
        StepperItem("Paragraph indent", paragraphIndent, preferences, editNavigator)
    }

    if (paragraphSpacing != null) {
        StepperItem("Paragraph spacing", paragraphSpacing, preferences, editNavigator)
    }

    if (wordSpacing != null) {
        StepperItem("Word spacing", wordSpacing, preferences, editNavigator)
    }

    if (letterSpacing != null) {
        StepperItem("Letter spacing", letterSpacing, preferences, editNavigator)
    }

    if (hyphens != null) {
        SwitchItem("Hyphens", hyphens, preferences, editNavigator)
    }

    if (ligatures != null) {
        SwitchItem("Ligatures", ligatures, preferences, editNavigator)
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
