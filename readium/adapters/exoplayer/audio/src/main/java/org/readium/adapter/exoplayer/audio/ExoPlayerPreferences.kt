/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import org.readium.r2.navigator.preferences.Configurable

/**
 * Preferences for the the ExoPlayer engine.
 *
 *  @param pitch Playback pitch rate.
 *  @param speed Playback speed rate.
 */
@kotlinx.serialization.Serializable
public data class ExoPlayerPreferences(
    val pitch: Double? = null,
    val speed: Double? = null,
) : Configurable.Preferences<ExoPlayerPreferences> {

    override fun plus(other: ExoPlayerPreferences): ExoPlayerPreferences =
        ExoPlayerPreferences(
            pitch = other.pitch ?: pitch,
            speed = other.speed ?: speed
        )
}
