/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.exoplayer

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Default values for the ExoPlayer engine.
 *
 * These values will be used as a last resort by [ExoPlayerSettingsResolver]
 * when no user preference takes precedence.
 *
 * @see ExoPlayerPreferences
 */
@ExperimentalReadiumApi
public data class ExoPlayerDefaults(
    val pitch: Double? = null,
    val speed: Double? = null
) {
    init {
        require(pitch == null || pitch > 0)
        require(speed == null || speed > 0)
    }
}
