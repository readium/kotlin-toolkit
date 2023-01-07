/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
internal class AndroidTtsSettingsResolver(
    private val metadata: Metadata,
) {

    fun settings(preferences: AndroidTtsPreferences): AndroidTtsSettings {

        return AndroidTtsSettings(
            language = preferences.language ?: metadata.language,
            voiceId = preferences.voiceId,
            pitchRate = preferences.pitchRate ?: 1.0,
            speedRate = preferences.speedRate ?: 1.0,
        )
    }
}
