/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.preferences

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.readium.demo.navigator.reader.LITERATA
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
import org.readium.r2.navigator.preferences.withSupportedValues
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Stateful user settings component.
 */
@Composable
fun <P : Preferences<P>, S : Settings, V : PreferencesViewModel<P, S>> UserPreferences(
    viewModel: V,
    title: String,
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
                onClick = { viewModel.clear() }
            ) {
                Text("Reset")
            }
        }

        Divider()

        when (viewModel) {
            is FixedPreferencesViewModel ->
                FixedLayoutUserPreferences(
                    readingProgression = viewModel.readingProgression,
                    fit = viewModel.fit,
                    spreads = viewModel.spreads
                )
            is ReflowablePreferencesViewModel ->
                ReflowableUserPreferences(
                    backgroundColor = viewModel.backgroundColor,
                    columnCount = viewModel.columnCount,
                    fontFamily = viewModel.fontFamily,
                    fontSize = viewModel.fontSize,
                    fontWeight = viewModel.fontWeight,
                    horizontalMargins = viewModel.minMargins,
                    hyphens = viewModel.hyphens,
                    imageFilter = viewModel.imageFilter,
                    language = viewModel.language,
                    letterSpacing = viewModel.letterSpacing,
                    ligatures = viewModel.ligatures,
                    lineHeight = viewModel.lineHeight,
                    linkColor = viewModel.linkColor,
                    maximalLineLength = viewModel.maximalLineLength,
                    minimalLineLength = viewModel.minimalLineLength,
                    optimalLineLength = viewModel.optimalLineLength,
                    overridePublisherColors = viewModel.overridePublisherColors,
                    paragraphIndent = viewModel.paragraphIndent,
                    paragraphSpacing = viewModel.paragraphSpacing,
                    readingProgression = viewModel.readingProgression,
                    scroll = viewModel.scroll,
                    textAlign = viewModel.textAlign,
                    textColor = viewModel.textColor,
                    textNormalization = viewModel.textNormalization,
                    verticalText = viewModel.verticalText,
                    visitedColor = viewModel.visitedColor,
                    wordSpacing = viewModel.wordSpacing
                )
        }
    }
}

/**
 * User preferences for a publication with a fixed layout, such as fixed-layout EPUB, PDF or comic book.
 */
@Composable
private fun FixedLayoutUserPreferences(
    readingProgression: EnumPreference<ReadingProgression>? = null,
    scrollAxis: EnumPreference<Axis>? = null,
    fit: EnumPreference<Fit>? = null,
    spreads: Preference<Boolean>? = null,
    offsetFirstPage: Preference<Boolean>? = null,
    pageSpacing: RangePreference<Double>? = null,
) {
    if (readingProgression != null) {
        ButtonGroupItem(
            title = "Reading progression",
            preference = readingProgression,
            formatValue = { it.name }
        )

        Divider()
    }

    if (scrollAxis != null) {
        ButtonGroupItem(
            title = "Scroll axis",
            preference = scrollAxis
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
            preference = spreads
        )

        if (offsetFirstPage != null) {
            SwitchItem(
                title = "Offset",
                preference = offsetFirstPage
            )
        }
    }

    if (fit != null) {
        ButtonGroupItem(
            title = "Fit",
            preference = fit
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
            preference = pageSpacing
        )
    }
}

/**
 * User settings for a publication with adjustable fonts and dimensions, such as
 * a reflowable EPUB, HTML document or PDF with reflow mode enabled.
 */
