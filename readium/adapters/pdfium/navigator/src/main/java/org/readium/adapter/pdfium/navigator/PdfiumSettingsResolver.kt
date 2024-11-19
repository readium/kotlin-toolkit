/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression

internal class PdfiumSettingsResolver(
    private val metadata: Metadata,
    private val defaults: PdfiumDefaults,
) {

    fun settings(preferences: PdfiumPreferences): PdfiumSettings {
        val readingProgression: ReadingProgression =
            preferences.readingProgression
                ?: when (metadata.readingProgression) {
                    PublicationReadingProgression.LTR -> ReadingProgression.LTR
                    PublicationReadingProgression.RTL -> ReadingProgression.RTL
                    else -> null
                } ?: defaults.readingProgression
                ?: ReadingProgression.LTR

        val scrollAxis: Axis =
            preferences.scrollAxis
                ?: Axis.VERTICAL

        val fit: Fit =
            preferences.fit ?: when (scrollAxis) {
                Axis.HORIZONTAL -> Fit.CONTAIN
                else -> Fit.WIDTH
            }

        val pageSpacing: Double =
            preferences.pageSpacing
                ?: defaults.pageSpacing
                ?: 16.0

        return PdfiumSettings(
            fit = fit,
            pageSpacing = pageSpacing,
            readingProgression = readingProgression,
            scrollAxis = scrollAxis
        )
    }
}
