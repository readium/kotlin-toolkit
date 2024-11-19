/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pspdfkit.navigator

import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression

internal class PsPdfKitSettingsResolver(
    private val metadata: Metadata,
    private val defaults: PsPdfKitDefaults,
) {
    fun settings(preferences: PsPdfKitPreferences): PsPdfKitSettings {
        val readingProgression: ReadingProgression =
            preferences.readingProgression
                ?: when (metadata.readingProgression) {
                    PublicationReadingProgression.LTR -> ReadingProgression.LTR
                    PublicationReadingProgression.RTL -> ReadingProgression.RTL
                    else -> null
                } ?: defaults.readingProgression
                ?: ReadingProgression.LTR

        val scroll: Boolean =
            preferences.scroll
                ?: defaults.scroll
                ?: false

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
                ?: Spread.AUTO

        val offsetFirstPage: Boolean =
            preferences.offsetFirstPage
                ?: defaults.offsetFirstPage
                ?: true

        val pageSpacing: Double =
            preferences.pageSpacing
                ?: defaults.pageSpacing
                ?: 16.0

        return PsPdfKitSettings(
            fit = fit,
            offsetFirstPage = offsetFirstPage,
            pageSpacing = pageSpacing,
            readingProgression = readingProgression,
            scroll = scroll,
            scrollAxis = scrollAxis,
            spread = spread
        )
    }
}
