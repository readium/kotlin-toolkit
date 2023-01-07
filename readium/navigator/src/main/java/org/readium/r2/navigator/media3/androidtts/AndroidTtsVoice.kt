/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Represents a voice provided by the TTS engine which can speak an utterance.
 *
 * @param id Unique and stable identifier for this voice. Can be used to store and retrieve the
 * voice from the user preferences.
 * @param name Human-friendly name for this voice, when available.
 * @param language Language (and region) this voice belongs to.
 * @param quality Voice quality.
 * @param requiresNetwork Indicates whether using this voice requires an Internet connection.
 */
@ExperimentalReadiumApi
data class AndroidTtsVoice(
    val id: String,
    val name: String? = null,
    val language: Language,
    val quality: Quality = Quality.Normal,
    val requiresNetwork: Boolean = false,
) {
    enum class Quality {
        Lowest, Low, Normal, High, Highest
    }
}
