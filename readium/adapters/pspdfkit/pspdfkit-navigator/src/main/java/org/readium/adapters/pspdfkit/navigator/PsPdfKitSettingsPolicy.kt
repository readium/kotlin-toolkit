/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.settings.ScrollAxis
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

@ExperimentalReadiumApi
internal object PsPdfKitSettingsPolicy {

    fun settingsValues(metadata: Metadata, preferences: Preferences): PsPdfKitSettingsValues {
        val readingProgression: ReadingProgression =
            preferences[PsPdfKitSettings.READING_PROGRESSION]
                ?: metadata.readingProgression.takeIf { it.isHorizontal == true }
                ?: ReadingProgression.LTR

        val scroll: Boolean =
            preferences[PsPdfKitSettings.SCROLL] ?: false

        val scrollAxis: ScrollAxis =
            preferences[PsPdfKitSettings.SCROLL_AXIS]
                ?: ScrollAxis.VERTICAL

        val fit: Presentation.Fit =
            preferences[PsPdfKitSettings.FIT] ?: when {
                !scroll || scrollAxis == ScrollAxis.HORIZONTAL -> Presentation.Fit.CONTAIN
                else -> Presentation.Fit.WIDTH
            }

        val spread: Presentation.Spread =
            preferences[PsPdfKitSettings.SPREAD]
                ?: Presentation.Spread.AUTO

        return PsPdfKitSettingsValues(
            readingProgression = readingProgression,
            scroll = scroll,
            scrollAxis = scrollAxis,
            fit = fit,
            spread = spread,
        )
    }
}
