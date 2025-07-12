/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.decorations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

class EditHighlightViewModel(
    val id: Long,
    val contentRect: DpRect,
    val highlightsManager: HighlightsManager<*>,
) {
    fun updateTint(tint: Int) {
        highlightsManager.updateHighlightStyle(id = id, tint = tint)
    }

    fun delete() {
        highlightsManager.deleteHighlight(id)
    }
}

@Composable
fun EditHighlightPopup(
    viewModel: EditHighlightViewModel,
    onDismissRequest: () -> Unit,
    onEditNoteRequest: () -> Unit,
) {
    val offset = with(LocalDensity.current) {
        IntOffset(
            x = viewModel.contentRect.left.toPx().roundToInt(),
            y = viewModel.contentRect.top.toPx().roundToInt()
        )
    }

    EditHighlightPopup(
        offset = offset,
        popupProperties = PopupProperties(),
        onDismissRequest = onDismissRequest,
        onColorSelected = { color ->
            viewModel.updateTint(color.toArgb())
            onDismissRequest()
        },
        onEditNote = {
            onDismissRequest()
            onEditNoteRequest()
        },
        onDelete = {
            viewModel.delete()
            onDismissRequest()
        }
    )
}

@Composable
private fun EditHighlightPopup(
    offset: IntOffset,
    popupProperties: PopupProperties,
    availableColors: List<Color> = listOf(Color.Yellow, Color.Green, Color.Blue, Color.Red),
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
) {
    Popup(
        offset = offset,
        onDismissRequest = onDismissRequest,
        properties = popupProperties
    ) {
        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .width(IntrinsicSize.Min)
        ) {
            Row {
                TintSelector(
                    availableColors = availableColors,
                    onColorSelected = onColorSelected
                )

                VerticalDivider()

                IconButton(
                    onClick = onEditNote
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Edit note"
                    )
                }
                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete highlight"
                    )
                }
            }
        }
    }
}

@Composable
private fun TintSelector(
    modifier: Modifier = Modifier,
    availableColors: List<Color>,
    onColorSelected: (Color) -> Unit,
) {
    Row(modifier.selectableGroup()) {
        for (color in availableColors) {
            IconButton(
                onClick = { onColorSelected(color) },
                content = {
                    Box(
                        modifier = Modifier
                            .requiredSize(24.dp)
                            .background(color, CircleShape),
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun TintSelectorPreview() {
    TintSelector(
        availableColors = listOf(Color.Red, Color.Green, Color.Blue),
        onColorSelected = {}
    )
}
