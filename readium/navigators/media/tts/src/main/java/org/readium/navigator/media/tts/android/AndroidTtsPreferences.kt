/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts.android

import kotlinx.serialization.Serializable
import org.readium.navigator.media.tts.TtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Preferences for the the Android built-in TTS engine.
 *
 *  @param language Language of the publication content.
 *  @param pitch Playback pitch rate.
 *  @param speed Playback speed rate.
 *  @param voices Map of preferred voices for specific languages.
 */
@OptIn(ExperimentalReadiumApi::class)
@Serializable
public data class AndroidTtsPreferences(
    override val language: Language? = null,
    val pitch: Double? = null,
    val speed: Double? = null,
    val voices: Map<Language, AndroidTtsEngine.Voice.Id>? = null,
) : TtsEngine.Preferences<AndroidTtsPreferences> {

    init {
        require(pitch == null || pitch > 0)
        require(speed == null || speed > 0)
    }

    override fun plus(other: AndroidTtsPreferences): AndroidTtsPreferences =
        AndroidTtsPreferences(
            language = other.language ?: language,
            pitch = other.pitch ?: pitch,
            speed = other.speed ?: speed,
            voices = other.voices ?: voices
        )
}
