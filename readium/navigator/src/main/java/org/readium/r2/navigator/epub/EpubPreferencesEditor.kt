/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.util.Language

/**
 * Interactive editor of [EpubPreferences].
 *
 * This can be used as a view model for a user preferences screen.
 *
 * @see EpubPreferences
 */
@ExperimentalReadiumApi
class EpubPreferencesEditor internal constructor(
    initialPreferences: EpubPreferences,
    publicationMetadata: Metadata,
    val layout: EpubLayout,
    defaults: EpubDefaults
) : PreferencesEditor<EpubPreferences> {

    private data class State(
        val preferences: EpubPreferences,
        val settings: EpubSettings,
        val layout: Layout
    )

    private val settingsResolver: EpubSettingsResolver =
        EpubSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: EpubPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { EpubPreferences() }
    }

    val backgroundColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { preferences.backgroundColor },
            getEffectiveValue = { state.settings.backgroundColor ?: Color((theme.value ?: theme.effectiveValue).backgroundColor) },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && preferences.backgroundColor != null },
            updateValue = { value -> updateValues { it.copy(backgroundColor = value) } },
        )

    val columnCount: EnumPreference<ColumnCount> =
        EnumPreferenceDelegate(
            getValue = { preferences.columnCount },
            getEffectiveValue = { state.settings.columnCount },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(columnCount = value) } },
            supportedValues = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
        )

    val fontFamily: Preference<FontFamily?> =
        PreferenceDelegate(
            getValue = { preferences.fontFamily },
            getEffectiveValue = { state.settings.fontFamily },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(fontFamily = value) } }
        )

    val fontWeight: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.fontWeight },
            getEffectiveValue = { state.settings.fontWeight ?: 1.0 },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && preferences.fontWeight != null },
            updateValue = { value -> updateValues { it.copy(fontWeight = value) } },
            valueFormatter = percentFormatter(),
            supportedRange = 0.0..2.5,
            progressionStrategy = DoubleIncrement(0.25)
        )

    val fontSize: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.fontSize },
            getEffectiveValue = { state.settings.fontSize ?: 1.0 },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(fontSize = value) } },
            supportedRange = 0.4..5.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
        )

    val hyphens: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.hyphens },
            getEffectiveValue = { state.settings.hyphens ?: false },
            getIsEffective = ::isHyphensEffective,
            updateValue = { value -> updateValues { it.copy(hyphens = value) } },
        )

    val imageFilter: EnumPreference<ImageFilter> =
        EnumPreferenceDelegate(
            getValue = { preferences.imageFilter },
            getEffectiveValue = { state.settings.imageFilter },
            getIsEffective = { state.settings.theme == Theme.DARK },
            updateValue = { value -> updateValues { it.copy(imageFilter = value) } },
            supportedValues = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT),
        )

    val language: Preference<Language?> =
        PreferenceDelegate(
            getValue = { preferences.language },
            getEffectiveValue = { state.settings.language },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(language = value) } },
        )

    val letterSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.letterSpacing },
            getEffectiveValue = { state.settings.letterSpacing ?: 0.0 },
            getIsEffective = ::isLetterSpacingEffective,
            updateValue = { value -> updateValues { it.copy(letterSpacing = value) } },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
        )

    val ligatures: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.ligatures },
            getEffectiveValue = { state.settings.ligatures ?: false },
            getIsEffective = ::isLigaturesEffective,
            updateValue = { value -> updateValues { it.copy(ligatures = value) } },
        )

    val lineHeight: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.lineHeight },
            getEffectiveValue = { state.settings.lineHeight ?: 1.2 },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.publisherStyles && preferences.lineHeight != null },
            updateValue = { value -> updateValues { it.copy(lineHeight = value) } },
            supportedRange = 1.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { it.format(5) },
        )

    val pageMargins: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pageMargins },
            getEffectiveValue = { state.settings.pageMargins },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(pageMargins = value) } },
            supportedRange = 0.5..4.0,
            progressionStrategy = DoubleIncrement(0.3),
            valueFormatter = { it.format(5) },
        )

    val paragraphIndent: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.paragraphIndent },
            getEffectiveValue = { state.settings.paragraphIndent ?: 0.0 },
            getIsEffective = ::isParagraphIndentEffective,
            updateValue = { value -> updateValues { it.copy(paragraphIndent = value) } },
            supportedRange = 0.0..3.0,
            progressionStrategy = DoubleIncrement(0.2),
            valueFormatter = percentFormatter(),
        )

    val paragraphSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.paragraphSpacing },
            getEffectiveValue = { state.settings.paragraphSpacing ?: 0.0 },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.publisherStyles && preferences.paragraphSpacing != null },
            updateValue = { value -> updateValues { it.copy(paragraphSpacing = value) } },
            supportedRange = 0.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
        )

    val publisherStyles: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.publisherStyles },
            getEffectiveValue = { state.settings.publisherStyles },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(publisherStyles = value) } },
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scroll: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.scroll },
            getEffectiveValue = { state.settings.scroll },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(scroll = value) } },
        )

    val spread: EnumPreference<Spread> =
        EnumPreferenceDelegate(
            getValue = { preferences.spread },
            getEffectiveValue = { state.settings.spread },
            getIsEffective = { layout == EpubLayout.FIXED },
            updateValue = { value -> updateValues { it.copy(spread = value) } },
            supportedValues = listOf(Spread.NEVER, Spread.ALWAYS),
        )

    val textAlign: EnumPreference<TextAlign?> =
        EnumPreferenceDelegate(
            getValue = { preferences.textAlign },
            getEffectiveValue = { state.settings.textAlign },
            getIsEffective = ::isTextAlignEffective,
            updateValue = { value -> updateValues { it.copy(textAlign = value) } },
            supportedValues = listOf(TextAlign.START, TextAlign.LEFT, TextAlign.RIGHT, TextAlign.JUSTIFY),
        )

    val textColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { preferences.textColor },
            getEffectiveValue = { state.settings.textColor ?: Color((theme.value ?: theme.effectiveValue).contentColor) },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && preferences.textColor != null },
            updateValue = { value -> updateValues { it.copy(textColor = value) } }
        )

    val textNormalization: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.textNormalization },
            getEffectiveValue = { state.settings.textNormalization },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(textNormalization = value) } }
        )

    val theme: EnumPreference<Theme> =
        EnumPreferenceDelegate(
            getValue = { preferences.theme },
            getEffectiveValue = { state.settings.theme },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(theme = value) } },
            supportedValues = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA),
        )

    val typeScale: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.typeScale },
            getEffectiveValue = { state.settings.typeScale ?: 1.2 },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !state.settings.publisherStyles && preferences.typeScale != null },
            updateValue = { value -> updateValues { it.copy(typeScale = value) } },
            valueFormatter = { it.format(5) },
            supportedRange = 1.0..2.0,
            progressionStrategy = StepsProgression(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
        )

    val verticalText: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.verticalText },
            getEffectiveValue = { state.settings.verticalText },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(verticalText = value) } },
        )

    val wordSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.wordSpacing },
            getEffectiveValue = { state.settings.wordSpacing ?: 0.0 },
            getIsEffective = ::isWordSpacingEffective,
            updateValue = { value -> updateValues { it.copy(wordSpacing = value) } },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
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
        preferences.hyphens != null

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
