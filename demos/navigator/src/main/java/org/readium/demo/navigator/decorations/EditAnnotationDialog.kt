/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.decorations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

class EditAnnotationViewModel(
    val id: Long,
    val highlightsManager: HighlightsManager<*>,
) {
    val originalHighlight = checkNotNull(highlightsManager.highlights.value[id])

    val targetedText = checkNotNull(originalHighlight.locator.text.highlight)

    val tint = originalHighlight.tint

    fun updateAnnotation(annotation: String) {
        highlightsManager.updateHighlightAnnotation(id = id, annotation = annotation)
    }
}

@Composable
fun EditAnnotationDialog(
    viewModel: EditAnnotationViewModel,
    onDismissRequest: () -> Unit,
) {
    EditAnnotationDialog(
        dialogProperties = DialogProperties(),
        onDismissRequest = onDismissRequest,
        onConfirmation = {
            viewModel.updateAnnotation(it)
            onDismissRequest()
        },
        targetedText = viewModel.targetedText,
        highlightTint = Color(viewModel.tint)
    )
}

@Composable
fun EditAnnotationDialog(
    dialogProperties: DialogProperties,
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    targetedText: String,
    highlightTint: Color,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .requiredWidth(4.dp)
                            .fillMaxHeight()
                            .background(highlightTint)
                    )
                    Text(
                        text = targetedText,
                        modifier = Modifier
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                        .fillMaxWidth()
                )

                val textFieldState = rememberTextFieldState()

                BasicTextField(
                    state = textFieldState,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = { onConfirmation(textFieldState.text.toString()) },
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
