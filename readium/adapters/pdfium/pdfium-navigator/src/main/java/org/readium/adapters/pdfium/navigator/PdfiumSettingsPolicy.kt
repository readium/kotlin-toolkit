/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.settings.Axis
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
interface PdfiumSettingsPolicy {

    fun settings(metadata: Metadata, preferences: Preferences): PdfiumSettingsValues {
        val readingProgression: ReadingProgression =
            preferences[PdfiumSettings.READING_PROGRESSION]
                ?: metadata.readingProgression.takeIf { it.isHorizontal == true }
                ?: ReadingProgression.LTR

        val scrollAxis: Axis =
            preferences[PdfiumSettings.SCROLL_AXIS]
                ?: Axis.VERTICAL

        val fit: Fit =
            preferences[PdfiumSettings.FIT] ?: when (scrollAxis) {
                Axis.HORIZONTAL -> Fit.CONTAIN
                else -> Fit.WIDTH
            }

        return PdfiumSettingsValues(
            readingProgression = readingProgression,
            scrollAxis = scrollAxis,
            fit = fit
        )
    }


    companion object {

        operator fun invoke() : PdfiumSettingsPolicy =
            object : PdfiumSettingsPolicy {}
    }
}
