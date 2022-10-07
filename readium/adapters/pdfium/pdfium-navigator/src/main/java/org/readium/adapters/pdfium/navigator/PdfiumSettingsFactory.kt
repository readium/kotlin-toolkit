/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.settings.Axis
import org.readium.r2.navigator.settings.EnumSetting
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

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

    private fun scrollAxisSetting(value: Axis): EnumSetting<Axis> =
        EnumSetting(
            key = PdfiumSettings.SCROLL_AXIS,
            value = value,
            values = listOf(Axis.VERTICAL, Axis.HORIZONTAL)
        )

    private fun fitSetting(value: Fit): EnumSetting<Fit> =
        EnumSetting(
            key = PdfiumSettings.FIT,
            value = value,
            values = listOf(Fit.CONTAIN, Fit.WIDTH)
        )
}
