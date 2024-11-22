/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression

/**
 * Preferences for the PDF navigator with the PDFium adapter.
 *
 *  @param fit Indicates how pages should be laid out within the viewport.
 *  @param pageSpacing Space between pages in dp.
 *  @param readingProgression Direction of the horizontal progression across pages.
 *  @param scrollAxis Indicates the axis along which pages should be laid out in scroll mode.
 */
@Serializable
public data class PdfiumPreferences(
    val fit: Fit? = null,
    val pageSpacing: Double? = null,
    val readingProgression: ReadingProgression? = null,
    val scrollAxis: Axis? = null,
) : Configurable.Preferences<PdfiumPreferences> {

    init {
        require(fit in listOf(null, Fit.CONTAIN, Fit.WIDTH))
        require(pageSpacing == null || pageSpacing >= 0)
    }

    override operator fun plus(other: PdfiumPreferences): PdfiumPreferences =
        PdfiumPreferences(
            fit = other.fit ?: fit,
            pageSpacing = other.pageSpacing ?: pageSpacing,
            readingProgression = other.readingProgression ?: readingProgression,
            scrollAxis = other.scrollAxis ?: scrollAxis
        )
}