@Composable
private fun ReflowableUserPreferences(
    backgroundColor: Preference<Color>? = null,
    columnCount: Preference<Int?>? = null,
    fontFamily: Preference<FontFamily?>? = null,
    fontSize: RangePreference<Double>? = null,
    fontWeight: OptionalRangePreference<Double>? = null,
    horizontalMargins: RangePreference<Double>? = null,
    hyphens: OptionalBooleanPreference? = null,
    imageFilter: EnumPreference<ImageFilter?>? = null,
    language: Preference<Language?>? = null,
    letterSpacing: OptionalRangePreference<Double>? = null,
    ligatures: OptionalBooleanPreference? = null,
    lineHeight: OptionalRangePreference<Double>? = null,
    linkColor: Preference<Color>? = null,
    maximalLineLength: OptionalRangePreference<Double>? = null,
    minimalLineLength: OptionalRangePreference<Double>? = null,
    optimalLineLength: RangePreference<Double>? = null,
    overridePublisherColors: Preference<Boolean>? = null,
    paragraphIndent: OptionalRangePreference<Double>? = null,
    paragraphSpacing: OptionalRangePreference<Double>? = null,
    readingProgression: EnumPreference<ReadingProgression>? = null,
    scroll: Preference<Boolean>? = null,
    textAlign: EnumPreference<ReadiumTextAlign?>? = null,
    textColor: Preference<Color>? = null,
    textNormalization: Preference<Boolean>? = null,
    verticalText: Preference<Boolean>? = null,
    visitedColor: Preference<Color>? = null,
    wordSpacing: OptionalRangePreference<Double>? = null,
) {
    if (language != null || readingProgression != null || verticalText != null) {
        if (language != null) {
            LanguageItem(
                preference = language,
            )
        }

        if (readingProgression != null) {
            ButtonGroupItem(
                title = "Reading progression",
                preference = readingProgression,
                formatValue = { it.name }
            )
        }

        if (verticalText != null) {
            SwitchItem(
                title = "Vertical text",
                preference = verticalText,
            )
        }

        Divider()
    }

    if (scroll != null || columnCount != null || horizontalMargins != null ||
        optimalLineLength != null || minimalLineLength != null || maximalLineLength != null
    ) {
        if (scroll != null) {
            SwitchItem(
                title = "Scroll",
                preference = scroll
            )
        }

        if (columnCount != null) {
            ButtonGroupItem(
                title = "Columns",
                preference = columnCount.withSupportedValues(null, 1, 2),
                formatValue = { value: Int? ->
                    value?.toString() ?: "Auto"
                }
            )
        }

        if (horizontalMargins != null) {
            StepperItem(
                title = "Horizontal margins",
                preference = horizontalMargins
            )
        }

        if (optimalLineLength != null) {
            StepperItem(
                title = "Optimal line length",
                preference = optimalLineLength
            )
        }

        if (minimalLineLength != null) {
            StepperItem(
                title = "Minimal line length",
                preference = minimalLineLength,
            )
        }

        if (maximalLineLength != null) {
            StepperItem(
                title = "Maximal line length",
                preference = maximalLineLength,
            )
        }

        Divider()
    }

    if (imageFilter != null || textColor != null || backgroundColor != null ||
        linkColor != null || visitedColor != null
    ) {
        if (imageFilter != null) {
            ButtonGroupItem(
                title = "Image filter",
                preference = imageFilter
            ) { value ->
                when (value) {
                    ImageFilter.DARKEN -> "Darken"
                    ImageFilter.INVERT -> "Invert"
                    null -> "None"
                }
            }
        }

        if (textColor != null) {
            ColorItem(
                title = "Text color",
                preference = textColor
            )
        }

        if (backgroundColor != null) {
            ColorItem(
                title = "Background color",
                preference = backgroundColor
            )
        }

        if (linkColor != null) {
            ColorItem(
                title = "Link color",
                preference = linkColor
            )
        }

        if (visitedColor != null) {
            ColorItem(
                title = "Visited color",
                preference = visitedColor
            )
        }

        if (overridePublisherColors != null) {
            SwitchItem(
                title = "Override Publisher Colors",
                preference = overridePublisherColors
            )
        }

        Divider()
    }

    if (fontFamily != null || fontSize != null ||
        fontWeight != null || textNormalization != null ||
        lineHeight != null || paragraphSpacing != null
    ) {
        if (fontFamily != null) {
            MenuItem(
                title = "Typeface",
                preference = fontFamily
                    .withSupportedValues(
                        null,
                        FontFamily.SANS_SERIF,
                        FontFamily.LITERATA,
                        FontFamily.IA_WRITER_DUOSPACE,
                        FontFamily.ACCESSIBLE_DFA,
                        FontFamily.OPEN_DYSLEXIC
                    )
            ) { value ->
                when (value) {
                    null -> "Original"
                    FontFamily.SANS_SERIF -> "Sans Serif"
                    else -> value.name
                }
            }
        }

        if (fontSize != null) {
            StepperItem(
                title = "Font size",
                preference = fontSize
            )
        }

        if (fontWeight != null) {
            StepperItem(
                title = "Font weight",
                preference = fontWeight,
            )
        }

        if (textNormalization != null) {
            SwitchItem(
                title = "Text normalization",
                preference = textNormalization
            )
        }

        if (lineHeight != null) {
            StepperItem(
                title = "Line height",
                preference = lineHeight
            )
        }

        if (paragraphSpacing != null) {
            StepperItem(
                title = "Paragraph spacing",
                preference = paragraphSpacing
            )
        }

        Divider()
    }

    if (textAlign != null || paragraphIndent != null || wordSpacing != null ||
        letterSpacing != null || hyphens != null || ligatures != null
    ) {
        if (textAlign != null) {
            ButtonGroupItem(
                title = "Alignment",
                preference = textAlign.withSupportedValues(
                    ReadiumTextAlign.LEFT,
                    ReadiumTextAlign.RIGHT,
                    ReadiumTextAlign.JUSTIFY
                )
            ) { value ->
                when (value) {
                    ReadiumTextAlign.CENTER -> "Center"
                    ReadiumTextAlign.JUSTIFY -> "Justify"
                    ReadiumTextAlign.START -> "Start"
                    ReadiumTextAlign.END -> "End"
                    ReadiumTextAlign.LEFT -> "Left"
                    ReadiumTextAlign.RIGHT -> "Right"
                    null -> "Default"
                }
            }
        }

        if (paragraphIndent != null) {
            StepperItem(
                title = "Paragraph indent",
                preference = paragraphIndent
            )
        }

        if (wordSpacing != null) {
            StepperItem(
                title = "Word spacing",
                preference = wordSpacing
            )
        }

        if (letterSpacing != null) {
            StepperItem(
                title = "Letter spacing",
                preference = letterSpacing
            )
        }

        if (hyphens != null) {
            SwitchItem(
                title = "Hyphens",
                preference = hyphens
            )
        }

        if (ligatures != null) {
            SwitchItem(
                title = "Ligatures",
                preference = ligatures
            )
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
}
