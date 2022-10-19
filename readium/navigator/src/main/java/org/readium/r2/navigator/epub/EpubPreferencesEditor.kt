/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.epub.extensions.format
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
    defaults: EpubNavigatorDefaults,
    configuration: Configuration
): PreferencesEditor<EpubPreferences> {

    data class Configuration(
        val ignoreDefaultFontFamilies: Boolean = false,
        val additionalFontFamilies: List<FontFamily> = emptyList(),
        val pageMarginsRange: ClosedRange<Double> = 0.5..4.0,
        val pageMarginsProgression: ProgressionStrategy<Double> = DoubleIncrement(0.1)
    )

    private val settingsResolver: EpubSettingsResolver =
        EpubSettingsResolver(publicationMetadata, defaults)

    private val defaultFontFamilies: List<FontFamily> =
        listOf(
            FontFamily.LITERATA,
            FontFamily.PT_SERIF,
            FontFamily.ROBOTO,
            FontFamily.SOURCE_SANS_PRO,
            FontFamily.VOLLKORN,
            FontFamily.ACCESSIBLE_DFA,
            FontFamily.IA_WRITER_DUOSPACE,
            FontFamily.OPEN_DYSLEXIC
        )

    private val fontFamilies: List<FontFamily> =
        defaultFontFamilies
            .takeUnless { configuration.ignoreDefaultFontFamilies }
            .orEmpty()
            .plus(configuration.additionalFontFamilies)


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
        DelegatingPreference(
            value = initialPreferences.backgroundColor,
            effectiveValue = currentSettings.backgroundColor,
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    val columnCount: EnumPreference<ColumnCount> =
        DelegatingEnumPreference(
            value = initialPreferences.columnCount,
            effectiveValue = currentSettings.columnCount,
            supportedValues = listOf(ColumnCount.AUTO, ColumnCount.ONE, ColumnCount.TWO),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    val fontFamily: EnumPreference<FontFamily?> =
        DelegatingEnumPreference(
            value = initialPreferences.fontFamily,
            effectiveValue = currentSettings.fontFamily,
            supportedValues = fontFamilies,
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    val fontSize: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.fontSize,
            effectiveValue = currentSettings.fontSize,
            supportedRange = 0.4..5.0,
            progressionStrategy = DoubleIncrement(0.1),
            formatValueImpl = percentFormatter(),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {},
        )

    val hyphens: SwitchPreference =
        DelegatingSwitchPreference(
            value = initialPreferences.hyphens,
            effectiveValue = currentSettings.hyphens,
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE) +
                requirePublisherStyles(false) +
                requireStylesheets(Layout.Stylesheets.Default),
            activateImpl = {}
        )

    val imageFilter: EnumPreference<ImageFilter> =
        DelegatingEnumPreference(
            value = initialPreferences.imageFilter,
            effectiveValue = currentSettings.imageFilter,
            supportedValues = listOf(ImageFilter.NONE, ImageFilter.DARKEN, ImageFilter.INVERT),
            isActiveImpl = requireTheme(Theme.DARK),
            activateImpl = { theme.value = Theme.DARK }
        )

    val language: Preference<Language?> =
        DelegatingPreference(
            value = initialPreferences.language,
            effectiveValue = currentSettings.language,
            isActiveImpl = { true },
            activateImpl = {}
        )

    val letterSpacing: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.letterSpacing,
            effectiveValue = currentSettings.letterSpacing,
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            formatValueImpl = percentFormatter(),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE) + requirePublisherStyles(false) +
                requireStylesheets(Layout.Stylesheets.Default),
            activateImpl = {}
        )

    val ligatures: SwitchPreference =
        DelegatingSwitchPreference(
            value = initialPreferences.ligatures,
            effectiveValue = currentSettings.ligatures,
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE) + requireStylesheets(Layout.Stylesheets.Rtl),
            activateImpl = {}
        )

    val lineHeight: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.lineHeight,
            effectiveValue = currentSettings.lineHeight,
            supportedRange = 1.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            formatValueImpl = { it.format(5) },
            isActiveImpl = requirePublisherStyles(false),
            activateImpl = { publisherStyles.value = false }
        )

    val pageMargins: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.pageMargins,
            effectiveValue = currentSettings.pageMargins,
            supportedRange = configuration.pageMarginsRange,
            progressionStrategy = configuration.pageMarginsProgression,
            formatValueImpl = { it.format(5) },
            isActiveImpl = requireScroll(false),
            activateImpl = { scroll.value = true }
        )

    val paragraphIndent: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.paragraphIndent,
            effectiveValue = currentSettings.paragraphIndent,
            supportedRange = 0.0..3.0,
            progressionStrategy = DoubleIncrement(0.2),
            formatValueImpl = percentFormatter(),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE) + requirePublisherStyles(false) +
                requireStylesheets(Layout.Stylesheets.Default).or(requireStylesheets(Layout.Stylesheets.Rtl)),
            activateImpl = { publisherStyles.value = false }
        )

    val paragraphSpacing: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.paragraphSpacing,
            effectiveValue = currentSettings.paragraphSpacing,
            supportedRange = 0.0..2.0,
            progressionStrategy = DoubleIncrement(0.1),
            formatValueImpl = percentFormatter(),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE) + requirePublisherStyles(false),
            activateImpl = { publisherStyles.value = false }
        )

    val publisherStyles: SwitchPreference =
        DelegatingSwitchPreference(
            value = initialPreferences.publisherStyles,
            effectiveValue = currentSettings.publisherStyles,
            isActiveImpl = { true },
            activateImpl = {}
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        DelegatingEnumPreference(
            value = initialPreferences.readingProgression,
            effectiveValue = currentSettings.readingProgression,
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
            isActiveImpl = { true },
            activateImpl = {}
        )

    val scroll: SwitchPreference =
        DelegatingSwitchPreference(
            value = initialPreferences.scroll,
            effectiveValue = currentSettings.scroll,
            isActiveImpl = { true },
            activateImpl = {}
        )

    val spread: EnumPreference<Spread> =
        DelegatingEnumPreference(
            value = initialPreferences.spread,
            effectiveValue = currentSettings.spread,
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.PREFERRED),
            isActiveImpl = requireScroll(false),
            activateImpl = { scroll.value = false }
        )

    val textAlign: EnumPreference<TextAlign> =
        DelegatingEnumPreference(
            value = initialPreferences.textAlign,
            effectiveValue = currentSettings.textAlign,
            supportedValues = listOf(TextAlign.START, TextAlign.LEFT, TextAlign.RIGHT, TextAlign.JUSTIFY),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE) + requirePublisherStyles(false) +
                requireStylesheets(Layout.Stylesheets.Default).or(requireStylesheets(Layout.Stylesheets.Rtl)),
            activateImpl = { publisherStyles.value = false }
        )

    val textColor: Preference<Color> =
        DelegatingPreference(
            value = initialPreferences.textColor,
            effectiveValue = currentSettings.textColor,
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    val textNormalization: EnumPreference<TextNormalization> =
        DelegatingEnumPreference(
            value = initialPreferences.textNormalization,
            effectiveValue = currentSettings.textNormalization,
            supportedValues = listOf(TextNormalization.NONE, TextNormalization.BOLD,
                TextNormalization.ACCESSIBILITY),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    val theme: EnumPreference<Theme> =
        DelegatingEnumPreference(
            value = initialPreferences.theme,
            effectiveValue = currentSettings.theme,
            supportedValues = listOf(Theme.LIGHT, Theme.DARK, Theme.SEPIA),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    val typeScale: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.typeScale,
            effectiveValue = currentSettings.typeScale,
            supportedRange = 1.0..2.0,
            progressionStrategy = StepsProgression(1.0, 1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE) + requirePublisherStyles(false),
            activateImpl = { publisherStyles.value = false },
            formatValueImpl = { it.format(5) }
        )

    val verticalText: SwitchPreference =
        DelegatingSwitchPreference(
            value = initialPreferences.verticalText,
            effectiveValue = currentSettings.verticalText,
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    val wordSpacing: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.wordSpacing,
            effectiveValue = currentSettings.wordSpacing,
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            formatValueImpl = percentFormatter(),
            isActiveImpl = requireStylesheets(Layout.Stylesheets.Default) +
                requirePublisherStyles(false),
            activateImpl = {},
        )

    private fun requireStylesheets(stylesheets: Layout.Stylesheets) = IsActive {
        val settings = settingsResolver.settings(preferences)
        Layout.from(settings).stylesheets == stylesheets
    }

    private fun requirePublisherStyles(value: Boolean) = IsActive {
        val settings = settingsResolver.settings(preferences)
        settings.publisherStyles == value
    }

    private fun requireEpubLayout(layout: EpubLayout) = IsActive {
        layout == EpubLayout.REFLOWABLE
    }

    private fun requireScroll(scroll: Boolean) = IsActive {
        settingsResolver.settings(preferences).scroll == scroll
    }

    private fun requireTheme(theme: Theme) = IsActive {
        settingsResolver.settings(preferences).theme == theme
    }

    private fun percentFormatter(): Formatter<Double> = Formatter {
        it.format(maximumFractionDigits = 0)
    }
}
