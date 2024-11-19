/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.exoplayer.audio

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.DoubleIncrement
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.RangePreferenceDelegate
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata

/**
 * Editor for a set of [ExoPlayerPreferences].
 *
 * Use [ExoPlayerPreferencesEditor] to assist you in building a preferences user interface or modifying
 * existing preferences. It includes rules for adjusting preferences, such as the supported values
 * or ranges.
 */
public class ExoPlayerPreferencesEditor(
    initialPreferences: ExoPlayerPreferences,
    @Suppress("UNUSED_PARAMETER") publicationMetadata: Metadata,
    defaults: ExoPlayerDefaults,
) : PreferencesEditor<ExoPlayerPreferences> {

    private data class State(
        val preferences: ExoPlayerPreferences,
        val settings: ExoPlayerSettings,
    )

    private val settingsResolver: ExoPlayerSettingsResolver =
        ExoPlayerSettingsResolver(defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: ExoPlayerPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { ExoPlayerPreferences() }
    }

    public val pitch: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pitch },
            getEffectiveValue = { state.settings.pitch },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(pitch = value) } },
            supportedRange = 0.1..Double.MAX_VALUE,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { "${it.format(2)}x" }
        )

    public val speed: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.speed },
            getEffectiveValue = { state.settings.speed },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(speed = value) } },
            supportedRange = 0.1..Double.MAX_VALUE,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { "${it.format(2)}x" }
        )

    private fun updateValues(updater: (ExoPlayerPreferences) -> ExoPlayerPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun ExoPlayerPreferences.toState() =
        State(
            preferences = this,
            settings = settingsResolver.settings(this)
        )
}
