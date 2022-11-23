/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

/**
 * Interactive editor of [PsPdfKitPreferences].
 *
 * This can be used as a view model for a user preferences screen.
 *
 * @see PsPdfKitPreferences
 */
@ExperimentalReadiumApi
class PsPdfKitPreferencesEditor internal constructor(
    initialPreferences: PsPdfKitPreferences,
    publicationMetadata: Metadata,
    defaults: PsPdfKitDefaults,
    configuration: Configuration
) : PreferencesEditor<PsPdfKitPreferences> {

    /**
     * Configuration for [PsPdfKitPreferencesEditor].
     *
     * @param pageSpacingRange The allowed range for page spacing.
     * @param pageSpacingProgression The progression strategy for page spacing.
     */
    data class Configuration(
        val pageSpacingRange: ClosedRange<Double> = 0.0..50.0,
        val pageSpacingProgression: ProgressionStrategy<Double> = DoubleIncrement(5.0),
    )
    
    private data class State(
        val preferences: PsPdfKitPreferences,
        val settings: PsPdfKitSettings
    )

    private val settingsResolver: PsPdfKitSettingsResolver =
        PsPdfKitSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: PsPdfKitPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { PsPdfKitPreferences() }
    }

    val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { state.settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH),
        )

    val offsetFirstPage: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.offsetFirstPage },
            getEffectiveValue = { state.settings.offsetFirstPage },
            getIsEffective = { !state.settings.scroll && state.settings.spread != Spread.NEVER},
            updateValue = { value -> updateValues { it.copy(offsetFirstPage = value) } },
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scroll: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.scroll },
            getEffectiveValue = { state.settings.scroll },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(scroll = value) } },
        )

    val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceDelegate(
            getValue = { preferences.scrollAxis },
            getEffectiveValue = { state.settings.scrollAxis },
            getIsEffective = { state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(scrollAxis = value) } },
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
        )

    val spread: EnumPreference<Spread> =
        EnumPreferenceDelegate(
            getValue = { preferences.spread },
            getEffectiveValue = { state.settings.spread },
            getIsEffective = { !state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(spread = value) } },
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.ALWAYS),
        )

    val pageSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pageSpacing },
            getEffectiveValue = { state.settings.pageSpacing },
            getIsEffective = { state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(pageSpacing = value) } },
            supportedRange = configuration.pageSpacingRange,
            progressionStrategy = configuration.pageSpacingProgression,
            valueFormatter = { "${it.format(1)} dp" },
        )

    private fun updateValues(updater: (PsPdfKitPreferences) -> PsPdfKitPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }
    
    private fun PsPdfKitPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
