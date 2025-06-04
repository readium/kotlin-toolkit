/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.util

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// A very basic implementation of Material Design's Toggle Button for Compose.
// https://material.io/components/buttons#toggle-button

@Composable
fun <T> ToggleButtonGroup(
    options: List<T>,
    activeOption: T?,
    selectedOption: T?,
    onSelectOption: (T) -> Unit,
    enabled: (T) -> Boolean = { true },
    content: @Composable RowScope.(T) -> Unit,
) {
    Row {
        for (option in options) {
            ToggleButton(
                enabled = enabled(option),
                active = activeOption == option,
                selected = selectedOption == option,
                onClick = { onSelectOption(option) },
                content = { content(option) }
            )
        }
    }
}

@Composable
fun ToggleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        content = content,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground,
            containerColor = when {
                selected ->
                    MaterialTheme.colorScheme.onBackground
                        .copy(alpha = 0.15f)
                        .compositeOver(MaterialTheme.colorScheme.background)
                active ->
                    MaterialTheme.colorScheme.onBackground
                        .copy(alpha = 0.05f)
                        .compositeOver(MaterialTheme.colorScheme.background)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation =
        if (selected) {
            ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        } else {
            null
        }
    )
}

@Composable
@Preview(showBackground = true)
fun PreviewToggleButtonGroup() {
    ToggleButtonGroup(
        options = listOf("1", "2", "3"),
        activeOption = "2",
        selectedOption = "2",
        onSelectOption = {}
    ) { option ->
        Text(text = option)
    }
}
