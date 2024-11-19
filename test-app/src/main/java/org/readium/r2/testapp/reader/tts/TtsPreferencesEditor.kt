/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.navigator.media.tts.android.AndroidTtsPreferencesEditor
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class)
class TtsPreferencesEditor(
    private val editor: AndroidTtsPreferencesEditor,
    private val availableVoices: Set<AndroidTtsEngine.Voice>,
) : PreferencesEditor<AndroidTtsPreferences> {

    override val preferences: AndroidTtsPreferences
        get() = editor.preferences

    override fun clear() {
        editor.clear()
    }

    val language: Preference<Language?> =
        editor.language

    val pitch: RangePreference<Double> =
        editor.pitch

    val speed: RangePreference<Double> =
        editor.speed

    /**
     * [AndroidTtsPreferencesEditor] supports choosing voices for any language or region.
     * For this test app, we've chosen to present to the user only the voice for the
     * TTS default language and to ignore regions.
     */
    val voice: EnumPreference<AndroidTtsEngine.Voice.Id?> = run {
        val currentLanguage = language.effectiveValue?.removeRegion()

        editor.voices.map(
            from = { voices ->
                currentLanguage?.let { voices[it] }
            },
            to = { voice ->
                currentLanguage
                    ?.let { editor.voices.value.orEmpty().update(it, voice) }
                    ?: editor.voices.value.orEmpty()
            }
        ).withSupportedValues(
            availableVoices
                .filter { it.language.removeRegion() == currentLanguage }
                .map { it.id }
        )
    }

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
