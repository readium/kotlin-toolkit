/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader.preferences

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
import java.util.*
import org.readium.adapters.pdfium.navigator.PdfiumPreferencesEditor
import org.readium.r2.navigator.epub.EpubPreferencesEditor
import org.readium.r2.navigator.preferences.*
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.compose.ColorPicker
import org.readium.r2.testapp.utils.compose.DropdownMenuButton
import org.readium.r2.testapp.utils.compose.ToggleButtonGroup

/**
 * Stateful user settings component paired with a [ReaderViewModel].
 */
@Composable
fun UserPreferences(model: UserPreferencesViewModel<*, *>) {
    val editor = remember { mutableStateOf(model.preferencesEditor, policy = neverEqualPolicy()) }
    val commit: () -> Unit = { editor.value = editor.value ; model.commitPreferences() }

    UserPreferences(
        editor = editor.value,
        commit = commit
    )
}

@Composable
private fun <P : Configurable.Preferences<P>, E : PreferencesEditor<P>> UserPreferences(
    editor: E,
    commit: () -> Unit
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
            PresetsMenuButton(presets = editor.presets, commit = commit, clear = editor::clear)

            Button(
                onClick = { editor.clear(); commit() }
            ) {
                Text("Reset")
            }
        }

        Divider()

        when (editor) {
            is PdfiumPreferencesEditor ->
                FixedLayoutUserPreferences(
                    commit = commit,
                    readingProgression = editor.readingProgression,
                    scrollAxis = editor.scrollAxis,
                    fit = editor.fit,
                    pageSpacing = editor.pageSpacing
                )

            is EpubPreferencesEditor ->
                when (editor.layout) {
                    EpubLayout.REFLOWABLE ->
                        ReflowableUserPreferences(
                            commit = commit,
                            backgroundColor = editor.backgroundColor,
                            columnCount = editor.columnCount,
                            fontFamily = editor.fontFamily,
                            fontSize = editor.fontSize,
                            hyphens = editor.hyphens,
                            imageFilter = editor.imageFilter,
                            language = editor.language,
                            letterSpacing = editor.letterSpacing,
                            ligatures = editor.ligatures,
                            lineHeight = editor.lineHeight,
                            pageMargins = editor.pageMargins,
                            paragraphIndent = editor.paragraphIndent,
                            paragraphSpacing = editor.paragraphSpacing,
                            publisherStyles = editor.publisherStyles,
                            readingProgression = editor.readingProgression,
                            scroll = editor.scroll,
                            textAlign = editor.textAlign,
                            textColor = editor.textColor,
                            textNormalization = editor.textNormalization,
                            theme = editor.theme,
                            typeScale = editor.typeScale,
                            verticalText = editor.verticalText,
                            wordSpacing = editor.wordSpacing,
                        )
                    EpubLayout.FIXED ->
                        FixedLayoutUserPreferences(
                            commit = commit,
                            language = editor.language,
                            readingProgression = editor.readingProgression,
                            spread = editor.spread,
                        )
                }
        }
    }
}

/**
 * User settings for a publication with a fixed layout, such as fixed-layout EPUB, PDF or comic book.
 */
@Composable
private fun ColumnScope.FixedLayoutUserPreferences(
    commit: () -> Unit,
    language: Preference<Language?>? = null,
    readingProgression: EnumPreference<ReadingProgression>? = null,
    scroll: Preference<Boolean>? = null,
    scrollAxis: EnumPreference<Axis>? = null,
    fit: EnumPreference<Fit>? = null,
    spread: EnumPreference<Spread>? = null,
    offsetFirstPage: Preference<Boolean>? = null,
    pageSpacing: RangePreference<Double>? = null
) {
    if (language != null || readingProgression != null) {
        fun reset() {
            language?.clear()
            readingProgression?.clear()
            commit()
        }

        if (language != null) {
            LanguageItem(
                preference = language,
                commit = commit
            )
        }

        if (readingProgression != null) {
            ButtonGroupItem(
                title = "Reading progression",
                preference = readingProgression,
                commit = commit,
                formatValue = { it.name }
            )
        }

        // The language preferences are specific to a publication. This button resets only the
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
        SwitchItem(
            title = "Scroll",
            preference = scroll,
            commit = commit
        )
    }

    if (scrollAxis != null) {
        ButtonGroupItem(
            title = "Scroll axis",
            preference = scrollAxis,
            commit = commit
        ) { value ->
            when (value) {
                Axis.HORIZONTAL -> "Horizontal"
                Axis.VERTICAL -> "Vertical"
            }
        }
    }

    if (spread != null) {
        ButtonGroupItem(
            title = "Spread",
            preference = spread,
            commit = commit,
        ) { value ->
            when (value) {
                Spread.AUTO -> "Auto"
                Spread.NEVER -> "Never"
                Spread.ALWAYS -> "Always"
            }
        }

        if (offsetFirstPage != null) {
            SwitchItem(
                title = "Offset",
                preference = offsetFirstPage,
                commit = commit
            )
        }
    }

    if (fit != null) {
        ButtonGroupItem(
            title = "Fit",
            preference = fit,
            commit = commit
        ) { value ->
            when (value) {
                Fit.CONTAIN -> "Contain"
                Fit.COVER -> "Cover"
                Fit.WIDTH -> "Width"
                Fit.HEIGHT -> "Height"
            }
        }
    }

    if (pageSpacing != null) {
        StepperItem(
            title = "Page spacing",
            preference = pageSpacing,
            commit = commit
        )
    }
}

