/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.preferences

import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression

@ExperimentalReadiumApi
internal class FixedWebSettingsResolver(
    private val metadata: Metadata,
    private val defaults: FixedWebDefaults,
) {

    fun settings(preferences: FixedWebPreferences): FixedWebSettings {
        val readingProgression: ReadingProgression =
            preferences.readingProgression
                ?: when (metadata.readingProgression) {
                    PublicationReadingProgression.LTR -> ReadingProgression.LTR
                    PublicationReadingProgression.RTL -> ReadingProgression.RTL
                    else -> null
                } ?: defaults.readingProgression
                ?: ReadingProgression.LTR

        val fit = preferences.fit ?: defaults.fit ?: Fit.CONTAIN

        val spreads = preferences.spreads ?: defaults.spreads ?: false

        return FixedWebSettings(
            fit = fit,
            readingProgression = readingProgression,
            spreads = spreads
        )
    }
}
