/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.presentation.PresentationController
import org.readium.r2.navigator.presentation.PresentationRange
import org.readium.r2.navigator.presentation.supportedValues
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation.*
import org.readium.r2.testapp.utils.compose.DropdownMenuButton
import org.readium.r2.testapp.utils.compose.ToggleButtonGroup

@OptIn(ExperimentalPresentation::class)
typealias UpdatePresentation = PresentationController.(PresentationController.Settings) -> Unit

@OptIn(ExperimentalPresentation::class)
typealias CommitPresentation = (UpdatePresentation) -> Unit

@Composable
@OptIn(ExperimentalPresentation::class)
fun UserSettingsView(presentation: PresentationController) {
    val settings by presentation.settings.collectAsState()
    UserSettingsView(
        settings = settings,
        onCommit = { presentation.commit(it) }
    )
}

@Composable
@OptIn(ExperimentalPresentation::class)
private fun UserSettingsView(settings: PresentationController.Settings, onCommit: CommitPresentation) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User settings",
            style = MaterialTheme.typography.subtitle1,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PresetsMenuButton(onCommit = onCommit)
            Button(
                onClick = {
                    onCommit { reset() }
                },
            ) {
                Text("Reset")
            }
        }

        EnumSetting("Reading progression", settings.readingProgression, onCommit)
        EnumSetting("Fit", settings.fit, onCommit)
        EnumSetting("Overflow", settings.overflow, onCommit)
        EnumSetting("Orientation", settings.orientation, onCommit)
        RangeSection("Page spacing", settings.pageSpacing, onCommit)
    }
}

@ExperimentalPresentation
@Composable
private fun PresetsMenuButton(onCommit: CommitPresentation) {
    DropdownMenuButton(
        text = { Text("Presets") }
    ) { dismiss ->
        @Composable fun item(title: String, updates: UpdatePresentation) {
            DropdownMenuItem(
                onClick = {
                    onCommit(updates)
                    dismiss()
                }
            ) {
                Text(title)
            }
        }

        item(
            title = "Document",
            updates = { settings ->
                set(settings.readingProgression, ReadingProgression.TTB)
                set(settings.overflow, Overflow.SCROLLED)
            }
        )
        item(
            title = "Ebook",
            updates = { settings ->
                set(settings.readingProgression, ReadingProgression.LTR)
                set(settings.overflow, Overflow.PAGINATED)
            }
        )
        item(
            title = "Manga",
            updates = { settings ->
                set(settings.readingProgression, ReadingProgression.RTL)
                set(settings.overflow, Overflow.PAGINATED)
            }
        )
    }
}

@Composable
@OptIn(ExperimentalPresentation::class)
private fun <T : Enum<T>> EnumSetting(title: String, setting: PresentationController.Setting<T>?, onCommit: CommitPresentation) {
    setting ?: return
    val values = setting.supportedValues ?: return

    Section(title) {
        ToggleButtonGroup(
            options = values,
            activeOption = setting.effectiveValue,
            selectedOption = setting.value,
            onSelectOption = { value ->
                onCommit {
                    toggle(setting, value)
                }
            }) { option ->
            Text(option.name)
        }
    }
}

@Composable
@OptIn(ExperimentalPresentation::class)
fun RangeSection(title: String, setting: PresentationController.Setting<PresentationRange>?, onCommit: CommitPresentation) {
    setting ?: return

    Section(title, isActive = setting.isActive) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    onCommit {
                        decrement(setting)
                    }
                },
                content = {
                    Icon(Icons.Default.Remove, contentDescription = "Less")
                }
            )

            (setting.value ?: setting.effectiveValue)?.let { value ->
                Text(value.percentageString)
            }

            IconButton(
                onClick = {
                    onCommit {
                        increment(setting)
                    }
                },
                content = {
                    Icon(Icons.Default.Add, contentDescription = "Plus")
                }
            )
        }
    }
}

@Composable
private fun Section(title: String, isActive: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val alpha = if (isActive) 1.0f else ContentAlpha.disabled
    CompositionLocalProvider(LocalContentAlpha provides alpha) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
            )
            content()
        }
    }
}
