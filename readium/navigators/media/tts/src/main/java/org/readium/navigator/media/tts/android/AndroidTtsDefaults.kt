/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts.android

import org.readium.r2.shared.util.Language

/**
 * Default values for the Android TTS engine.
 *
 * These values will be used as a last resort by [AndroidTtsSettingsResolver]
 * when no user preference takes precedence.
 *
 * @see AndroidTtsPreferences
 */
public data class AndroidTtsDefaults(
    val language: Language? = null,
    val pitch: Double? = null,
    val speed: Double? = null,
) {
    init {
        require(pitch == null || pitch > 0)
        require(speed == null || speed > 0)
    }
}
