/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
internal class ExoPlayerSettingsResolver(
    private val metadata: Metadata,
) {

    fun settings(preferences: ExoPlayerPreferences): ExoPlayerSettings {

        return ExoPlayerSettings(
            rateMultiplier = preferences.rateMultiplier ?: 1.0,
        )
    }
}
