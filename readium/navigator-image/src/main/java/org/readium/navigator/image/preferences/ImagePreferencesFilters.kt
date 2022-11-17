/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.image.preferences

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [ImagePreferences].
 */
@ExperimentalReadiumApi
object ImageSharedPreferencesFilter : PreferencesFilter<ImagePreferences> {

    override fun filter(preferences: ImagePreferences): ImagePreferences =
        preferences.copy(
            readingProgression = null,
        )
}

/**
 * Suggested filter to keep only publication-specific [ImagePreferences].
 */
@ExperimentalReadiumApi
object ImagePublicationPreferencesFilter : PreferencesFilter<ImagePreferences> {

    override fun filter(preferences: ImagePreferences): ImagePreferences =
        ImagePreferences(
            readingProgression = preferences.readingProgression,
        )
}
