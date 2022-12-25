/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested filter to keep only shared [EpubPreferences].
 */
@ExperimentalReadiumApi
object EpubSharedPreferencesFilter : PreferencesFilter<EpubPreferences> {

    override fun filter(preferences: EpubPreferences): EpubPreferences =
        preferences.copy(
            readingProgression = null,
            language = null,
            spread = null,
            verticalText = null,
        )
}

/**
 * Suggested filter to keep only publication-specific [EpubPreferences].
 */
@ExperimentalReadiumApi
object EpubPublicationPreferencesFilter : PreferencesFilter<EpubPreferences> {

    override fun filter(preferences: EpubPreferences): EpubPreferences =
        EpubPreferences(
            readingProgression = preferences.readingProgression,
            language = preferences.language,
            spread = preferences.spread,
            verticalText = preferences.verticalText,
        )
}
