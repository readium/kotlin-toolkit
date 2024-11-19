/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.shared.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.*
import org.readium.r2.navigator.preferences.*
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.utils.compose.ColorPicker
import org.readium.r2.testapp.utils.compose.DropdownMenuButton
import org.readium.r2.testapp.utils.compose.ToggleButtonGroup

/**
 * Component for an [EnumPreference] displayed as a group of mutually exclusive buttons.
 * This works best with a small number of enum values.
 */
@Composable
fun <T> ButtonGroupItem(
    title: String,
    preference: EnumPreference<T>,
    commit: () -> Unit,
    formatValue: (T) -> String,
) {
    ButtonGroupItem(
        title = title,
        options = preference.supportedValues,
        isActive = preference.isEffective,
        activeOption = preference.effectiveValue,
        selectedOption = preference.value,
        formatValue = formatValue,
        onClear = {
            preference.clear()
            commit()
        }
            .takeIf { preference.value != null },
        onSelectedOptionChanged = { newValue ->
            if (newValue == preference.value) {
                preference.clear()
            } else {
                preference.set(newValue)
            }
            commit()
        }
    )
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
    onClear: (() -> Unit)?,
    onSelectedOptionChanged: (T) -> Unit,
) {
    Item(title, isActive = isActive, onClear = onClear) {
        ToggleButtonGroup(
            options = options,
            activeOption = activeOption,
            selectedOption = selectedOption,
            onSelectOption = { option -> onSelectedOptionChanged(option) }
        ) { option ->
            Text(
                text = formatValue(option),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Component for an [EnumPreference] displayed as a dropdown menu.
 */
@Composable
fun <T> MenuItem(
    title: String,
    preference: EnumPreference<T>,
    commit: () -> Unit,
    formatValue: (T) -> String,
) {
    MenuItem(
        title = title,
        value = preference.value ?: preference.effectiveValue,
        values = preference.supportedValues,
        isActive = preference.isEffective,
        formatValue = formatValue,
        onClear = {
            preference.clear()
            commit()
        }
            .takeIf { preference.value != null },
        onValueChanged = { value ->
            preference.set(value)
            commit()
        }
    )
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
    onClear: (() -> Unit)?,
) {
    Item(title, isActive = isActive, onClear = onClear) {
        DropdownMenuButton(
            text = {
                Text(
                    text = formatValue(value),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        ) { dismiss ->
            for (aValue in values) {
                DropdownMenuItem(
                    text = { Text(formatValue(aValue)) },
                    onClick = {
                        dismiss()
                        onValueChanged(aValue)
                    }
                )
            }
        }
    }
}

/**
 * Component for a [RangePreference] with decrement and increment buttons.
 */
@Composable
fun <T : Comparable<T>> StepperItem(
    title: String,
    preference: RangePreference<T>,
    commit: () -> Unit,
) {
    StepperItem(
        title = title,
        isActive = preference.isEffective,
        value = preference.value ?: preference.effectiveValue,
        formatValue = preference::formatValue,
        onDecrement = {
            preference.decrement()
            commit()
        },
        onIncrement = {
            preference.increment()
            commit()
        },
        onClear = {
            preference.clear()
            commit()
        }
            .takeIf { preference.value != null }
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
    onClear: (() -> Unit)?,
) {
    Item(title, isActive = isActive, onClear = onClear) {
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
fun SwitchItem(
    title: String,
    preference: Preference<Boolean>,
    commit: () -> Unit,
) {
    SwitchItem(
        title = title,
        value = preference.value ?: preference.effectiveValue,
        isActive = preference.isEffective,
        onCheckedChange = {
            preference.set(it)
            commit()
        },
        onToggle = {
            preference.toggle()
            commit()
        },
        onClear = {
            preference.clear()
            commit()
        }
            .takeIf { preference.value != null }
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
    onClear: (() -> Unit)?,
) {
    Item(
        title = title,
        isActive = isActive,
        onClick = onToggle,
        onClear = onClear
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
fun ColorItem(
    title: String,
    preference: Preference<ReadiumColor>,
    commit: () -> Unit,
) {
    ColorItem(
        title = title,
        isActive = preference.isEffective,
        value = preference.value ?: preference.effectiveValue,
        noValueSelected = preference.value == null,
        onColorChanged = {
            preference.set(it)
            commit()
        },
        onClear = {
            preference.clear()
            commit()
        }
            .takeIf { preference.value != null }
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
    onColorChanged: (ReadiumColor?) -> Unit,
    onClear: (() -> Unit)?,
) {
    var isPicking by remember { mutableStateOf(false) }

    Item(
        title = title,
        isActive = isActive,
        onClick = { isPicking = true },
        onClear = onClear
    ) {
        val color = Color(value.int)

        OutlinedButton(
            onClick = { isPicking = true },
            colors = ButtonDefaults.buttonColors(containerColor = color)
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
    commit: () -> Unit,
) {
    val languages = remember {
        Locale.getAvailableLocales()
            .map { Language(it).removeRegion() }
            .distinct()
            .sortedBy { it.locale.displayName }
    }

    MenuItem(
        title = "Language",
        preference = preference.withSupportedValues(languages + null),
        formatValue = { it?.locale?.displayName ?: "Unknown" },
        commit = commit
    )
}

@Composable
private fun Item(
    title: String,
    isActive: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ListItem(
        modifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        },
        headlineContent = {
            Group(enabled = isActive) {
                Text(title)
            }
        },
        trailingContent = {
            Row {
                content()

                IconButton(onClick = onClear ?: {}, enabled = onClear != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Clear"
                    )
                }
            }
        }
    )
}

@Composable
fun <T> SelectorListItem(
    title: String,
    preference: EnumPreference<T>,
    formatValue: (T) -> String,
    commit: () -> Unit,
) {
    SelectorListItem(
        title = title,
        values = preference.supportedValues,
        selection = preference.value ?: preference.effectiveValue,
        formatValue = formatValue,
        onSelected = {
            preference.set(it)
            commit()
        }
    )
}

/**
 * A Material [ListItem] displaying a dropdown menu to select a value. The current value is
 * displayed on the right.
 */
@Composable
private fun <T> SelectorListItem(
    title: String,
    values: List<T>,
    selection: T,
    formatValue: (T) -> String,
    onSelected: (T) -> Unit,
    enabled: Boolean = values.isNotEmpty(),
) {
    var isExpanded by remember { mutableStateOf(false) }
    fun dismiss() {
        isExpanded = false
    }

    ListItem(
        modifier = Modifier
            .clickable(enabled = enabled) {
                isExpanded = true
            },
        headlineContent = {
            Group(enabled = enabled) {
                Text(title)
            }
        },
        trailingContent = {
            Group(enabled = enabled) {
                Text(formatValue(selection))
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { dismiss() }
            ) {
                for (value in values) {
                    DropdownMenuItem(
                        text = { Text(formatValue(value)) },
                        onClick = {
                            onSelected(value)
                            dismiss()
                        }
                    )
                }
            }
        }
    )
}
