/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.pdf.PdfSettings
import org.readium.r2.navigator.pdf.PdfSettingsValues
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

/**
 * @param readingProgression
 * @param scrollAxis
 * @param fit
 */
@ExperimentalReadiumApi
data class PdfiumSettings internal constructor(
    val readingProgression: EnumSetting<ReadingProgression>,
    val scrollAxis: EnumSetting<ScrollAxis>,
    val fit: EnumSetting<Presentation.Fit>
) : PdfSettings {

    override val readingProgressionValue: ReadingProgression
        get() = readingProgression.value

    override val scrollValue: Boolean
        get() = true

    companion object {

        val FIT = Setting.Key<Presentation.Fit>("fit")
        val READING_PROGRESSION = Setting.Key<ReadingProgression>("readingProgression")
        val SCROLL_AXIS = Setting.Key<ScrollAxis>("scrollAxis")
    }
}

@ExperimentalReadiumApi
data class PdfiumSettingsValues(
    val readingProgression: ReadingProgression,
    val scrollAxis: ScrollAxis,
    val fit: Presentation.Fit
) : PdfSettingsValues
