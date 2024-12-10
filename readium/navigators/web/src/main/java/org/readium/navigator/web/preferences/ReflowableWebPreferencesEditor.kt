/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.web.css.Layout
import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.DoubleIncrement
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.EnumPreferenceDelegate
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.PreferenceDelegate
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.RangePreferenceDelegate
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Language

/**
 * Interactive editor of [ReflowableWebPreferences].
 *
 * This can be used as a view model for a user preferences screen. Every data you can get
 * from the editor is observable so if you use it in a composable function,
 * it will be recomposed on every change.
 *
 * @see ReflowableWebPreferences
 * @see ReflowableWebSettings
 */
@ExperimentalReadiumApi
@OptIn(InternalReadiumApi::class)
@Stable
public class ReflowableWebPreferencesEditor internal constructor(
    initialPreferences: ReflowableWebPreferences,
    publicationMetadata: Metadata,
    defaults: ReflowableWebDefaults,
) : PreferencesEditor<ReflowableWebPreferences, ReflowableWebSettings> {

    private data class State(
        val preferences: ReflowableWebPreferences,
        val settings: ReflowableWebSettings,
        val layout: Layout,
    )

    private val settingsResolver: ReflowableWebSettingsResolver =
        ReflowableWebSettingsResolver(publicationMetadata, defaults)

    private var state by mutableStateOf(initialPreferences.toState())

    override val preferences: ReflowableWebPreferences
        get() = state.preferences

    override val settings: ReflowableWebSettings
        get() = state.settings

    /**
     * Reset all preferences.
     */
    @OptIn(ExperimentalReadiumApi::class)
    override fun clear() {
        updateValues { ReflowableWebPreferences() }
    }

    /**
     * Default background color.
     *
     * For fixed-layout publications, it applies to the navigator background but not the publication
     * pages.
     *
     * When unset, the current [theme] background color is effective.
     */
    public val backgroundColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { preferences.backgroundColor },
            getEffectiveValue = {
                state.settings.backgroundColor ?: Color(
                    (theme.value ?: theme.effectiveValue).backgroundColor
                )
            },
            getIsEffective = { preferences.backgroundColor != null },
            updateValue = { value -> updateValues { it.copy(backgroundColor = value) } }
        )

    /**
     * Number of reflowable columns to display (one-page view or two-page spread).
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [scroll] is off
     */
    @ExperimentalReadiumApi
    public val columnCount: EnumPreference<Int> =
        EnumPreferenceDelegate(
            getValue = { preferences.columnCount },
            getEffectiveValue = { state.settings.columnCount },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(columnCount = value) } },
            supportedValues = listOf(1, 2)
        )

    /**
     * Default typeface for the text.
     *
     * Only effective with reflowable publications.
     */
    public val fontFamily: Preference<FontFamily?> =
        PreferenceDelegate(
            getValue = { preferences.fontFamily },
            getEffectiveValue = { state.settings.fontFamily },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fontFamily = value) } }
        )

    /**
     * Base text font size as a percentage. Default to 100%.
     *
     * Note that allowing a font size that is too large could break the pagination.
     *
     * Only effective with reflowable publications.
     */
    public val fontSize: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.fontSize },
            getEffectiveValue = { state.settings.fontSize },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fontSize = value) } },
            supportedRange = 0.1..5.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    /**
     * Default boldness for the text as a percentage.
     *
     * If you want to change the boldness of all text, including headers, you can use this with
     * [textNormalization].
     *
     * Only effective with reflowable publications.
     */
    public val fontWeight: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.fontWeight },
            getEffectiveValue = { state.settings.fontWeight ?: 1.0 },
            getIsEffective = { preferences.fontWeight != null },
            updateValue = { value -> updateValues { it.copy(fontWeight = value) } },
            valueFormatter = percentFormatter(),
            supportedRange = 0.0..2.5,
            progressionStrategy = DoubleIncrement(0.25)
        )

    /**
     * Enable hyphenation for latin languages.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     *  - the layout is LTR
     */
    public val hyphens: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.hyphens },
            getEffectiveValue = {
                state.settings.hyphens
                    ?: (state.settings.textAlign == TextAlign.JUSTIFY)
            },
            getIsEffective = ::isHyphensEffective,
            updateValue = { value -> updateValues { it.copy(hyphens = value) } }
        )

    /**
     * Filter applied to images in dark theme.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - the [theme] is set to [Theme.DARK]
     */
    public val imageFilter: EnumPreference<ImageFilter?> =
        EnumPreferenceDelegate(
            getValue = { preferences.imageFilter },
            getEffectiveValue = { state.settings.imageFilter },
            getIsEffective = { state.settings.theme == Theme.DARK },
            updateValue = { value -> updateValues { it.copy(imageFilter = value) } },
            supportedValues = listOf(ImageFilter.DARKEN, ImageFilter.INVERT)
        )

    /**
     * Language of the publication content.
     *
     * This has an impact on the resolved layout (e.g. LTR, RTL).
     */
    public val language: Preference<Language?> =
        PreferenceDelegate(
            getValue = { preferences.language },
            getEffectiveValue = { state.settings.language },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(language = value) } }
        )

    /**
     * Space between letters.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     *  - the layout is LTR
     */
    public val letterSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.letterSpacing },
            getEffectiveValue = { state.settings.letterSpacing ?: 0.0 },
            getIsEffective = ::isLetterSpacingEffective,
            updateValue = { value -> updateValues { it.copy(letterSpacing = value) } },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    /**
     * Enable ligatures in Arabic.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     *  - the layout is RTL
     */
    public val ligatures: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.ligatures },
            getEffectiveValue = { state.settings.ligatures ?: false },
            getIsEffective = ::isLigaturesEffective,
            updateValue = { value -> updateValues { it.copy(ligatures = value) } }
        )

    /**
     * Leading line height.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     */
    public val lineHeight: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.lineHeight },
            getEffectiveValue = { state.settings.lineHeight ?: 1.2 },
            getIsEffective = { !state.settings.publisherStyles && preferences.lineHeight != null },
            updateValue = { value -> updateValues { it.copy(lineHeight = value) } },
            supportedRange = 1.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { it.format(5) }
        )

    /**
     * Factor applied to horizontal margins. Default to 1.
     *
     * Only effective with reflowable publications.
     */
    @ExperimentalReadiumApi
    public val pageMargins: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pageMargins },
            getEffectiveValue = { state.settings.pageMargins },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(pageMargins = value) } },
            supportedRange = 0.0..4.0,
            progressionStrategy = DoubleIncrement(0.3),
            valueFormatter = { it.format(5) }
        )

    /**
     * Text indentation for paragraphs.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     *  - the layout is LTR or RTL
     */
    public val paragraphIndent: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.paragraphIndent },
            getEffectiveValue = { state.settings.paragraphIndent ?: 0.0 },
            getIsEffective = ::isParagraphIndentEffective,
            updateValue = { value -> updateValues { it.copy(paragraphIndent = value) } },
            supportedRange = 0.0..3.0,
            progressionStrategy = DoubleIncrement(0.2),
            valueFormatter = percentFormatter()
        )

    /**
     * Vertical margins for paragraphs.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     */
    public val paragraphSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.paragraphSpacing },
            getEffectiveValue = { state.settings.paragraphSpacing ?: 0.0 },
            getIsEffective = { !state.settings.publisherStyles && preferences.paragraphSpacing != null },
            updateValue = { value -> updateValues { it.copy(paragraphSpacing = value) } },
            supportedRange = 0.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    /**
     * Indicates whether the original publisher styles should be observed. Many advanced settings
     * require this to be off.
     *
     * Only effective with reflowable publications.
     */
    public val publisherStyles: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.publisherStyles },
            getEffectiveValue = { state.settings.publisherStyles },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(publisherStyles = value) } }
        )

    /**
     * Direction of the reading progression across resources.
     *
     * This can be changed to influence directly the layout (e.g. LTR or RTL).
     */
    public val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    /**
     * Indicates if the overflow of resources should be handled using scrolling instead of synthetic
     * pagination.
     *
     * Only effective with reflowable publications.
     */
    public val scroll: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.scroll },
            getEffectiveValue = { state.settings.scroll },
            getIsEffective = { !state.settings.verticalText },
            updateValue = { value -> updateValues { it.copy(scroll = value) } }
        )

    /**
     * Page text alignment.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     *  - the layout is LTR or RTL
     */
    public val textAlign: EnumPreference<TextAlign?> =
        EnumPreferenceDelegate(
            getValue = { preferences.textAlign },
            getEffectiveValue = { state.settings.textAlign },
            getIsEffective = ::isTextAlignEffective,
            updateValue = { value -> updateValues { it.copy(textAlign = value) } },
            supportedValues = listOf(
                TextAlign.START,
                TextAlign.LEFT,
                TextAlign.RIGHT,
                TextAlign.JUSTIFY
            )
        )

    /**
     * Default page text color.
     *
     * When unset, the current [theme] text color is effective.
     * Only effective with reflowable publications.
     */
    public val textColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { preferences.textColor },
            getEffectiveValue = {
                state.settings.textColor ?: Color(
                    (theme.value ?: theme.effectiveValue).contentColor
                )
            },
            getIsEffective = { preferences.textColor != null },
            updateValue = { value -> updateValues { it.copy(textColor = value) } }
        )

    /**
     * Normalize text styles to increase accessibility.
     *
     * Only effective with reflowable publications.
     */
    public val textNormalization: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.textNormalization },
            getEffectiveValue = { state.settings.textNormalization },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(textNormalization = value) } }
        )

    /**
     * Reader theme (light, dark, sepia).
     *
     * Only effective with reflowable publications.
     */
    public val theme: EnumPreference<Theme> =
        EnumPreferenceDelegate(
            getValue = { preferences.theme },
            getEffectiveValue = { state.settings.theme },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(theme = value) } },
            supportedValues = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
        )

    /**
     * Indicates whether the text should be laid out vertically. This is used for example with CJK
     * languages. This setting is automatically derived from the language if no preference is given.
     *
     * Only effective with reflowable publications.
     */
    public val verticalText: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.verticalText },
            getEffectiveValue = { state.settings.verticalText },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(verticalText = value) } }
        )

    /**
     * Space between words.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - the layout is LTR
     */
    public val wordSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.wordSpacing },
            getEffectiveValue = { state.settings.wordSpacing ?: 0.0 },
            getIsEffective = ::isWordSpacingEffective,
            updateValue = { value -> updateValues { it.copy(wordSpacing = value) } },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter()
        )

    private fun percentFormatter(): (Double) -> String =
        { it.format(maximumFractionDigits = 0, percent = true) }

    private fun updateValues(updater: (ReflowableWebPreferences) -> ReflowableWebPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun ReflowableWebPreferences.toState(): State {
        val settings = settingsResolver.settings(this)
        val layout = Layout.from(settings)

        return State(
            preferences = this,
            settings = settings,
            layout = layout
        )
    }

    private fun isHyphensEffective() =
        state.layout.stylesheets == Layout.Stylesheets.Default &&
            !state.settings.publisherStyles &&
            (preferences.hyphens != null || state.settings.textAlign == TextAlign.JUSTIFY)

    private fun isLetterSpacingEffective() =
        state.layout.stylesheets == Layout.Stylesheets.Default &&
            !state.settings.publisherStyles &&
            preferences.letterSpacing != null

    private fun isLigaturesEffective() =
        state.layout.stylesheets == Layout.Stylesheets.Rtl &&
            !state.settings.publisherStyles &&
            preferences.ligatures != null

    private fun isParagraphIndentEffective() =
        state.layout.stylesheets in listOf(Layout.Stylesheets.Default, Layout.Stylesheets.Rtl) &&
            !state.settings.publisherStyles &&
            preferences.paragraphIndent != null

    private fun isTextAlignEffective() =
        state.layout.stylesheets in listOf(Layout.Stylesheets.Default, Layout.Stylesheets.Rtl) &&
            !state.settings.publisherStyles &&
            preferences.textAlign != null

    private fun isWordSpacingEffective(): Boolean =
        state.layout.stylesheets == Layout.Stylesheets.Default &&
            !state.settings.publisherStyles &&
            preferences.wordSpacing != null
}
