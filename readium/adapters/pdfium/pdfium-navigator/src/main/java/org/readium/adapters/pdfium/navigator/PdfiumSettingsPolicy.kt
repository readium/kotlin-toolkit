/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.settings.ScrollAxis
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

@ExperimentalReadiumApi
interface PdfiumSettingsPolicy {

    fun settings(metadata: Metadata, preferences: Preferences): PdfiumSettingsValues {
        val readingProgression: ReadingProgression =
            preferences[PdfiumSettings.READING_PROGRESSION]
                ?: metadata.readingProgression.takeIf { it.isHorizontal == true }
                ?: ReadingProgression.LTR

        val scrollAxis: ScrollAxis =
            preferences[PdfiumSettings.SCROLL_AXIS]
                ?: ScrollAxis.VERTICAL

        val fit: Presentation.Fit =
            preferences[PdfiumSettings.FIT] ?: when (scrollAxis) {
                ScrollAxis.HORIZONTAL -> Presentation.Fit.CONTAIN
                else -> Presentation.Fit.WIDTH
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
