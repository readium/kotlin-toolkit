/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import org.readium.r2.navigator.preferences.Configurable

/**
 * Settings values of the ExoPlayer engine.
 *
 * @see ExoPlayerPreferences
 */
public data class ExoPlayerSettings(
    val pitch: Double,
    val speed: Double,
) : Configurable.Settings
