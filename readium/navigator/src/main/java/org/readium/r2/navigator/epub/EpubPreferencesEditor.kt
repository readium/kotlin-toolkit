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
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
class EpubPreferencesEditor internal constructor(
    initialPreferences: EpubPreferences,
    publicationMetadata: Metadata,
    epubLayout: EpubLayout,
    defaults: EpubDefaults,
    configuration: Configuration
): PreferencesEditor<EpubPreferences> {

    data class Configuration(
        val fontFamilies: List<FontFamily> = emptyList(),
        val fontSizeRange: ClosedRange<Double> = 0.4..5.0,
        val fontSizeProgression: ProgressionStrategy<Double> = DoubleIncrement(0.1),
        val pageMarginsRange: ClosedRange<Double> = 0.5..4.0,
        val pageMarginsProgression: ProgressionStrategy<Double> = DoubleIncrement(0.3)
    )

    private val settingsResolver: EpubSettingsResolver =
        EpubSettingsResolver(publicationMetadata, defaults)

    private var settings: EpubSettings =
        settingsResolver.settings(initialPreferences)

    private var readiumCssLayout: Layout =
        Layout.from(settings)

    val layout: EpubLayout = epubLayout

    override var preferences: EpubPreferences =
        initialPreferences
        private set

    override fun clear() {
       updateValues { EpubPreferences() }
    }

    val backgroundColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { preferences.backgroundColor },
            getEffectiveValue = { settings.backgroundColor },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(backgroundColor = value) } },
        )
    
    val columnCount: EnumPreference<ColumnCount> =
        EnumPreferenceDelegate(
            getValue = { preferences.columnCount },
            getEffectiveValue = { settings.columnCount },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !settings.scroll },
            updateValue = { value -> updateValues { it.copy(columnCount = value) } },
            supportedValues = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
        )

    val fontFamily: EnumPreference<FontFamily?> =
        EnumPreferenceDelegate(
            getValue = { preferences.fontFamily },
            getEffectiveValue = { settings.fontFamily },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(fontFamily = value) } },
            supportedValues = configuration.fontFamilies,
        )

    val fontSize: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.fontSize },
            getEffectiveValue = { settings.fontSize },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(fontSize = value) } },
            supportedRange = configuration.fontSizeRange,
            progressionStrategy = configuration.fontSizeProgression,
            valueFormatter = percentFormatter(),
        )

    val hyphens: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.hyphens },
            getEffectiveValue = { settings.hyphens },
            getIsEffective = { isHyphensEffective() },
            updateValue = { value -> updateValues { it.copy(hyphens = value) }},
        )

    val imageFilter: EnumPreference<ImageFilter> =
        EnumPreferenceDelegate(
            getValue = { preferences.imageFilter },
            getEffectiveValue = { settings.imageFilter },
            getIsEffective = { settings.theme == Theme.DARK },
            updateValue = { value -> updateValues { it.copy(imageFilter = value) } },
            supportedValues = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT),
        )

    val language: Preference<Language?> =
        PreferenceDelegate(
            getValue = { preferences.language },
            getEffectiveValue = { settings.language },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(language = value) } },
        )

    val letterSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.letterSpacing },
            getEffectiveValue = { settings.letterSpacing },
            getIsEffective = { isLetterSpacing() },
            updateValue = { value -> updateValues { it.copy(letterSpacing = value) } },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
        )

    val ligatures: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.ligatures },
            getEffectiveValue = { settings.ligatures },
            getIsEffective = { isLigaturesSpacing() },
            updateValue = { value -> updateValues { it.copy(ligatures = value) } },
        )

    val lineHeight: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.lineHeight },
            getEffectiveValue = { settings.lineHeight },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !settings.publisherStyles },
            updateValue = { value -> updateValues { it.copy(lineHeight = value) } },
            supportedRange = 1.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { it.format(5) },
        )

    val pageMargins: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pageMargins },
            getEffectiveValue = { settings.pageMargins },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !settings.scroll },
            updateValue = { value -> updateValues { it.copy(pageMargins = value) } },
            supportedRange = configuration.pageMarginsRange,
            progressionStrategy = configuration.pageMarginsProgression,
            valueFormatter = { it.format(5) },
        )

    val paragraphIndent: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.paragraphIndent },
            getEffectiveValue = { settings.paragraphIndent },
            getIsEffective = { isParagraphIndentEffective() },
            updateValue = { value -> updateValues { it.copy(paragraphIndent = value) } },
            supportedRange = 0.0..3.0,
            progressionStrategy = DoubleIncrement(0.2),
            valueFormatter = percentFormatter(),
        )

    val paragraphSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.paragraphSpacing },
            getEffectiveValue = { settings.paragraphSpacing },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !settings.publisherStyles },
            updateValue = { value -> updateValues { it.copy(paragraphSpacing = value) } },
            supportedRange = 0.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
        )

    val publisherStyles: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.publisherStyles },
            getEffectiveValue = { settings.publisherStyles },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(publisherStyles = value) }},
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scroll: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.scroll },
            getEffectiveValue = { settings.scroll },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(scroll = value) } },
        )

    val spread: EnumPreference<Spread> =
        EnumPreferenceDelegate(
            getValue = { preferences.spread },
            getEffectiveValue = { settings.spread },
            getIsEffective = { layout == EpubLayout.FIXED },
            updateValue = { value -> updateValues { it.copy(spread = value) } },
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.ALWAYS),
        )

    val textAlign: EnumPreference<TextAlign> =
        EnumPreferenceDelegate(
            getValue = { preferences.textAlign },
            getEffectiveValue = { settings.textAlign },
            getIsEffective = { isTextAlignEffective() },
            updateValue = { value -> updateValues { it.copy(textAlign = value) } },
            supportedValues = listOf(TextAlign.START, TextAlign.LEFT, TextAlign.RIGHT, TextAlign.JUSTIFY),
        )

    val textColor: Preference<Color> =
        PreferenceDelegate(
            getValue = { preferences.textColor },
            getEffectiveValue = { settings.textColor },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(textColor = value) } }
        )

    val textNormalization: EnumPreference<TextNormalization> =
        EnumPreferenceDelegate(
            getValue = { preferences.textNormalization },
            getEffectiveValue = { settings.textNormalization },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(textNormalization = value) } },
            supportedValues = listOf(TextNormalization.NONE, TextNormalization.BOLD, TextNormalization.ACCESSIBILITY),
        )

    val theme: EnumPreference<Theme> =
        EnumPreferenceDelegate(
            getValue = { preferences.theme },
            getEffectiveValue = { settings.theme },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(theme = value) } },
            supportedValues = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA),
        )

    val typeScale: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.typeScale },
            getEffectiveValue = { settings.typeScale },
            getIsEffective = { layout == EpubLayout.REFLOWABLE && !settings.publisherStyles },
            updateValue = { value -> updateValues { it.copy(typeScale = value) } },
            valueFormatter = { it.format(5) },
            supportedRange = 1.0..2.0,
            progressionStrategy = StepsProgression(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
        )

    val verticalText: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.verticalText },
            getEffectiveValue = { settings.verticalText },
            getIsEffective = { layout == EpubLayout.REFLOWABLE },
            updateValue = { value -> updateValues { it.copy(verticalText = value) } },
        )

    val wordSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.wordSpacing },
            getEffectiveValue = { settings.wordSpacing },
            getIsEffective = { isWordSpacingEffective() },
            updateValue = { value -> updateValues { it.copy(wordSpacing = value) } },
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = percentFormatter(),
        )

    private fun percentFormatter(): (Double) -> String =
        { it.format(maximumFractionDigits = 0, percent = true) }

    private fun updateValues(updater: (EpubPreferences) -> EpubPreferences) {
        preferences = updater(preferences)
        settings = settingsResolver.settings(preferences)
        readiumCssLayout = Layout.from(settings)
    }

    private fun isHyphensEffective() = layout == EpubLayout.REFLOWABLE &&
        readiumCssLayout.stylesheets == Layout.Stylesheets.Default &&
        !settings.publisherStyles


    private fun isLetterSpacing() = layout == EpubLayout.REFLOWABLE &&
        readiumCssLayout.stylesheets == Layout.Stylesheets.Default &&
        !settings.publisherStyles

    private fun isLigaturesSpacing() = layout == EpubLayout.REFLOWABLE &&
        readiumCssLayout.stylesheets == Layout.Stylesheets.Rtl &&
        !settings.publisherStyles

    private fun isParagraphIndentEffective() = layout == EpubLayout.REFLOWABLE &&
        readiumCssLayout.stylesheets in listOf(Layout.Stylesheets.Default, Layout.Stylesheets.Rtl) &&
        !settings.publisherStyles

    private fun isTextAlignEffective() = layout == EpubLayout.REFLOWABLE &&
        readiumCssLayout.stylesheets in listOf(Layout.Stylesheets.Default, Layout.Stylesheets.Rtl) &&
        !settings.publisherStyles

    private fun isWordSpacingEffective(): Boolean = layout == EpubLayout.REFLOWABLE &&
        readiumCssLayout.stylesheets == Layout.Stylesheets.Default &&
        !settings.publisherStyles
}
