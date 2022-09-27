/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.settings.Setting
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

@OptIn(ExperimentalReadiumApi::class)
internal class PdfiumLayoutResolver(
    private val metadata: Metadata = Metadata(localizedTitle = LocalizedString("fake title")),
    private val defaults: Preferences = Preferences()
) {
    data class Layout(
        val readingProgression: ReadingProgression = ReadingProgression.LTR,
        val scroll: Boolean = false,
        val scrollAxis: Setting.ScrollAxis = Setting.ScrollAxis.VERTICAL,
        val fit: Presentation.Fit = Presentation.Fit.CONTAIN
    ) {

        companion object {
            fun create(
                readingProgression: ReadingProgression,
                scroll: Boolean,
                scrollAxis: Setting.ScrollAxis? = null,
                fit: Presentation.Fit? = null
            ): Layout {
                val actualScrollAxis = scrollAxis
                    ?: Setting.ScrollAxis.VERTICAL

                val actualFit = fit ?: when {
                    !scroll -> Presentation.Fit.CONTAIN
                    scrollAxis == Setting.ScrollAxis.HORIZONTAL -> Presentation.Fit.HEIGHT
                    else -> Presentation.Fit.WIDTH
                }

                return Layout(readingProgression, scroll, actualScrollAxis, actualFit)
            }
        }
    }

    fun resolve(preferences: Preferences = Preferences()): Layout {
        val readingProgressionSetting: Setting<ReadingProgression> =
            PdfiumSettings.readingProgressionSetting()
        val scrollSetting: Setting<Boolean> =
            PdfiumSettings.scrollSetting()
        val scrollAxisSetting: Setting<Setting.ScrollAxis> =
            PdfiumSettings.scrollAxisSetting()
        val fitSetting: Setting<Presentation.Fit> =
            PdfiumSettings.fitSetting()

        val rpDefault: ReadingProgression? =
            readingProgressionSetting.firstValidValue(defaults)
        val scrollDefault: Boolean? =
            scrollSetting.firstValidValue(defaults)
        val scrollAxisDefault: Setting.ScrollAxis? =
            scrollAxisSetting.firstValidValue(defaults)
        val fitDefault: Presentation.Fit? =
            fitSetting.firstValidValue(defaults)

        val rpPref = readingProgressionSetting.firstValidValue(preferences)
        val scrollPref = scrollSetting.firstValidValue(preferences)
        val scrollAxisPref = scrollAxisSetting.firstValidValue(preferences)
        val fitPref = fitSetting.firstValidValue(preferences)

        val readingProgression: ReadingProgression = rpPref
            ?: metadata.readingProgression.takeIf { it.isHorizontal == true }
            ?: rpDefault
            ?: ReadingProgression.LTR

        val scroll: Boolean = scrollPref
            ?: scrollDefault
            ?: false

        val scrollAxis: Setting.ScrollAxis? = scrollAxisPref
            ?: scrollAxisDefault

        val fit: Presentation.Fit? = fitPref
            ?: fitDefault

        return Layout.create(
            readingProgression = readingProgression,
            scroll = scroll,
            scrollAxis = scrollAxis,
            fit = fit
        )
    }
}