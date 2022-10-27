/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
object PsPdfKitSharedPreferencesFilter : PreferencesFilter<PsPdfKitPreferences> {

    override fun filter(preferences: PsPdfKitPreferences): PsPdfKitPreferences =
        preferences.copy(
            readingProgression = null,
            offset = null
        )

}

@ExperimentalReadiumApi
object PsPdfKitPublicationPreferencesFilter : PreferencesFilter<PsPdfKitPreferences> {

    override fun filter(preferences: PsPdfKitPreferences): PsPdfKitPreferences =
        PsPdfKitPreferences(
            readingProgression = preferences.readingProgression,
            offset = preferences.offset
        )
}
