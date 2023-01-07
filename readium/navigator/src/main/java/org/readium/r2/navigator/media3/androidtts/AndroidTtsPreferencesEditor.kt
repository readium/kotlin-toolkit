/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
class AndroidTtsPreferencesEditor(
    initialPreferences: AndroidTtsPreferences,
    publicationMetadata: Metadata,
) : PreferencesEditor<AndroidTtsPreferences> {

    private data class State(
        val preferences: AndroidTtsPreferences,
        val settings: AndroidTtsSettings
    )

    private val settingsResolver: AndroidTtsSettingsResolver =
        AndroidTtsSettingsResolver(publicationMetadata)

    private var state: State =
        initialPreferences.toState()

    override val preferences: AndroidTtsPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { AndroidTtsPreferences() }
    }

    val language: Preference<Language?> =
        PreferenceDelegate(
            getValue = { preferences.language },
            getEffectiveValue = { state.settings.language },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(language = value) } },
        )

    val pitchRate: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pitchRate },
            getEffectiveValue = { state.settings.pitchRate },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(pitchRate = value) } },
            supportedRange = 0.0..Double.MAX_VALUE,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { it.format(1) },
        )

    val speedRate: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.speedRate },
            getEffectiveValue = { state.settings.speedRate },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(speedRate = value) } },
            supportedRange = 0.0..Double.MAX_VALUE,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { "${it.format(1)}x" },
        )

    val voiceId: Preference<String?> =
        PreferenceDelegate(
            getValue = { preferences.voiceId },
            getEffectiveValue = { state.settings.voiceId },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(voiceId = value) } },
        )

    private fun updateValues(updater: (AndroidTtsPreferences) -> AndroidTtsPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun AndroidTtsPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
