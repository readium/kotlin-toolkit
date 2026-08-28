/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

@Composable
fun ColorPicker(onPick: (Int) -> Unit) {
    val colors = listOf(
        // Gray
        listOf("#ffffff", "#bfbfbf", "#808080", "#404040", "#000000"),
        // Sepia
        listOf("#FAF4E8", "#CDBEAE", "#A08774", "#73513A", "#461A00"),
        // Brown
        listOf("#d7ccc8", "#a1887f", "#795548", "#5d4037", "#3e2723"),
        // Red
        listOf("#ffcdd2", "#e57373", "#f44336", "#d32f2f", "#b71c1c"),
        // Purple
        listOf("#e1bee7", "#ba68c8", "#9c27b0", "#7b1fa2", "#4a148c"),
        // Blue
        listOf("#bbdefb", "#64b5f6", "#2196f3", "#1976d2", "#0d47a1"),
        // Green
        listOf("#c8e6c9", "#81c784", "#4caf50", "#388e3c", "#1b5e20"),
        // Yellow
        listOf("#fff9c4", "#fff176", "#ffeb3b", "#fbc02d", "#f57f17"),
        // Orange
        listOf("#ffe0b2", "#ffb74d", "#ff9800", "#f57c00", "#e65100")
    )

    Column {
        for (row in colors) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                for (color in row) {
                    ColorBox(color, onClick = onPick)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ColorBox(colorHex: String, onClick: (Int) -> Unit) {
    val color = remember(colorHex) { colorHex.toColorInt() }

    Box(
        modifier = Modifier
            .clickable { onClick(color) }
            .weight(1f)
            .size(40.dp)
            .background(Color(color))
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewColorPicker() {
    ColorPicker {}
}
