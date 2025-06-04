/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.preferences

import org.readium.navigator.common.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [ReflowableWebPreferences].
 */
@ExperimentalReadiumApi
public object ReflowableWebSharedPreferencesFilter : PreferencesFilter<ReflowableWebPreferences> {

    override fun filter(preferences: ReflowableWebPreferences): ReflowableWebPreferences =
        preferences.copy(
            readingProgression = null,
            language = null,
            verticalText = null
        )
}

/**
 * Suggested filter to keep only publication-specific [ReflowableWebPreferences].
 */
@ExperimentalReadiumApi
public object ReflowableWebPublicationPreferencesFilter : PreferencesFilter<ReflowableWebPreferences> {

    override fun filter(preferences: ReflowableWebPreferences): ReflowableWebPreferences =
        ReflowableWebPreferences(
            readingProgression = preferences.readingProgression,
            language = preferences.language,
            verticalText = preferences.verticalText
        )
}
