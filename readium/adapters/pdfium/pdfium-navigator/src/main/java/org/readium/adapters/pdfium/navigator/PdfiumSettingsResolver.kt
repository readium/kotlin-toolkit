/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
internal class PdfiumSettingsResolver(
    private val metadata: Metadata,
    private val defaults: PdfiumDefaults
) {

    fun settings(preferences: PdfiumPreferences): PdfiumSettings {
        val readingProgression: ReadingProgression =
            preferences.readingProgression
                ?: metadata.readingProgression.takeIf { it.isHorizontal == true }
                ?: defaults.readingProgression
                ?: ReadingProgression.LTR

        val scrollAxis: Axis =
            preferences.scrollAxis
                ?: Axis.VERTICAL

        val fit: Fit =
            preferences.fit ?: when (scrollAxis) {
                Axis.HORIZONTAL -> Fit.CONTAIN
                else -> Fit.WIDTH
            }

        return PdfiumSettings(
            readingProgression = readingProgression,
            scrollAxis = scrollAxis,
            fit = fit
        )
    }
}
