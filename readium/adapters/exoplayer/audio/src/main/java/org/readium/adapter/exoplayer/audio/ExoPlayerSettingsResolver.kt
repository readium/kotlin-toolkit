/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal class ExoPlayerSettingsResolver(
    private val defaults: ExoPlayerDefaults,
) : ExoPlayerEngine.SettingsResolver {

    override fun settings(preferences: ExoPlayerPreferences): ExoPlayerSettings {
        return ExoPlayerSettings(
            pitch = preferences.pitch ?: defaults.pitch ?: 1.0,
            speed = preferences.speed ?: defaults.speed ?: 1.0
        )
    }
}
