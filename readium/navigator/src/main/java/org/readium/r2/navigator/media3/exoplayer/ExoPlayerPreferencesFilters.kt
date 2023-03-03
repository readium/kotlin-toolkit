/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [ExoPlayerPreferences].
 */
@ExperimentalReadiumApi
object ExoPlayerSharedPreferencesFilter : PreferencesFilter<ExoPlayerPreferences> {

    override fun filter(preferences: ExoPlayerPreferences): ExoPlayerPreferences =
        preferences.copy()
}

/**
 * Suggested filter to keep only publication-specific [ExoPlayerPreferences].
 */
@ExperimentalReadiumApi
object ExoPlayerPublicationPreferencesFilter : PreferencesFilter<ExoPlayerPreferences> {

    override fun filter(preferences: ExoPlayerPreferences): ExoPlayerPreferences =
        ExoPlayerPreferences()
}
