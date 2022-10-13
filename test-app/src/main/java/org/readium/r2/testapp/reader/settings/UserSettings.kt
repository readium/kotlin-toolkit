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
import org.readium.adapters.pspdfkit.navigator.PsPdfKitSettingsValues
import org.readium.r2.navigator.epub.EpubSettingsValues
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

/**
 * Stateful user settings component paired with a [ReaderViewModel].
 */
@Composable
fun UserSettings(model: UserSettingsViewModel) {
    val settings = model.settings.collectAsState().value ?: return
    val editor = model.editor.collectAsState().value ?: return

    UserSettings(
        settings = settings,
        editor = editor,
        presets = model.presets
    )
}

/**
 * Stateless user settings component displaying the given [settings] and setting user [preferences],
 * using the [editNavigator] and [editPublication] closures.
 */
@Composable
fun UserSettings(
    settings: Configurable.Settings,
    editor: PreferencesEditor,
    presets: List<UserSettingsViewModel.Preset>
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
            PresetsMenuButton(editor = editor, presets = presets)

            Button(
                onClick = editor::clearPreferences
            ) {
                Text("Reset")
            }
        }

        Divider()

        when (settings) {
            is PsPdfKitSettingsValues ->
                FixedLayoutUserSettings(
                    effectiveLanguage = null,
                    languageEditor = null,
                    effectiveReadingProgression = settings.readingProgression,
                    readingProgressionEditor = editor as? ReadingProgressionEditor,
                    effectiveOffset = settings.offset,
                    offsetEditor = editor as? OffsetEditor,
                    effectiveScroll = settings.scroll,
                    scrollEditor = editor as? ScrollEditor,
                    effectiveScrollAxis = settings.scrollAxis,
                    scrollAxisEditor = editor as? ScrollAxisEditor,
                    effectiveFit = settings.fit,
                    fitEditor = editor as? FitEditor,
                    effectiveSpread = settings.spread,
                    spreadEditor = editor as? SpreadEditor,
                    effectivePageSpacing = settings.pageSpacing,
                    pageSpacingEditor = editor as? PageSpacingEditor
                )

            is EpubSettingsValues.FixedLayout ->
                FixedLayoutUserSettings(
                    effectiveLanguage = settings.language,
                    languageEditor = editor as? LanguageEditor,
                    effectiveReadingProgression = settings.readingProgression,
                    readingProgressionEditor = editor as? ReadingProgressionEditor,
                    fitEditor = editor as? FitEditor,
                    effectiveSpread = settings.spread,
                    spreadEditor = editor as? SpreadEditor,
                )

            /*is EpubSettingsValues.Reflowable ->
                ReflowableUserSettings(
                    preferences = preferences,
                    editNavigator = editNavigator,
                    editPublication = editPublication,
                    settings = settings
                )*/
        }
    }
}

/**
 * User settings for a publication with a fixed layout, such as fixed-layout EPUB, PDF or comic book.
 */
