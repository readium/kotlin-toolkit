/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.util.Language

/**
 * Editor for a set of [EpubPreferences].
 *
 * Use [EpubPreferencesEditor] to assist you in building a preferences user interface or modifying
 * existing preferences. It includes rules for adjusting preferences, such as the supported values
 * or ranges.
 */
@OptIn(ExperimentalReadiumApi::class)
public class EpubPreferencesEditor internal constructor(
    initialPreferences: EpubPreferences,
    publicationMetadata: Metadata,
    public val layout: EpubLayout,
    defaults: EpubDefaults,
) : PreferencesEditor<EpubPreferences> {

    private data class State(
        val preferences: EpubPreferences,
        val settings: EpubSettings,
        val layout: Layout,
    )

    private val settingsResolver: EpubSettingsResolver =
        EpubSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: EpubPreferences
        get() = state.preferences

    /**
     * Reset all preferences.
     */
    @OptIn(ExperimentalReadiumApi::class)
    override fun clear() {
        updateValues { EpubPreferences() }
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
    public val columnCount: EnumPreference<ColumnCount> =
        EnumPreferenceDelegate(
            getValue = { preferences.columnCount },
            getEffectiveValue = { state.settings.columnCount },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(columnCount = value) } },
            supportedValues = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO)
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE && preferences.fontWeight != null },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.publisherStyles && preferences.lineHeight != null },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.publisherStyles && preferences.paragraphSpacing != null },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.verticalText },
            updateValue = { value -> updateValues { it.copy(scroll = value) } }
        )

    /**
     * Indicates if the fixed-layout publication should be rendered with a synthetic spread
     * (dual-page).
     *
     * Only effective with fixed-layout publications.
     */
    public val spread: EnumPreference<Spread> =
        EnumPreferenceDelegate(
            getValue = { preferences.spread },
            getEffectiveValue = { state.settings.spread },
            getIsEffective = { layout == EpubLayout.FIXED },
            updateValue = { value -> updateValues { it.copy(spread = value) } },
            supportedValues = listOf(Spread.NEVER, Spread.ALWAYS)
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE && preferences.textColor != null },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(theme = value) } },
            supportedValues = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA)
        )

    /**
     * Scale applied to all element font sizes.
     *
     * Only effective when:
     *  - the publication is reflowable
     *  - [publisherStyles] is off
     */
    @ExperimentalReadiumApi
    public val typeScale: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.typeScale },
            getEffectiveValue = { state.settings.typeScale ?: 1.2 },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.publisherStyles && preferences.typeScale != null },
            updateValue = { value -> updateValues { it.copy(typeScale = value) } },
            valueFormatter = { it.format(5) },
            supportedRange = 1.0..2.0,
            progressionStrategy = StepsProgression(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618)
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
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
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

    private fun updateValues(updater: (EpubPreferences) -> EpubPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun EpubPreferences.toState(): State {
        val settings = settingsResolver.settings(this)
        val layout = Layout.from(settings)

        return State(
            preferences = this,
            settings = settings,
            layout = layout
        )
    }

    private fun isHyphensEffective() = layout == EpubLayout.REFLOWABLE &&
        state.layout.stylesheets == Layout.Stylesheets.Default &&
        !state.settings.publisherStyles &&
        (preferences.hyphens != null || state.settings.textAlign == TextAlign.JUSTIFY)

    private fun isLetterSpacingEffective() = layout == EpubLayout.REFLOWABLE &&
        state.layout.stylesheets == Layout.Stylesheets.Default &&
        !state.settings.publisherStyles &&
        preferences.letterSpacing != null

    private fun isLigaturesEffective() = layout == EpubLayout.REFLOWABLE &&
        state.layout.stylesheets == Layout.Stylesheets.Rtl &&
        !state.settings.publisherStyles &&
        preferences.ligatures != null

    private fun isParagraphIndentEffective() = layout == EpubLayout.REFLOWABLE &&
        state.layout.stylesheets in listOf(Layout.Stylesheets.Default, Layout.Stylesheets.Rtl) &&
        !state.settings.publisherStyles &&
        preferences.paragraphIndent != null

    private fun isTextAlignEffective() = layout == EpubLayout.REFLOWABLE &&
        state.layout.stylesheets in listOf(Layout.Stylesheets.Default, Layout.Stylesheets.Rtl) &&
        !state.settings.publisherStyles &&
        preferences.textAlign != null

    private fun isWordSpacingEffective(): Boolean = layout == EpubLayout.REFLOWABLE &&
        state.layout.stylesheets == Layout.Stylesheets.Default &&
        !state.settings.publisherStyles &&
        preferences.wordSpacing != null
}
