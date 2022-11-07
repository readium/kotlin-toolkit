/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Preferences for the PDF navigator with Pdfium adapter
 */
@ExperimentalReadiumApi
@Serializable
data class PdfiumPreferences(
    val readingProgression: ReadingProgression? = null,
    val scrollAxis: Axis? = null,
    val fit: Fit? = null,
    val pageSpacing: Double? = null
) : Configurable.Preferences {

    init {
        require(fit in listOf(null, Fit.CONTAIN, Fit.WIDTH))
        require(pageSpacing == null || pageSpacing >= 0)
    }

    operator fun plus(other: PdfiumPreferences) =
        PdfiumPreferences(
            readingProgression = other.readingProgression ?: readingProgression,
            scrollAxis = other.scrollAxis ?: scrollAxis,
            fit = other.fit ?: fit,
            pageSpacing = other.pageSpacing ?: pageSpacing
        )
}