@Composable
private fun ColumnScope.FixedLayoutUserSettings(
    effectiveReadingProgression: ReadingProgression? = null,
    readingProgressionEditor: ReadingProgressionEditor? = null,
    effectiveLanguage: Language? = null,
    languageEditor: LanguageEditor? = null,
    effectiveScroll: Boolean? = null,
    scrollEditor: ScrollEditor? = null,
    effectiveScrollAxis: Axis? = null,
    scrollAxisEditor: ScrollAxisEditor? = null,
    effectiveSpread: Spread? = null,
    spreadEditor: SpreadEditor? = null,
    effectiveOffset: Boolean? = null,
    offsetEditor: OffsetEditor? = null,
    effectivePageSpacing: Double? = null,
    pageSpacingEditor: PageSpacingEditor? = null,
    effectiveFit: Fit? = null,
    fitEditor: FitEditor? = null,
) {
    if (languageEditor != null || readingProgressionEditor != null) {
        fun reset() {
            languageEditor?.language = null
            readingProgressionEditor?.readingProgression = null
        }

        if (languageEditor != null) {
            LanguageItem(
                value = languageEditor.language ?: effectiveLanguage,
                isActive = languageEditor.isLanguagePreferenceActive
            ) { value ->
                languageEditor.language = value

            }
        }

        if (readingProgressionEditor != null && effectiveReadingProgression != null) {
            ButtonGroupItem(
                title = "Reading progression",
                options = readingProgressionEditor.supportedReadingProgressionValues,
                activeOption = effectiveReadingProgression,
                isActive = readingProgressionEditor.isReadingProgressionPreferenceActive,
                selectedOption = readingProgressionEditor.readingProgression,
                onOptionChanged = { value -> readingProgressionEditor.readingProgression = value }
            ) { value ->
                when (value) {
                    ReadingProgression.AUTO -> "Auto"
                    else -> value.name
                }
            }
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

    if (scrollEditor != null && effectiveScroll != null) {
        SwitchItem(
            title = "Scroll",
            value = scrollEditor.scroll ?: effectiveScroll,
            isActive = scrollEditor.isScrollPreferenceActive,
            onCheckedChange = { value -> scrollEditor.scroll = value },
            onToggle =  {  }
        )
    }

    if (scrollAxisEditor != null && effectiveScrollAxis != null) {
        ButtonGroupItem(
            title = "Scroll axis",
            options = scrollAxisEditor.supportedScrollAxes,
            isActive = scrollAxisEditor.isScrollAxisPreferenceActive,
            activeOption = effectiveScrollAxis,
            selectedOption = scrollAxisEditor.scrollAxis,
            onOptionChanged = { scrollAxisEditor.scrollAxis = it }
        ) { value ->
            when (value) {
                Axis.HORIZONTAL-> "Horizontal"
                Axis.VERTICAL -> "Vertical"
            }
        }
    }

    if (spreadEditor != null && effectiveSpread != null) {
        ButtonGroupItem(
            title = "Spread",
            options = spreadEditor.supportedSpreadValues,
            isActive = spreadEditor.isSpreadPreferenceActive,
            activeOption = effectiveSpread,
            selectedOption = spreadEditor.spread,
            onOptionChanged = { spreadEditor.spread = it }
        ) { value ->
            when (value) {
                Spread.AUTO -> "Auto"
                Spread.NEVER -> "Never"
                Spread.PREFERRED -> "Preferred"
            }
        }

        if (offsetEditor != null && effectiveOffset != null) {
            SwitchItem(
                title = "Offset",
                value = effectiveOffset,
                isActive = offsetEditor.isOffsetPreferenceActive,
                onCheckedChange = { offsetEditor.offset = it },
                onToggle = { offsetEditor.toggleOffset() }
            )
        }
    }

    if (fitEditor != null && effectiveFit != null) {
        ButtonGroupItem(
            title = "Fit",
            options = fitEditor.supportedFitValues,
            isActive = fitEditor.isFitPreferenceActive,
            activeOption = effectiveFit,
            selectedOption = fitEditor.fit,
            onOptionChanged = { fitEditor.fit = it }
        ) { value ->
            when (value) {
                Fit.CONTAIN-> "Contain"
                Fit.COVER -> "Cover"
                Fit.WIDTH -> "Width"
                Fit.HEIGHT -> "Height"
            }
        }
    }

    if (pageSpacingEditor != null && effectivePageSpacing != null) {
        StepperItem(
            title = "Page spacing",
            isActive = pageSpacingEditor.isPageSpacingPreferenceActive,
            value = pageSpacingEditor.pageSpacing ?: effectivePageSpacing,
            decrement = pageSpacingEditor::decrementPageSpacing,
            increment = pageSpacingEditor::incrementPageSpacing,
            formatValue = pageSpacingEditor::formatPageSpacing
        )
    }
}
/*
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
*/

/**
 * Component for an [EnumSetting] displayed as a group of mutually exclusive buttons.
 * This works best with a small number of enum values.
 */
@Composable
private fun <T> ButtonGroupItem(
    title: String,
    options: List<T>? = null,
    isActive: Boolean,
    activeOption: T,
    selectedOption: T?,
    onOptionChanged: (T) -> Unit,
    formatValue: (T) -> String
) {
    Item(title, isActive = isActive) {
        ToggleButtonGroup(
            options = options ?: emptyList(),
            activeOption = activeOption,
            selectedOption = selectedOption,
            onSelectOption = { option -> onOptionChanged(option)
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
 * Component displayed as a dropdown menu.
 */
@Composable
private fun <T> MenuItem(
    title: String,
    value: T,
    values: List<T>,
    isActive: Boolean,
    edit: (T) -> Unit,
    formatValue: (T) -> String
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
                        edit(value)
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
    isActive: Boolean,
    value: Double,
    decrement: () -> Unit,
    increment: () -> Unit,
    formatValue: (Double) -> String
) {
    Item(title, isActive = isActive) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = decrement,
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
                onClick = increment,
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
 * Component for a [Setting<ReadiumColor>].
 */
@Composable
private fun ColorItem(
    title: String,
    isActive: Boolean,
    value: ReadiumColor,
    valueHasBeenSelected: Boolean,
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
            if (valueHasBeenSelected) {
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
 * Component for a `Setting<Language?>`.
 */
@Composable
fun LanguageItem(
    value: Language?,
    isActive: Boolean,
    edit: (Language?) -> Unit
) {
    val languages = remember {
        Locale.getAvailableLocales()
            .map { Language(it).removeRegion() }
            .distinct()
            .sortedBy { it.locale.displayName }
    }

    MenuItem(
        title = "Language",
        isActive = isActive,
        value = value,
        values = listOf(null) + languages,
        edit = edit,
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

@Composable
private fun PresetsMenuButton(editor: PreferencesEditor, presets: List<UserSettingsViewModel.Preset>) {
    if (presets.isEmpty()) return

    DropdownMenuButton(
        text = { Text("Presets") }
    ) { dismiss ->

        for (preset in presets) {
            DropdownMenuItem(
                onClick = {
                    dismiss()
                    editor.clearPreferences()
                    preset.changes(editor)
                }
            ) {
                Text(preset.title)
            }
        }
    }
}

