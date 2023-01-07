/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.media3.tts2.TtsPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
@Serializable
data class AndroidTtsPreferences(
    override val language: Language? = null,
    val voiceId: String? = null,
    val pitchRate: Double? = null,
    val speedRate: Double? = null,
) : TtsPreferences<AndroidTtsPreferences> {

    override fun plus(other: AndroidTtsPreferences): AndroidTtsPreferences =
        AndroidTtsPreferences(
            language = other.language ?: language,
            voiceId = other.voiceId ?: voiceId,
            speedRate = other.speedRate ?: speedRate,
        )
}