/**
 * User settings for a publication with adjustable fonts and dimensions, such as
 * a reflowable EPUB, HTML document or PDF with reflow mode enabled.
 */
@Composable
private fun ColumnScope.ReflowableUserPreferences(
    commit: () -> Unit,
    backgroundColor: Preference<ReadiumColor>? = null,
    columnCount: EnumPreference<ColumnCount>? = null,
    fontFamily: EnumPreference<FontFamily?>? = null,
    fontSize: RangePreference<Double>? = null,
    hyphens: Preference<Boolean>? = null,
    imageFilter: EnumPreference<ImageFilter>? = null,
    language: Preference<Language?>? = null,
    letterSpacing: RangePreference<Double>? = null,
    ligatures: Preference<Boolean>? = null,
    lineHeight: RangePreference<Double>? = null,
    pageMargins: RangePreference<Double>? = null,
    paragraphIndent: RangePreference<Double>? = null,
    paragraphSpacing: RangePreference<Double>? = null,
    publisherStyles: Preference<Boolean>? = null,
    readingProgression: EnumPreference<ReadingProgression>? = null,
    scroll: Preference<Boolean>? = null,
    textAlign: EnumPreference<ReadiumTextAlign>? = null,
    textColor: Preference<ReadiumColor>? = null,
    textNormalization: EnumPreference<TextNormalization>? = null,
    theme: EnumPreference<Theme>? = null,
    typeScale: RangePreference<Double>? = null,
    verticalText: Preference<Boolean>? = null,
    wordSpacing: RangePreference<Double>? = null,
) {
    if (language != null || readingProgression != null || verticalText != null) {
        fun reset() {
            language?.clear()
            readingProgression?.clear()
            verticalText?.clear()
            commit()
        }

        if (language != null) {
            LanguageItem(
                preference = language,
                commit = commit
            )
        }

        if (readingProgression != null) {
            ButtonGroupItem(
                title = "Reading progression",
                preference = readingProgression,
                commit = commit,
                formatValue = { it.name }
            )
        }

        if (verticalText != null) {
            SwitchItem(
                title = "Vertical text",
                preference = verticalText,
                commit = commit
            )
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
            SwitchItem(
                title = "Scroll",
                preference = scroll,
                commit = commit
            )
        }

        if (columnCount != null) {
            ButtonGroupItem(
                title = "Columns",
                preference = columnCount,
                commit = commit,
            ) { value ->
                when (value) {
                    ColumnCount.AUTO -> "Auto"
                    ColumnCount.ONE -> "1"
                    ColumnCount.TWO -> "2"
                }
            }
        }

        if (pageMargins != null) {
            StepperItem(
                title = "Page margins",
                preference = pageMargins,
                commit = commit
            )
        }

        Divider()
    }

    if (theme != null || textColor != null || imageFilter != null) {

        if (theme != null) {
            ButtonGroupItem(
                title = "Theme",
                preference = theme,
                commit = commit
            ) { value ->
                when (value) {
                    Theme.LIGHT -> "Light"
                    Theme.DARK -> "Dark"
                    Theme.SEPIA -> "Sepia"
                }
            }
        }

        if (imageFilter != null) {
            ButtonGroupItem(
                title = "Image filter",
                preference = imageFilter,
                commit = commit
            ) { value ->
                when (value) {
                    ImageFilter.NONE -> "None"
                    ImageFilter.DARKEN -> "Darken"
                    ImageFilter.INVERT -> "Invert"
                }
            }
        }

        if (textColor != null) {
            ColorItem(
                title = "Text color",
                preference = textColor,
                commit = commit
            )
        }

        if (backgroundColor != null) {
            ColorItem(
                title = "Background color",
                preference = backgroundColor,
                commit = commit
            )
        }

        Divider()
    }

    if (fontFamily != null || fontSize != null || textNormalization != null) {
        if (fontFamily != null) {
            MenuItem(
                title = "Typeface",
                preference = fontFamily,
                commit = commit
            ) { value ->
                when (value) {
                    null -> "Original"
                    FontFamily.SANS_SERIF -> "Sans Serif"
                    else -> value.name
                }
            }
        }

        if (fontSize != null) {
            StepperItem(
                title = "Font size",
                preference = fontSize,
                commit = commit
            )
        }

        if (textNormalization != null) {
            ButtonGroupItem(
                title = "Text normalization",
                preference = textNormalization,
                commit = commit,
            ) { value ->
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
        SwitchItem(
            title = "Publisher styles",
            preference = publisherStyles,
            commit = commit,
        )
    }

    if (textAlign != null) {
        ButtonGroupItem(
            title = "Alignment",
            preference = textAlign,
            commit = commit
        ) { value ->
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
        StepperItem(
            title = "Type scale",
            preference = typeScale,
            commit = commit
        )
    }

    if (lineHeight != null) {
        StepperItem(
            title = "Line height",
            preference = lineHeight,
            commit = commit
        )
    }

    if (paragraphIndent != null) {
        StepperItem(
            title = "Paragraph indent",
            preference = paragraphIndent,
            commit = commit
        )
    }

    if (paragraphSpacing != null) {
        StepperItem(
            title = "Paragraph spacing",
            preference = paragraphSpacing,
            commit = commit
        )
    }

    if (wordSpacing != null) {
        StepperItem(
            title = "Word spacing",
            preference = wordSpacing,
            commit = commit
        )
    }

    if (letterSpacing != null) {
        StepperItem(
            title = "Letter spacing",
            preference = letterSpacing,
            commit = commit
        )
    }

    if (hyphens != null) {
        SwitchItem(
            title = "Hyphens",
            preference = hyphens,
            commit = commit
        )
    }

    if (ligatures != null) {
        SwitchItem(
            title = "Ligatures",
            preference = ligatures,
            commit = commit
        )
    }
}

/**
 * Component for an [EnumPreference] displayed as a group of mutually exclusive buttons.
 * This works best with a small number of enum values.
 */
@Composable
private fun <T> ButtonGroupItem(
    title: String,
    preference: EnumPreference<T>,
    commit: () -> Unit,
    formatValue: (T) -> String
) {
    ButtonGroupItem(
        title = title,
        options = preference.supportedValues,
        isActive = preference.isEffective,
        activeOption = preference.effectiveValue,
        selectedOption = preference.value,
        formatValue = formatValue
    ) { newValue ->
        if (newValue == preference.value) {
            preference.clear()
        } else {
            preference.set(newValue)
        }
        commit()
    }
}

/**
 * Group of mutually exclusive buttons.
 */
@Composable
private fun <T> ButtonGroupItem(
    title: String,
    options: List<T>,
    isActive: Boolean,
    activeOption: T,
    selectedOption: T?,
    formatValue: (T) -> String,
    onSelectedOptionChanged: (T) -> Unit,
) {
    Item(title, isActive = isActive) {
        ToggleButtonGroup(
            options = options,
            activeOption = activeOption,
            selectedOption = selectedOption,
            onSelectOption = { option -> onSelectedOptionChanged(option) }
        ) { option ->
            Text(
                text = formatValue(option),
                style = MaterialTheme.typography.caption
            )
        }
    }
}

/**
 * Component for an [EnumPreference] displayed as a dropdown menu.
 */
@Composable
private fun <T> MenuItem(
    title: String,
    preference: EnumPreference<T>,
    commit: () -> Unit,
    formatValue: (T?) -> String
) {
    MenuItem(
        title = title,
        value = preference.value ?: preference.effectiveValue,
        values = listOf(null) + preference.supportedValues,
        isActive = preference.isEffective,
        formatValue = formatValue
    ) { value ->
        preference.set(value)
        commit()
    }
}

/**
 * Dropdown menu.
 */
@Composable
private fun <T> MenuItem(
    title: String,
    value: T,
    values: List<T>,
    isActive: Boolean,
    formatValue: (T) -> String,
    onValueChanged: (T) -> Unit,
) {
    Item(title, isActive = isActive) {
        DropdownMenuButton(
            text = {
                Text(
                    text = formatValue(value),
                    style = MaterialTheme.typography.caption
                )
            }
        ) { dismiss ->
            for (value in values) {
                DropdownMenuItem(
                    onClick = {
                        dismiss()
                        onValueChanged(value)
                    }
                ) {
                    Text(formatValue(value))
                }
            }
        }
    }
}

/**
 * Component for a [RangePreference] with decrement and increment buttons.
 */
@Composable
private fun <T : Comparable<T>> StepperItem(
    title: String,
    preference: RangePreference<T>,
    commit: () -> Unit
) {
    StepperItem(
        title = title,
        isActive = preference.isEffective,
        value = preference.value ?: preference.effectiveValue,
        formatValue = preference::formatValue,
        onDecrement = { preference.decrement(); commit() },
        onIncrement = { preference.increment(); commit() }
    )
}

/**
 * Component for a [RangePreference] with decrement and increment buttons.
 */
@Composable
private fun <T> StepperItem(
    title: String,
    isActive: Boolean,
    value: T,
    formatValue: (T) -> String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Item(title, isActive = isActive) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                content = {
                    Icon(Icons.Default.Remove, contentDescription = "Less")
                }
            )

            Text(
                text = formatValue(value),
                modifier = Modifier.widthIn(min = 30.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = onIncrement,
                content = {
                    Icon(Icons.Default.Add, contentDescription = "More")
                }
            )
        }
    }
}

/**
 * Component for a boolean [Preference].
 */
@Composable
private fun SwitchItem(
    title: String,
    preference: Preference<Boolean>,
    commit: () -> Unit
) {
    SwitchItem(
        title = title,
        value = preference.value ?: preference.effectiveValue,
        isActive = preference.isEffective,
        onCheckedChange = { preference.set(it); commit() },
        onToggle = { preference.toggle(); commit() }
    )
}

/**
 * Switch
 */
@Composable
private fun SwitchItem(
    title: String,
    value: Boolean,
    isActive: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onToggle: () -> Unit,
) {
    Item(
        title = title,
        isActive = isActive,
        onClick = onToggle
    ) {
        Switch(
            checked = value,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Component for a [Preference<ReadiumColor>].
 */
@Composable
private fun ColorItem(
    title: String,
    preference: Preference<ReadiumColor>,
    commit: () -> Unit
) {
    ColorItem(
        title = title,
        isActive = preference.isEffective,
        value = preference.value ?: preference.effectiveValue,
        noValueSelected = preference.value == null,
        onColorChanged = { preference.set(it); commit() }
    )
}

/**
 * Color picker
 */
@Composable
private fun ColorItem(
    title: String,
    isActive: Boolean,
    value: ReadiumColor,
    noValueSelected: Boolean,
    onColorChanged: (ReadiumColor?) -> Unit
) {
    var isPicking by remember { mutableStateOf(false) }

    Item(
        title = title,
        isActive = isActive,
        onClick = { isPicking = true }
    ) {
        val color = Color(value.int)

        OutlinedButton(
            onClick = { isPicking = true },
            colors = ButtonDefaults.buttonColors(backgroundColor = color)
        ) {
            if (noValueSelected) {
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
                        onColorChanged(ReadiumColor(color))
                    }
                    Button(
                        onClick = {
                            isPicking = false
                            onColorChanged(null)
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
 * Component for a [Preference<Language?>]`.
 */
@Composable
fun LanguageItem(
    preference: Preference<Language?>,
    commit: () -> Unit
) {
    val languages = remember {
        Locale.getAvailableLocales()
            .map { Language(it).removeRegion() }
            .distinct()
            .sortedBy { it.locale.displayName }
    }

    MenuItem(
        title = "Language",
        isActive = preference.isEffective,
        value = preference.value ?: preference.effectiveValue,
        values = languages,
        formatValue = { it?.locale?.displayName ?: "Unknown" }
    ) { value ->
        preference.set(value)
        commit()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Item(
    title: String,
    isActive: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
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

@Composable
private fun PresetsMenuButton(
    presets: List<Preset>,
    clear: () -> Unit,
    commit: () -> Unit,
) {
    if (presets.isEmpty()) return

    DropdownMenuButton(
        text = { Text("Presets") }
    ) { dismiss ->

        for (preset in presets) {
            DropdownMenuItem(
                onClick = {
                    dismiss()
                    clear()
                    preset.apply()
                    commit()
                }
            ) {
                Text(preset.title)
            }
        }
    }
}

/**
 * A preset is a named group of settings applied together.
 */

/**
 * A preset is a named group of settings applied together.
 */
class Preset(
    val title: String,
    val apply: () -> Unit
)

/**
 * Returns the presets associated with the [Configurable.Settings] receiver.
 */
val <P : Configurable.Preferences<P>> PreferencesEditor<P>.presets: List<Preset> get() =
    when (this) {
        is EpubPreferencesEditor ->
            when (layout) {
                EpubLayout.FIXED -> emptyList()
                EpubLayout.REFLOWABLE -> listOf(
                    Preset("Increase legibility") {
                        wordSpacing.set(0.6)
                        fontSize.set(1.4)
                        textNormalization.set(TextNormalization.ACCESSIBILITY)
                    },
                    Preset("Document") {
                        scroll.set(true)
                    },
                    Preset("Ebook") {
                        scroll.set(false)
                    },
                    Preset("Manga") {
                        scroll.set(false)
                        readingProgression.set(ReadingProgression.RTL)
                    }
                )
            }
        else ->
            emptyList()
    }
