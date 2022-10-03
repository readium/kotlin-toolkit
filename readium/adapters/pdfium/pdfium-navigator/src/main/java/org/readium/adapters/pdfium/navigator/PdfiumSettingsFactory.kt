/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.settings.EnumSetting
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.navigator.settings.ScrollAxis
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

@ExperimentalReadiumApi
internal class PdfiumSettingsFactory(
    private val metadata: Metadata,
    private val settingsPolicy: PdfiumSettingsPolicy
) {
    fun createSettings(preferences: Preferences): PdfiumSettings {
        val values = settingsPolicy.settings(metadata, preferences)

        return PdfiumSettings(
            readingProgression = readingProgressionSetting(values.readingProgression),
            scrollAxis = scrollAxisSetting(values.scrollAxis),
            fit = fitSetting(values.fit),
        )
    }

    private fun readingProgressionSetting(value: ReadingProgression): EnumSetting<ReadingProgression> =
        EnumSetting(
            key = PdfiumSettings.READING_PROGRESSION,
            value = value,
            values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    private fun scrollAxisSetting(value: ScrollAxis): EnumSetting<ScrollAxis> =
        EnumSetting(
            key = PdfiumSettings.SCROLL_AXIS,
            value = value,
            values = ScrollAxis.values().toList()
        )

    private fun fitSetting(value: Presentation.Fit): EnumSetting<Presentation.Fit> =
        EnumSetting(
            key = PdfiumSettings.FIT,
            value = value,
            values = listOf(Presentation.Fit.CONTAIN, Presentation.Fit.WIDTH)
        )
}
