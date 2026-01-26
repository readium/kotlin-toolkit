/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.preferences

import kotlinx.coroutines.flow.StateFlow
import org.readium.navigator.common.PreferencesEditor
import org.readium.navigator.media.readaloud.SystemTtsEngine
import org.readium.navigator.media.readaloud.preferences.ReadAloudPreferences
import org.readium.navigator.media.readaloud.preferences.ReadAloudSettings
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.map
import org.readium.r2.navigator.preferences.withSupportedValues
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class)
class ReadAloudPreferencesEditor(
    private val editor: org.readium.navigator.media.readaloud.preferences.ReadAloudPreferencesEditor,
    private val availableVoices: Set<SystemTtsEngine.Voice>,
) : PreferencesEditor<ReadAloudPreferences, ReadAloudSettings> {

    override val preferences: ReadAloudPreferences
        get() = editor.preferences

    val preferencesState: StateFlow<ReadAloudPreferences> =
        editor.preferencesState

    override val settings: ReadAloudSettings
        get() = editor.settings

    override fun clear() {
        editor.clear()
    }

    val language: Preference<Language?> =
        editor.language

    /**
     * [ReadAloudPreferencesEditor] supports choosing voices for any language or region.
     * For this test app, we've chosen to present to the user only the voice for the
     * TTS default language and to ignore regions.
     */
    val voice: EnumPreference<SystemTtsEngine.Voice?> = run {
        val currentLanguage = language.effectiveValue?.removeRegion()

        editor.voices.map(
            from = { voiceIds ->
                currentLanguage
                    ?.let { voiceIds[it] }
                    ?.let { voiceId -> availableVoices.firstOrNull { it.id == voiceId } }
            },
            to = { voice ->
                currentLanguage
                    ?.let { editor.voices.value.orEmpty().update(it, voice?.id) }
                    ?: editor.voices.value.orEmpty()
            }
        ).withSupportedValues(
            availableVoices
                .filter { voice -> currentLanguage in voice.languages.map { it.removeRegion() } }
        )
    }
    val pitch: RangePreference<Double> =
        editor.pitch

    val speed: RangePreference<Double> =
        editor.speed

    val readContinuously: Preference<Boolean> =
        editor.readContinuously

    private fun <K, V> Map<K, V>.update(key: K, value: V?): Map<K, V> =
        buildMap {
            putAll(this@update)
            if (value == null) {
                remove(key)
            } else {
                put(key, value)
            }
        }
}
