/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.preferences.PreferencesDemux
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
class PsPdfKitPreferencesDemux : PreferencesDemux<PsPdfKitPreferences> {

    override fun demux(preferences: PsPdfKitPreferences): PreferencesDemux.Preferences<PsPdfKitPreferences> =
        PreferencesDemux.Preferences(
            shared = preferences.copy(
                readingProgression = null,
                offset = null
            ),
            publication = PsPdfKitPreferences(
                readingProgression = preferences.readingProgression,
                offset = preferences.offset
            )
        )
}
