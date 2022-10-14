/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.preferences.PreferencesDemux
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
class EpubPreferencesDemux : PreferencesDemux<EpubPreferences> {

    override fun demux(preferences: EpubPreferences): PreferencesDemux.Preferences<EpubPreferences> =
        PreferencesDemux.Preferences(
            shared = preferences.copy(
                readingProgression = null,
                language = null,
                verticalText = null
            ),
            publication = EpubPreferences(
                readingProgression = preferences.readingProgression,
                language = preferences.language,
                verticalText = preferences.verticalText
            )
        )
}
