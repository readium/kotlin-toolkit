/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pspdfkit.navigator

import org.readium.r2.navigator.preferences.PreferencesFilter

/**
 * Suggested filter to keep only shared [PsPdfKitPreferences].
 */
public object PsPdfKitSharedPreferencesFilter : PreferencesFilter<PsPdfKitPreferences> {

    override fun filter(preferences: PsPdfKitPreferences): PsPdfKitPreferences =
        preferences.copy(
            readingProgression = null,
            offsetFirstPage = null,
            spread = null
        )
}

/**
 * Suggested filter to keep only publication-specific [PsPdfKitPreferences].
 */
public object PsPdfKitPublicationPreferencesFilter : PreferencesFilter<PsPdfKitPreferences> {

    override fun filter(preferences: PsPdfKitPreferences): PsPdfKitPreferences =
        PsPdfKitPreferences(
            readingProgression = preferences.readingProgression,
            offsetFirstPage = preferences.offsetFirstPage,
            spread = preferences.spread
        )
}
