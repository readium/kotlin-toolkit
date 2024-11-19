/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts.android

import java.util.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Language

@OptIn(ExperimentalReadiumApi::class)
internal class AndroidTtsSettingsResolver(
    private val metadata: Metadata,
    private val defaults: AndroidTtsDefaults,
) : AndroidTtsEngine.SettingsResolver {

    override fun settings(preferences: AndroidTtsPreferences): AndroidTtsSettings {
        val language = preferences.language
            ?: metadata.language
            ?: defaults.language
            ?: Language(Locale.getDefault())

        return AndroidTtsSettings(
            language = language,
            voices = preferences.voices ?: emptyMap(),
            pitch = preferences.pitch ?: defaults.pitch ?: 1.0,
            speed = preferences.speed ?: defaults.speed ?: 1.0,
            overrideContentLanguage = preferences.language != null
        )
    }
}
