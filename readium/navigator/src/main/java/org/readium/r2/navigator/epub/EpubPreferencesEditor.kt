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
        val additionalFontFamilies: List<FontFamily> = emptyList()
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
            readingProgression = readingProgression.value,
            scroll = scroll.value,
            spread = spread.value
        )

    override fun clear() {
        readingProgression.value = null
        scroll.value = null
        spread.value = null
    }

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

    val wordSpacing: RangePreference<Double> =
        DelegatingRangePreference(
            value = initialPreferences.wordSpacing,
            effectiveValue = currentSettings.wordSpacing,
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            formatValueImpl = percentFormatter(),
            isActiveImpl = requireStylesheets(Layout.Stylesheets.Default) +
                requirePublisherDefaults(false),
            activateImpl = {},
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

    val textNormalization: EnumPreference<TextNormalization> =
        DelegatingEnumPreference(
            value = initialPreferences.textNormalization,
            effectiveValue = currentSettings.textNormalization,
            supportedValues = listOf(TextNormalization.NONE, TextNormalization.BOLD,
                TextNormalization.ACCESSIBILITY),
            isActiveImpl = requireEpubLayout(EpubLayout.REFLOWABLE),
            activateImpl = {}
        )

    private fun requireStylesheets(stylesheets: Layout.Stylesheets) = IsActive {
        val settings = settingsResolver.settings(preferences)
        Layout.from(settings).stylesheets == stylesheets
    }

    private fun requirePublisherDefaults(value: Boolean) = IsActive {
        val settings = settingsResolver.settings(preferences)
        settings.publisherStyles == value
    }

    private fun requireEpubLayout(layout: EpubLayout) = IsActive {
        layout == EpubLayout.REFLOWABLE
    }

    private fun requireScroll(scroll: Boolean) = IsActive {
        settingsResolver.settings(preferences).scroll && scroll
    }

    private fun percentFormatter(): Formatter<Double> = Formatter {
        it.format(maximumFractionDigits = 0)
    }
}
