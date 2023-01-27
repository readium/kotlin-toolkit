/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts.android

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
internal class AndroidTtsSettingsResolver(
    private val metadata: Metadata,
) {

    fun settings(preferences: AndroidTtsPreferences): AndroidTtsSettings {

        return AndroidTtsSettings(
            language = preferences.language ?: metadata.language,
            voices = preferences.voices ?: emptyMap(),
            pitch = preferences.pitch ?: 1.0,
            speed = preferences.speed ?: 1.0,
        )
    }
}
