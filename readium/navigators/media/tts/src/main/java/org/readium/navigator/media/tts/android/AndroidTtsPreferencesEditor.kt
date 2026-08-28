/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.tts.android

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Language

/**
 * Editor for a set of [AndroidTtsPreferences].
 *
 * Use [AndroidTtsPreferencesEditor] to assist you in building a preferences user interface or modifying
 * existing preferences. It includes rules for adjusting preferences, such as the supported values
 * or ranges.
 */
@OptIn(ExperimentalReadiumApi::class)
public class AndroidTtsPreferencesEditor(
    initialPreferences: AndroidTtsPreferences,
    publicationMetadata: Metadata,
    defaults: AndroidTtsDefaults,
) : PreferencesEditor<AndroidTtsPreferences> {

    private data class State(
        val preferences: AndroidTtsPreferences,
        val settings: AndroidTtsSettings,
    )

    private val settingsResolver: AndroidTtsSettingsResolver =
        AndroidTtsSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: AndroidTtsPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { AndroidTtsPreferences() }
    }

    public val language: Preference<Language?> =
        PreferenceDelegate(
            getValue = { preferences.language },
            getEffectiveValue = { state.settings.language },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(language = value) } }
        )

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

    public val voices: Preference<Map<Language, AndroidTtsEngine.Voice.Id>> =
        PreferenceDelegate(
            getValue = { preferences.voices },
            getEffectiveValue = { state.settings.voices },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(voices = value) } }
        )

    private fun updateValues(updater: (AndroidTtsPreferences) -> AndroidTtsPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun AndroidTtsPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
