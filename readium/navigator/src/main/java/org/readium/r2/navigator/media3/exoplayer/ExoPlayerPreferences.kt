/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@kotlinx.serialization.Serializable
data class ExoPlayerPreferences(
    val rateMultiplier: Double? = null,
) : Configurable.Preferences<ExoPlayerPreferences> {

    override fun plus(other: ExoPlayerPreferences): ExoPlayerPreferences =
        ExoPlayerPreferences(
            rateMultiplier = other.rateMultiplier ?: rateMultiplier,
        )
}
