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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

    val content = originalHighlight.annotation

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
        initialContent = viewModel.content,
        highlightTint = Color(viewModel.tint)
    )
}

@Composable
private fun EditAnnotationDialog(
    dialogProperties: DialogProperties,
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    targetedText: String,
    initialContent: String,
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
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .requiredWidth(4.dp)
                                    .fillMaxHeight()
                                    .background(highlightTint)

                            )
                        },
                        value = targetedText.substring(0, 50.coerceAtMost(targetedText.length)),
                        onValueChange = {},
                        readOnly = true,
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                        .fillMaxWidth()
                )

                var content by rememberSaveable { mutableStateOf(initialContent) }

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    value = content,
                    onValueChange = { content = it }
                )

                Row(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = { onConfirmation(content) },
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
