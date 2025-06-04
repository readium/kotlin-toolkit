/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.preferences

import org.readium.navigator.common.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [FixedWebPreferences].
 */
@ExperimentalReadiumApi
public object FixedWebSharedPreferencesFilter : PreferencesFilter<FixedWebPreferences> {

    override fun filter(preferences: FixedWebPreferences): FixedWebPreferences =
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
public object FixedWebPublicationPreferencesFilter : PreferencesFilter<FixedWebPreferences> {

    override fun filter(preferences: FixedWebPreferences): FixedWebPreferences =
        FixedWebPreferences(
            fit = preferences.fit,
            readingProgression = preferences.readingProgression,
            spreads = preferences.spreads
        )
}
