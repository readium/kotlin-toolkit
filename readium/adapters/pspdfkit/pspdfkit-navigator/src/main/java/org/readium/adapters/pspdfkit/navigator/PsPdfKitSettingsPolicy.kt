/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.settings.Axis
import org.readium.r2.navigator.settings.Spread
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
internal class PsPdfKitSettingsPolicy(
    private val defaults: PsPdfKitSettingsDefaults
) {

    fun settings(metadata: Metadata, preferences: Preferences): PsPdfKitSettingsValues {
        val readingProgression: ReadingProgression =
            preferences[PsPdfKitSettings.READING_PROGRESSION]
                ?: metadata.readingProgression.takeIf { it.isHorizontal == true }
                ?: defaults.readingProgression

        val scroll: Boolean =
            preferences[PsPdfKitSettings.SCROLL]
                ?: defaults.scroll

        val scrollAxis: Axis =
            preferences[PsPdfKitSettings.SCROLL_AXIS]
                ?: Axis.VERTICAL

        val fit: Fit =
            preferences[PsPdfKitSettings.FIT] ?: when {
                !scroll || scrollAxis == Axis.HORIZONTAL -> Fit.CONTAIN
                else -> Fit.WIDTH
            }

        val spread: Spread =
            preferences[PsPdfKitSettings.SPREAD]
                ?: defaults.spread

        val offset: Boolean =
            preferences[PsPdfKitSettings.OFFSET]
                ?: defaults.offset

        val pageSpacing: Double =
            preferences[PsPdfKitSettings.PAGE_SPACING]
                ?: defaults.pageSpacing

        return PsPdfKitSettingsValues(
            readingProgression = readingProgression,
            scroll = scroll,
            scrollAxis = scrollAxis,
            fit = fit,
            spread = spread,
            pageSpacing = pageSpacing,
            offset = offset
        )
    }
}
