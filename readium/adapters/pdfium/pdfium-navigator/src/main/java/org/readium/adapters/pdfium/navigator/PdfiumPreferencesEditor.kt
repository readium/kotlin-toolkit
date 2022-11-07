/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
class PdfiumPreferencesEditor internal constructor(
    initialPreferences: PdfiumPreferences,
    publicationMetadata: Metadata,
    defaults: PdfiumDefaults,
    configuration: Configuration
) : PreferencesEditor<PdfiumPreferences> {

    data class Configuration(
        val pageSpacingRange: ClosedRange<Double> = 0.0..50.0,
        val pageSpacingProgression: ProgressionStrategy<Double> = DoubleIncrement(5.0),
    )

    private data class State(
        val preferences: PdfiumPreferences,
        val settings: PdfiumSettings
    )

    private val settingsResolver: PdfiumSettingsResolver =
        PdfiumSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: PdfiumPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { PdfiumPreferences() }
    }

    val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { state.settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH),
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceDelegate(
            getValue = { preferences.scrollAxis},
            getEffectiveValue = { state.settings.scrollAxis },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(scrollAxis = value) } },
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
        )

    val pageSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pageSpacing },
            getEffectiveValue = { state.settings.pageSpacing },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(pageSpacing = value) } },
            supportedRange = configuration.pageSpacingRange,
            progressionStrategy = configuration.pageSpacingProgression,
            valueFormatter = { "${it.format(1)} dp" },
        )

    private fun updateValues(updater: (PdfiumPreferences) -> PdfiumPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun PdfiumPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
