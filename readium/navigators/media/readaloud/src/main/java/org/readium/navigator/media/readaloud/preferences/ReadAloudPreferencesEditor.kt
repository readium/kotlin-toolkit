/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.readaloud.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.media.readaloud.TtsVoice
import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.DoubleIncrement
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.PreferenceDelegate
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.RangePreferenceDelegate
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Language

/**
 * Editor for a set of [ReadAloudPreferences].
 *
 * Use [ReadAloudPreferencesEditor] to assist you in building a preferences user interface or modifying
 * existing preferences. It includes rules for adjusting preferences, such as the supported values
 * or ranges.
 */
@ExperimentalReadiumApi
public class ReadAloudPreferencesEditor(
    initialPreferences: ReadAloudPreferences,
    publicationMetadata: Metadata,
    defaults: ReadAloudDefaults,
) : PreferencesEditor<ReadAloudPreferences, ReadAloudSettings> {

    private data class State(
        val preferences: ReadAloudPreferences,
        val settings: ReadAloudSettings,
    )

    private val coroutineScope: CoroutineScope =
        MainScope()
    private val settingsResolver: ReadAloudSettingsResolver =
        ReadAloudSettingsResolver(publicationMetadata, defaults)

    private var state: MutableStateFlow<State> =
        MutableStateFlow(initialPreferences.toState())

    override val preferences: ReadAloudPreferences
        get() = state.value.preferences

    override val settings: ReadAloudSettings
        get() = state.value.settings

    public val preferencesState: StateFlow<ReadAloudPreferences> =
        state.mapStateIn(coroutineScope) { it.preferences }

    override fun clear() {
        updateValues { ReadAloudPreferences() }
    }

    public val language: Preference<Language?> =
        PreferenceDelegate(
            getValue = { preferences.language },
            getEffectiveValue = { state.value.settings.language },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(language = value) } }
        )

    public val pitch: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pitch },
            getEffectiveValue = { state.value.settings.pitch },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(pitch = value) } },
            supportedRange = 0.1..Double.MAX_VALUE,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { "${it.format(2)}x" }
        )

    public val speed: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.speed },
            getEffectiveValue = { state.value.settings.speed },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(speed = value) } },
            supportedRange = 0.1..Double.MAX_VALUE,
            progressionStrategy = DoubleIncrement(0.1),
            valueFormatter = { "${it.format(2)}x" }
        )

    public val voices: Preference<Map<Language, TtsVoice.Id>> =
        PreferenceDelegate(
            getValue = { preferences.voices },
            getEffectiveValue = { state.value.settings.voices },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(voices = value) } }
        )

    public val readContinuously: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.readContinuously },
            getEffectiveValue = { state.value.settings.readContinuously },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readContinuously = value) } }
        )

    private fun updateValues(updater: (ReadAloudPreferences) -> ReadAloudPreferences) {
        val newPreferences = updater(preferences)
        state.value = newPreferences.toState()
    }

    private fun ReadAloudPreferences.toState(): State {
        return State(
            preferences = this,
            settings = settingsResolver.settings(this),
        )
    }
}
