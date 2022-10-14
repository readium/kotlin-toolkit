/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
internal class PsPdfKitSettingsPolicy(
    private val defaults: PsPdfKitDefaults,
    private val metadata: Metadata
) {

    fun settings(preferences: PsPdfKitPreferences): PsPdfKitSettings {
        val readingProgression: ReadingProgression =
            preferences.readingProgression
                ?: metadata.readingProgression.takeIf { it.isHorizontal == true }
                ?: defaults.readingProgression

        val scroll: Boolean =
            preferences.scroll
                ?: defaults.scroll

        val scrollAxis: Axis =
            preferences.scrollAxis
                ?: Axis.VERTICAL

        val fit: Fit =
            preferences.fit ?: when {
                !scroll || scrollAxis == Axis.HORIZONTAL -> Fit.CONTAIN
                else -> Fit.WIDTH
            }

        val spread: Spread =
            preferences.spread
                ?: defaults.spread

        val offset: Boolean =
            preferences.offset
                ?: defaults.offset

        val pageSpacing: Double =
            preferences.pageSpacing
                ?: defaults.pageSpacing

        return PsPdfKitSettings(
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
