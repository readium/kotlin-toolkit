/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.demo.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.readium.adapter.pdfium.navigator.PdfiumPreferencesEditor
import org.readium.navigator.web.preferences.FixedWebPreferencesEditor
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Stateful user settings component.
 */
@Composable
fun UserPreferences(
    model: UserPreferencesViewModel<*, *>,
    title: String
) {
    val editor by model.editor.collectAsState()

    UserPreferences(
        editor = editor,
        commit = model::commit,
        title = title
    )
}

@Composable
private fun <P : Configurable.Preferences<P>, E : PreferencesEditor<P>> UserPreferences(
    editor: E,
    commit: () -> Unit,
    title: String
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { editor.clear(); commit() }
            ) {
                Text("Reset")
            }
        }

        Divider()

        when (editor) {
            is FixedWebPreferencesEditor ->
                FixedLayoutUserPreferences(
                    commit = commit,
                    readingProgression = editor.readingProgression,
                    fit = editor.fit,
                    spreads = editor.spreads
                )
            is PdfiumPreferencesEditor ->
                FixedLayoutUserPreferences(
                    commit = commit,
                    readingProgression = editor.readingProgression,
                    scrollAxis = editor.scrollAxis,
                    fit = editor.fit,
                    pageSpacing = editor.pageSpacing
                )
        }
    }
}

/**
 * User preferences for a publication with a fixed layout, such as fixed-layout EPUB, PDF or comic book.
 */
@Composable
private fun FixedLayoutUserPreferences(
    commit: () -> Unit,
    readingProgression: EnumPreference<ReadingProgression>? = null,
    scrollAxis: EnumPreference<Axis>? = null,
    fit: EnumPreference<Fit>? = null,
    spreads: Preference<Boolean>? = null,
    offsetFirstPage: Preference<Boolean>? = null,
    pageSpacing: RangePreference<Double>? = null
) {
    if (readingProgression != null) {
        ButtonGroupItem(
            title = "Reading progression",
            preference = readingProgression,
            commit = commit,
            formatValue = { it.name }
        )

        Divider()
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

    if (spreads != null) {
        SwitchItem(
            title = "Spreads",
            preference = spreads,
            commit = commit
        )

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

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
}
