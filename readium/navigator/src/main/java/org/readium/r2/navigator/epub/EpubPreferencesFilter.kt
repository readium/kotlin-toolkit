/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.preferences.PreferencesFilter
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
class EpubPreferencesFilter : PreferencesFilter<EpubPreferences> {

    override fun filterSharedPreferences(preferences: EpubPreferences): EpubPreferences =
        preferences.copy(
            readingProgression = null,
            language = null,
            verticalText = null
        )

    override fun filterPublicationPreferences(preferences: EpubPreferences): EpubPreferences =
        EpubPreferences(
            readingProgression = preferences.readingProgression,
            language = preferences.language,
            verticalText = preferences.verticalText
        )
}
