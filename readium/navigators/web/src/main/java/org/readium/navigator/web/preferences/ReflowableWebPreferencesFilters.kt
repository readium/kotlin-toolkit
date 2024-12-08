/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [ReflowableWebPreferences].
 */
@ExperimentalReadiumApi
public object ReflowableWebSharedPreferencesFilter : PreferencesFilter<ReflowableWebPreferences> {

    override fun filter(preferences: ReflowableWebPreferences): ReflowableWebPreferences =
        preferences.copy(
            fit = null,
            readingProgression = null,
            spreads = null
        )
}

/**
 * Suggested filter to keep only publication-specific [FixedWebPreferences].
 */
@ExperimentalReadiumApi
public object ReflowableWebPublicationPreferencesFilter : PreferencesFilter<ReflowableWebPreferences> {

    override fun filter(preferences: ReflowableWebPreferences): ReflowableWebPreferences =
        ReflowableWebPreferences(
            fit = preferences.fit,
            readingProgression = preferences.readingProgression,
            spreads = preferences.spreads
        )
}
