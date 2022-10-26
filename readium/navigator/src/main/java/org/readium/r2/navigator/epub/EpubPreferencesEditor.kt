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
class EpubPreferencesEditor(
    currentSettings: EpubSettings,
    initialPreferences: EpubPreferences,
    publicationMetadata: Metadata,
    epubLayout: EpubLayout,
    defaults: EpubDefaults,
    configuration: Configuration
): PreferencesEditor<EpubPreferences> {

    data class Configuration(
        val fontFamilies: List<FontFamily> = emptyList(),
        val pageMarginsRange: ClosedRange<Double> = 0.5..4.0,
        val pageMarginsProgression: ProgressionStrategy<Double> = DoubleIncrement(0.3)
    )

    private val settingsResolver: EpubSettingsResolver =
        EpubSettingsResolver(publicationMetadata, defaults)

    private val requireReflowable: NonEnforceableRequirement =
        NonEnforceableRequirement { layout == EpubLayout.REFLOWABLE }

    private val requireFixedLayout: NonEnforceableRequirement =
        NonEnforceableRequirement { layout == EpubLayout.FIXED }

    private val requirePublisherStylesDisabled = EnforceableRequirement(
        isSatisfied = { !settingsResolver.settings(preferences).publisherStyles },
        enforce = { publisherStyles.value = false }
    )

    private val percentFormatter: Formatter<Double> =
        Formatter { it.format(maximumFractionDigits = 0, percent = true) }

    private val percentProgression: ProgressionStrategy<Double> =
        DoubleIncrement(0.1)

    private fun requireStylesheets(stylesheets: Layout.Stylesheets) = NonEnforceableRequirement {
        val settings = settingsResolver.settings(preferences)
        Layout.from(settings).stylesheets == stylesheets
    }

    private fun requireScroll(value: Boolean) = EnforceableRequirement(
        isSatisfied = { settingsResolver.settings(preferences).scroll == value },
        enforce = { scroll.value = value }
    )

    private fun requireTheme(value: Theme) = EnforceableRequirement(
        isSatisfied = { settingsResolver.settings(preferences).theme == value },
        enforce = { theme.value = value }
    )

    val layout: EpubLayout = epubLayout

    override val preferences: EpubPreferences
        get() = EpubPreferences(
            backgroundColor = backgroundColor.value,
            columnCount = columnCount.value,
            fontFamily = fontFamily.value,
            fontSize = fontSize.value,
            hyphens =  hyphens.value,
            imageFilter = imageFilter.value,
            language = language.value,
            letterSpacing = letterSpacing.value,
            ligatures = ligatures.value,
            lineHeight = lineHeight.value,
            pageMargins = pageMargins.value,
            paragraphIndent = paragraphIndent.value,
            paragraphSpacing = paragraphSpacing.value,
            publisherStyles = publisherStyles.value,
            readingProgression = readingProgression.value,
            scroll = scroll.value,
            spread = spread.value,
            textAlign = textAlign.value,
            textColor = textColor.value,
            textNormalization = textNormalization.value,
            theme = theme.value,
            typeScale = typeScale.value,
            verticalText = verticalText.value,
            wordSpacing = wordSpacing.value
        )

    override fun clear() {
        backgroundColor.value = null
        columnCount.value = null
        fontFamily.value = null
        fontSize.value = null
        hyphens.value = null
        imageFilter.value = null
        language.value = null
        letterSpacing.value = null
        ligatures.value = null
        lineHeight.value = null
        pageMargins.value = null
        paragraphIndent.value = null
        paragraphSpacing.value = null
        publisherStyles.value = null
        readingProgression.value = null
        scroll.value = null
        spread.value = null
        textAlign.value = null
        textColor.value = null
        textNormalization.value = null
        theme.value = null
        typeScale.value = null
        verticalText.value = null
        wordSpacing.value = null
    }

    val backgroundColor: Preference<Color> =
        PreferenceImpl(
            value = initialPreferences.backgroundColor,
            effectiveValue = currentSettings.backgroundColor,
            nonEnforceableRequirement = requireReflowable,
        )

    val columnCount: EnumPreference<ColumnCount> =
        EnumPreferenceImpl(
            value = initialPreferences.columnCount,
            effectiveValue = currentSettings.columnCount,
            supportedValues = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
            nonEnforceableRequirement = requireReflowable,
            enforceableRequirement = requireScroll(false),
        )

    val fontFamily: EnumPreference<FontFamily?> =
        EnumPreferenceImpl(
            value = initialPreferences.fontFamily,
            effectiveValue = currentSettings.fontFamily,
            supportedValues = configuration.fontFamilies,
            nonEnforceableRequirement = requireReflowable,
        )

    val fontSize: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.fontSize,
            effectiveValue = currentSettings.fontSize,
            supportedRange = 0.4..5.0,
            progressionStrategy = percentProgression,
            valueFormatter = percentFormatter,
            nonEnforceableRequirement = requireReflowable,
        )

    val hyphens: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.hyphens,
            effectiveValue = currentSettings.hyphens,
            nonEnforceableRequirement = requireReflowable + requireStylesheets(Layout.Stylesheets.Default),
            enforceableRequirement = requirePublisherStylesDisabled
        )

    val imageFilter: EnumPreference<ImageFilter> =
        EnumPreferenceImpl(
            value = initialPreferences.imageFilter,
            effectiveValue = currentSettings.imageFilter,
            supportedValues = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT),
            enforceableRequirement = requireTheme(Theme.DARK),
        )

    val language: Preference<Language?> =
        PreferenceImpl(
            value = initialPreferences.language,
            effectiveValue = currentSettings.language
        )

    val letterSpacing: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.letterSpacing,
            effectiveValue = currentSettings.letterSpacing,
            supportedRange = 0.0..1.0,
            progressionStrategy = percentProgression,
            valueFormatter = percentFormatter,
            nonEnforceableRequirement = requireReflowable + requireStylesheets(Layout.Stylesheets.Default),
            enforceableRequirement = requirePublisherStylesDisabled
        )

    val ligatures: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.ligatures,
            effectiveValue = currentSettings.ligatures,
            nonEnforceableRequirement = requireReflowable + requireStylesheets(Layout.Stylesheets.Rtl),
            enforceableRequirement = requirePublisherStylesDisabled
        )

    val lineHeight: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.lineHeight,
            effectiveValue = currentSettings.lineHeight,
            supportedRange = 1.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { it.format(5) },
            nonEnforceableRequirement = requireReflowable,
            enforceableRequirement = requirePublisherStylesDisabled
        )

    val pageMargins: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.pageMargins,
            effectiveValue = currentSettings.pageMargins,
            supportedRange = configuration.pageMarginsRange,
            progressionStrategy = configuration.pageMarginsProgression,
            valueFormatter = { it.format(5) },
            nonEnforceableRequirement = requireReflowable,
            enforceableRequirement = requireScroll(false)
        )

    val paragraphIndent: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.paragraphIndent,
            effectiveValue = currentSettings.paragraphIndent,
            supportedRange = 0.0..3.0,
            progressionStrategy = DoubleIncrement(0.2),
            valueFormatter = percentFormatter,
            nonEnforceableRequirement = requireReflowable +
                requireStylesheets(Layout.Stylesheets.Default).or(requireStylesheets(Layout.Stylesheets.Rtl)),
            enforceableRequirement = requirePublisherStylesDisabled
        )

    val paragraphSpacing: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.paragraphSpacing,
            effectiveValue = currentSettings.paragraphSpacing,
            supportedRange = 0.0..2.0,
            progressionStrategy = percentProgression,
            valueFormatter = percentFormatter,
            nonEnforceableRequirement = requireReflowable,
            enforceableRequirement = requirePublisherStylesDisabled
        )

    val publisherStyles: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.publisherStyles,
            effectiveValue = currentSettings.publisherStyles,
            nonEnforceableRequirement = requireReflowable,
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceImpl(
            value = initialPreferences.readingProgression,
            effectiveValue = currentSettings.readingProgression,
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scroll: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.scroll,
            effectiveValue = currentSettings.scroll,
        )

    val spread: EnumPreference<Spread> =
        EnumPreferenceImpl(
            value = initialPreferences.spread,
            effectiveValue = currentSettings.spread,
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.ALWAYS),
            nonEnforceableRequirement = requireFixedLayout,
        )

    val textAlign: EnumPreference<TextAlign> =
        EnumPreferenceImpl(
            value = initialPreferences.textAlign,
            effectiveValue = currentSettings.textAlign,
            supportedValues = listOf(TextAlign.START, TextAlign.LEFT, TextAlign.RIGHT, TextAlign.JUSTIFY),
            nonEnforceableRequirement = requireReflowable +
                requireStylesheets(Layout.Stylesheets.Default).or(requireStylesheets(Layout.Stylesheets.Rtl)),
            enforceableRequirement = requirePublisherStylesDisabled
        )

    val textColor: Preference<Color> =
        PreferenceImpl(
            value = initialPreferences.textColor,
            effectiveValue = currentSettings.textColor,
            nonEnforceableRequirement = requireReflowable,
        )

    val textNormalization: EnumPreference<TextNormalization> =
        EnumPreferenceImpl(
            value = initialPreferences.textNormalization,
            effectiveValue = currentSettings.textNormalization,
            supportedValues = listOf(TextNormalization.NONE, TextNormalization.BOLD, TextNormalization.ACCESSIBILITY),
            nonEnforceableRequirement = requireReflowable,
        )

    val theme: EnumPreference<Theme> =
        EnumPreferenceImpl(
            value = initialPreferences.theme,
            effectiveValue = currentSettings.theme,
            supportedValues = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA),
            nonEnforceableRequirement = requireReflowable,
        )

    val typeScale: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.typeScale,
            effectiveValue = currentSettings.typeScale,
            valueFormatter = { it.format(5) },
            supportedRange = 1.0..2.0,
            progressionStrategy = StepsProgression(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
            nonEnforceableRequirement = requireReflowable,
            enforceableRequirement = requirePublisherStylesDisabled

        )

    val verticalText: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.verticalText,
            effectiveValue = currentSettings.verticalText,
            nonEnforceableRequirement = requireReflowable,
        )

    val wordSpacing: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.wordSpacing,
            effectiveValue = currentSettings.wordSpacing,
            supportedRange = 0.0..1.0,
            progressionStrategy = percentProgression,
            valueFormatter = percentFormatter,
            nonEnforceableRequirement = requireReflowable + requireStylesheets(Layout.Stylesheets.Default),
            enforceableRequirement = requirePublisherStylesDisabled
        )
}
