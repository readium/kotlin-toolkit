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
    epubLayout: EpubLayout
): PreferencesEditor<EpubPreferences> {

    private val settingsPolicy: EpubSettingsPolicy =
        EpubSettingsPolicy(publicationMetadata)

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
        EnumPreferenceImpl(
            value = initialPreferences.readingProgression,
            effectiveValue = currentSettings.readingProgression,
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
            isActiveDelegate = { true },
            activateDelegate = {}
        )

    val scroll: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.scroll,
            effectiveValue = currentSettings.scroll,
            isActiveDelegate = { true },
            activateDelegate = {}
        )

    val spread: EnumPreference<Spread> =
        EnumPreferenceImpl(
            value = initialPreferences.spread,
            effectiveValue = currentSettings.spread,
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.PREFERRED),
            isActiveDelegate = { !settingsPolicy.settings(preferences).scroll },
            activateDelegate = { scroll.value = true }
        )

    val wordSpacing: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.wordSpacing,
            effectiveValue = currentSettings.wordSpacing,
            supportedRange = 0.0..1.0,
            progressionStrategy = DoubleIncrement(0.1),
            isActiveDelegate = {
                val settings = settingsPolicy.settings(preferences)
                val stylesheets = Layout.from(settings).stylesheets
                !settings.publisherStyles &&  stylesheets == Layout.Stylesheets.Default
            },
            activateDelegate = {},
            formatValueDelegate = { it.format(maximumFractionDigits = 0) }
        )

    val fontSize: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.fontSize,
            effectiveValue = currentSettings.fontSize,
            supportedRange = 0.4..5.0,
            isActiveDelegate = { layout == EpubLayout.REFLOWABLE },
            activateDelegate = {},
            formatValueDelegate = { it.format(maximumFractionDigits = 0) },
            progressionStrategy = DoubleIncrement(0.1)
        )

    val textNormalization: EnumPreference<TextNormalization> =
        EnumPreferenceImpl(
            value = initialPreferences.textNormalization,
            effectiveValue = currentSettings.textNormalization,
            supportedValues = listOf(TextNormalization.NONE, TextNormalization.BOLD,
                TextNormalization.ACCESSIBILITY),
            isActiveDelegate = { layout == EpubLayout.REFLOWABLE },
            activateDelegate = {}
        )
}

