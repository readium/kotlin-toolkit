/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [NavigatorPreferences].
 */
@ExperimentalReadiumApi
public object NavigatorSharedPreferencesFilter : PreferencesFilter<NavigatorPreferences> {

    override fun filter(preferences: NavigatorPreferences): NavigatorPreferences =
        preferences.copy(
            readingProgression = null
        )
}

/**
 * Suggested filter to keep only publication-specific [NavigatorPreferences].
 */
@ExperimentalReadiumApi
public object NavigatorPublicationPreferencesFilter : PreferencesFilter<NavigatorPreferences> {

    override fun filter(preferences: NavigatorPreferences): NavigatorPreferences =
        NavigatorPreferences(
            readingProgression = preferences.readingProgression
        )
}
