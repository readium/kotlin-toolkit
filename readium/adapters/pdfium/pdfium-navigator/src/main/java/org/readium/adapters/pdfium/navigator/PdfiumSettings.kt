/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.pdf.PdfSettingsValues
import org.readium.r2.navigator.settings.EnumSetting
import org.readium.r2.navigator.settings.Axis
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.navigator.settings.Setting
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression

/**
 * @param readingProgression
 * @param scrollAxis
 * @param fit
 */
@ExperimentalReadiumApi
data class PdfiumSettings internal constructor(
    val readingProgression: EnumSetting<ReadingProgression>,
    val scrollAxis: EnumSetting<Axis>,
    val fit: EnumSetting<Fit>
) : Configurable.Settings {

    companion object {

        val FIT = Setting.Key<Fit>("fit")
        val READING_PROGRESSION = Setting.Key<ReadingProgression>("readingProgression")
        val SCROLL_AXIS = Setting.Key<Axis>("scrollAxis")
    }
}

@ExperimentalReadiumApi
data class PdfiumSettingsValues(
    val readingProgression: ReadingProgression,
    val scrollAxis: Axis,
    val fit: Fit
) : PdfSettingsValues
