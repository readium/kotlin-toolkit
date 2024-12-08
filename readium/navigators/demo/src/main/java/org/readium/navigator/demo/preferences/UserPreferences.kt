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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.common.Settings
import org.readium.navigator.web.preferences.FixedWebPreferencesEditor
import org.readium.navigator.web.preferences.ReflowableWebPreferencesEditor
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
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.preferences.withSupportedValues
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Stateful user settings component.
 */

@Composable
fun <P : Preferences<P>, S : Settings, E : PreferencesEditor<P, S>> UserPreferences(
    editor: E,
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
                onClick = { editor.clear() }
            ) {
                Text("Reset")
            }
        }

        Divider()

        when (editor) {
            is FixedWebPreferencesEditor ->
                FixedLayoutUserPreferences(
                    readingProgression = editor.readingProgression,
                    fit = editor.fit,
                    spreads = editor.spreads
                )
            is ReflowableWebPreferencesEditor ->
                ReflowableUserPreferences(
                    backgroundColor = editor.backgroundColor,
                    columnCount = editor.columnCount,
                    fontFamily = editor.fontFamily,
                    fontSize = editor.fontSize,
                    fontWeight = editor.fontWeight,
                    hyphens = editor.hyphens,
                    imageFilter = editor.imageFilter,
                    language = editor.language,
                    letterSpacing = editor.letterSpacing,
                    ligatures = editor.ligatures,
                    lineHeight = editor.lineHeight,
                    paragraphIndent = editor.paragraphIndent,
                    paragraphSpacing = editor.paragraphSpacing,
                    publisherStyles = editor.publisherStyles,
                    readingProgression = editor.readingProgression,
                    scroll = editor.scroll,
                    textAlign = editor.textAlign,
                    textColor = editor.textColor,
                    textNormalization = editor.textNormalization,
                    theme = editor.theme,
                    verticalText = editor.verticalText,
                    wordSpacing = editor.wordSpacing
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
    columnCount: EnumPreference<Int>? = null,
    fontFamily: Preference<FontFamily?>? = null,
    fontSize: RangePreference<Double>? = null,
    fontWeight: RangePreference<Double>? = null,
    hyphens: Preference<Boolean>? = null,
    imageFilter: EnumPreference<ImageFilter?>? = null,
    language: Preference<Language?>? = null,
    letterSpacing: RangePreference<Double>? = null,
    ligatures: Preference<Boolean>? = null,
    lineHeight: RangePreference<Double>? = null,
    pageMargins: RangePreference<Double>? = null,
    paragraphIndent: RangePreference<Double>? = null,
    paragraphSpacing: RangePreference<Double>? = null,
    publisherStyles: Preference<Boolean>? = null,
    readingProgression: EnumPreference<ReadingProgression>? = null,
    scroll: Preference<Boolean>? = null,
    textAlign: EnumPreference<ReadiumTextAlign?>? = null,
    textColor: Preference<Color>? = null,
    textNormalization: Preference<Boolean>? = null,
    theme: EnumPreference<Theme>? = null,
    typeScale: RangePreference<Double>? = null,
    verticalText: Preference<Boolean>? = null,
    wordSpacing: RangePreference<Double>? = null,
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

    if (scroll != null || columnCount != null || pageMargins != null) {
        if (scroll != null) {
            SwitchItem(
                title = "Scroll",
                preference = scroll
            )
        }

        if (columnCount != null) {
            ButtonGroupItem(
                title = "Columns",
                preference = columnCount,
                formatValue = Int::toString
            )
        }

        if (pageMargins != null) {
            StepperItem(
                title = "Page margins",
                preference = pageMargins
            )
        }

        Divider()
    }

    if (theme != null || textColor != null || imageFilter != null) {
        if (theme != null) {
            ButtonGroupItem(
                title = "Theme",
                preference = theme
            ) { value ->
                when (value) {
                    Theme.LIGHT -> "Light"
                    Theme.DARK -> "Dark"
                    Theme.SEPIA -> "Sepia"
                }
            }
        }

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

        Divider()
    }

    if (fontFamily != null || fontSize != null || textNormalization != null) {
        if (fontFamily != null) {
            MenuItem(
                title = "Typeface",
                preference = fontFamily
                    .withSupportedValues(
                        null,
                        // FontFamily.LITERATA,
                        FontFamily.SANS_SERIF,
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
                preference = fontWeight
            )
        }

        if (textNormalization != null) {
            SwitchItem(
                title = "Text normalization",
                preference = textNormalization
            )
        }

        Divider()
    }

    if (publisherStyles != null) {
        SwitchItem(
            title = "Publisher styles",
            preference = publisherStyles
        )

        if (!(publisherStyles.value ?: publisherStyles.effectiveValue)) {
            if (textAlign != null) {
                ButtonGroupItem(
                    title = "Alignment",
                    preference = textAlign
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

            if (typeScale != null) {
                StepperItem(
                    title = "Type scale",
                    preference = typeScale
                )
            }

            if (lineHeight != null) {
                StepperItem(
                    title = "Line height",
                    preference = lineHeight
                )
            }

            if (paragraphIndent != null) {
                StepperItem(
                    title = "Paragraph indent",
                    preference = paragraphIndent
                )
            }

            if (paragraphSpacing != null) {
                StepperItem(
                    title = "Paragraph spacing",
                    preference = paragraphSpacing
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
}

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
}
