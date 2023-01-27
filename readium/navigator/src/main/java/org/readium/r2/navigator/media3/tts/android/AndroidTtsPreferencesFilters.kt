/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts.android

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [AndroidTtsPreferences].
 */
@ExperimentalReadiumApi
object AndroidTtsSharedPreferencesFilter : PreferencesFilter<AndroidTtsPreferences> {

    override fun filter(preferences: AndroidTtsPreferences): AndroidTtsPreferences =
        preferences.copy(
            language = null
        )
}

/**
 * Suggested filter to keep only publication-specific [AndroidTtsPreferences].
 */
@ExperimentalReadiumApi
object AndroidTtsPublicationPreferencesFilter : PreferencesFilter<AndroidTtsPreferences> {

    override fun filter(preferences: AndroidTtsPreferences): AndroidTtsPreferences =
        AndroidTtsPreferences(
            language = preferences.language
        )
}
