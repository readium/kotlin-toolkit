/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.media3.tts2.TtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
@Serializable
data class AndroidTtsPreferences(
    override val language: Language? = null,
    val voices: Map<Language, String>? = null,
    val pitch: Double? = null,
    val speed: Double? = null,
) : TtsEngine.Preferences<AndroidTtsPreferences> {

    override fun plus(other: AndroidTtsPreferences): AndroidTtsPreferences =
        AndroidTtsPreferences(
            language = other.language ?: language,
            voices = other.voices ?: voices,
            speed = other.speed ?: speed,
            pitch = other.pitch ?: pitch
        )
}
