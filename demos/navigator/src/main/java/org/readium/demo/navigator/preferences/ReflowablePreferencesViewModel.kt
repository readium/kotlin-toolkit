/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.preferences

import androidx.compose.runtime.Stable
import org.readium.navigator.common.PreferencesController
import org.readium.navigator.web.reflowable.preferences.ReflowableWebPreferences
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.DoubleIncrement
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.EnumPreferenceDelegate
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.IntIncrement
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.PreferenceDelegate
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.RangePreferenceDelegate
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
@Stable
class ReflowablePreferencesViewModel(
    private val controller: PreferencesController<ReflowableWebPreferences, ReflowableWebSettings>,
) : PreferencesViewModel<ReflowableWebPreferences, ReflowableWebSettings> {

    /**
     * Reset all preferences.
     */
    override fun clear() {
        controller.preferences = ReflowableWebPreferences()
    }

    /**
     * Default background color.
     */
    val backgroundColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { controller.preferences.backgroundColor },
            getEffectiveValue = { controller.settings.backgroundColor },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(backgroundColor = value)
            }
        )

    /**
     * Number of reflowable columns to display .
     *
     * Only effective when [scroll] is off.
     */
    val columnCount: OptionalRangePreference<Int> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.columnCount },
            getEffectiveValue = { controller.settings.columnCount },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(columnCount = value)
            },
            defaultValue = 1,
            supportedRange = 1..10,
            valueFormatter = Int::toString,
            progressionStrategy = IntIncrement(1)
        )

    /**
     * Default typeface for the text.
     */
    val fontFamily: Preference<FontFamily?> =
        PreferenceDelegate(
            getValue = { controller.preferences.fontFamily },
            getEffectiveValue = { controller.settings.fontFamily },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(fontFamily = value)
            }
        )

    /**
     * Base text font size as a percentage. Default to 100%.
     *
     * Note that allowing a font size that is too large could break the pagination.
     */
    val fontSize: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { controller.preferences.fontSize },
            getEffectiveValue = { controller.settings.fontSize },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(fontSize = value)
            },
            supportedRange = 0.1..5.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    /**
     * Default boldness for the text as a percentage.
     *
     * If you want to change the boldness of all text, including headers, you can use this with
     * [textNormalization].
     */
    val fontWeight: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.fontWeight },
            getEffectiveValue = { controller.settings.fontWeight },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(fontWeight = value)
            },
            valueFormatter = percentFormatter(),
            supportedRange = 0.0..2.5,
            progressionStrategy = DoubleIncrement(0.25),
            defaultValue = 1.0
        )

    /**
     * Enable hyphenation for latin languages.
     *
     * Only effective when the layout is LTR.
     */
    val hyphens: OptionalBooleanPreference =
        OptionalBooleanPreferenceDelegate(
            getValue = { controller.preferences.hyphens },
            getEffectiveValue = { controller.settings.hyphens },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(hyphens = value)
            },
            defaultValue = false
        )

    /**
     * Filter applied to images.
     */
    val imageFilter: EnumPreference<ImageFilter?> =
        EnumPreferenceDelegate(
            getValue = { controller.preferences.imageFilter },
            getEffectiveValue = { controller.settings.imageFilter },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(imageFilter = value)
            },
            supportedValues = listOf(ImageFilter.DARKEN, ImageFilter.INVERT)
        )

    /**
     * Language of the publication content.
     *
     * This has an impact on the resolved layout (e.g. LTR, RTL).
     */
    val language: Preference<Language?> =
        PreferenceDelegate(
            getValue = { controller.preferences.language },
            getEffectiveValue = { controller.settings.language },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(language = value)
            }
        )

    /**
     * Space between letters.
     *
     * Only effective when the layout is LTR.
     */
    val letterSpacing: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.letterSpacing },
            getEffectiveValue = { controller.settings.letterSpacing },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(letterSpacing = value)
            },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
            defaultValue = 0.0
        )

    /**
     * Enable ligatures.
     */
    val ligatures: OptionalBooleanPreference =
        OptionalBooleanPreferenceDelegate(
            getValue = { controller.preferences.ligatures },
            getEffectiveValue = { controller.settings.ligatures },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(ligatures = value)
            },
            defaultValue = false
        )

    /**
     * Leading line height.
     */
    val lineHeight: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.lineHeight },
            getEffectiveValue = { controller.settings.lineHeight },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(lineHeight = value)
            },
            supportedRange = 1.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { it.format(5) },
            defaultValue = 1.2
        )

    /**
     * Link color.
     */
    val linkColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { controller.preferences.linkColor },
            getEffectiveValue = { controller.settings.linkColor },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(linkColor = value)
            }
        )

    /**
     * Factor applied to maximal line length.  Default to no maximal line length.
     *
     * Only effective when [scroll] is false.
     */
    val maximalLineLength: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.maximalLineLength },
            getEffectiveValue = { controller.settings.maximalLineLength },
            getIsEffective = { scroll.value != true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(maximalLineLength = value)
            },
            defaultValue = 1.0,
            supportedRange = 0.5..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    /**
     * Factor applied to minimal line length. Default to no minimal line length.
     *
     * Only effective when [scroll] is false.
     */
    val minimalLineLength: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.minimalLineLength },
            getEffectiveValue = { controller.settings.minimalLineLength },
            getIsEffective = { scroll.value != true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(minimalLineLength = value)
            },
            defaultValue = 1.0,
            supportedRange = 0.5..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    /**
     * Factor applied to horizontal margins. Default to 1.
     */
    val minMargins: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { controller.preferences.minMargins },
            getEffectiveValue = { controller.settings.minMargins },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(minMargins = value)
            },
            supportedRange = 0.0..4.0,
            progressionStrategy = DoubleIncrement(0.3),
            valueFormatter = { it.format(5) }
        )

    /**
     * Factor applied to optimal line length. Default to 1.
     *
     * Only effective when [scroll] is false.
     */
    val optimalLineLength: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { controller.preferences.optimalLineLength },
            getEffectiveValue = { controller.settings.optimalLineLength },
            getIsEffective = { scroll.value != true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(optimalLineLength = value)
            },
            supportedRange = 0.5..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    /**
     * Normalize text styles to increase accessibility.
     */
    val overridePublisherColors: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { controller.preferences.overridePublisherColors },
            getEffectiveValue = { controller.settings.overridePublisherColors },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(overridePublisherColors = value)
            }
        )

    /**
     * Color for visited links.
     */
    val visitedColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { controller.preferences.visitedColor },
            getEffectiveValue = { controller.settings.visitedColor },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(visitedColor = value)
            }
        )

    /**
     * Text indentation for paragraphs.
     *
     * Only effective when the layout is LTR or RTL.
     */
    val paragraphIndent: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.paragraphIndent },
            getEffectiveValue = { controller.settings.paragraphIndent },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(paragraphIndent = value)
            },
            supportedRange = 0.0..3.0,
            progressionStrategy = DoubleIncrement(0.2),
            valueFormatter = percentFormatter(),
            defaultValue = 0.0
        )

    /**
     * Vertical margins for paragraphs.
     */
    val paragraphSpacing: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.paragraphSpacing },
            getEffectiveValue = { controller.settings.paragraphSpacing },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(paragraphSpacing = value)
            },
            supportedRange = 0.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
            defaultValue = 0.0
        )

    /**
     * Direction of the reading progression across resources.
     *
     * This can be changed to influence directly the layout (e.g. LTR or RTL).
     */
    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { controller.preferences.readingProgression },
            getEffectiveValue = { controller.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(readingProgression = value)
            },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    /**
     * Indicates if the overflow of resources should be handled using scrolling instead of synthetic
     * pagination.
     *
     * Only effective when [verticalText] is off.
     */
    val scroll: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { controller.preferences.scroll },
            getEffectiveValue = { controller.settings.scroll },
            getIsEffective = { !controller.settings.verticalText },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(scroll = value)
            }
        )

    /**
     * Page text alignment.
     *
     * Only effective when the layout is LTR or RTL.
     */
    val textAlign: EnumPreference<TextAlign?> =
        EnumPreferenceDelegate(
            getValue = { controller.preferences.textAlign },
            getEffectiveValue = { controller.settings.textAlign },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(textAlign = value)
            },
            supportedValues = listOf(
                TextAlign.START,
                TextAlign.LEFT,
                TextAlign.RIGHT,
                TextAlign.JUSTIFY
            )
        )

    /**
     * Default page text color.
     */
    val textColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { controller.preferences.textColor },
            getEffectiveValue = { controller.settings.textColor },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(textColor = value)
            }
        )

    /**
     * Normalize text styles to increase accessibility.
     */
    val textNormalization: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { controller.preferences.textNormalization },
            getEffectiveValue = { controller.settings.textNormalization },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(textNormalization = value)
            }
        )

    /**
     * Indicates whether the text should be laid out vertically. This is used for example with CJK
     * languages. This setting is automatically derived from the language if no preference is given.
     */
    val verticalText: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { controller.preferences.verticalText },
            getEffectiveValue = { controller.settings.verticalText },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(verticalText = value)
            }
        )

    /**
     * Space between words.
     *
     * Only effective when the layout is LTR.
     */
    val wordSpacing: OptionalRangePreference<Double> =
        OptionalRangePreferenceDelegate(
            getValue = { controller.preferences.wordSpacing },
            getEffectiveValue = { controller.settings.wordSpacing },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(wordSpacing = value)
            },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
            defaultValue = 0.0
        )

    private fun percentFormatter(): (Double) -> String =
        { it.format(maximumFractionDigits = 0, percent = true) }
}
