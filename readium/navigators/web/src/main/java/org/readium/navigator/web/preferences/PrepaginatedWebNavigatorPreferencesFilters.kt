/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [PrepaginatedWebNavigatorPreferences].
 */
@ExperimentalReadiumApi
public object NavigatorSharedPreferencesFilter : PreferencesFilter<PrepaginatedWebNavigatorPreferences> {

    override fun filter(preferences: PrepaginatedWebNavigatorPreferences): PrepaginatedWebNavigatorPreferences =
        preferences.copy(
            readingProgression = null
        )
}

/**
 * Suggested filter to keep only publication-specific [PrepaginatedWebNavigatorPreferences].
 */
@ExperimentalReadiumApi
public object NavigatorPublicationPreferencesFilter : PreferencesFilter<PrepaginatedWebNavigatorPreferences> {

    override fun filter(preferences: PrepaginatedWebNavigatorPreferences): PrepaginatedWebNavigatorPreferences =
        PrepaginatedWebNavigatorPreferences(
            readingProgression = preferences.readingProgression
        )
}
