/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.demo.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.readium.navigator.demo.util.ToggleButtonGroup
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.clear
import org.readium.r2.navigator.preferences.toggle

/**
 * Component for an [EnumPreference] displayed as a group of mutually exclusive buttons.
 * This works best with a small number of enum values.
 */
@Composable
fun <T> ButtonGroupItem(
    title: String,
    preference: EnumPreference<T>,
    formatValue: (T) -> String
) {
    ButtonGroupItem(
        title = title,
        options = preference.supportedValues,
        isActive = preference.isEffective,
        activeOption = preference.effectiveValue,
        selectedOption = preference.value,
        formatValue = formatValue,
        onClear = { preference.clear() }
            .takeIf { preference.value != null },
        onSelectedOptionChanged = { newValue ->
            if (newValue == preference.value) {
                preference.clear()
            } else {
                preference.set(newValue)
            }
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
    onSelectedOptionChanged: (T) -> Unit
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
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Component for a [RangePreference] with decrement and increment buttons.
 */
@Composable
fun <T : Comparable<T>> StepperItem(
    title: String,
    preference: RangePreference<T>
) {
    StepperItem(
        title = title,
        isActive = preference.isEffective,
        value = preference.value ?: preference.effectiveValue,
        formatValue = preference::formatValue,
        onDecrement = { preference.decrement() },
        onIncrement = { preference.increment() },
        onClear = { preference.clear() }
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
    onClear: (() -> Unit)?
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
    preference: Preference<Boolean>
) {
    SwitchItem(
        title = title,
        value = preference.value ?: preference.effectiveValue,
        isActive = preference.isEffective,
        onCheckedChange = { preference.set(it) },
        onToggle = { preference.toggle() },
        onClear = { preference.clear() }
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
    onClear: (() -> Unit)?
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

@Composable
private fun Item(
    title: String,
    isActive: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ListItem(
        modifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        },
        headlineContent = {
            val alpha = if (isActive) 1.0f else 0.5f
            Text(title, modifier = Modifier.alpha(alpha))
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
