/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
@Serializable
data class PdfiumPreferences(
    val readingProgression: ReadingProgression? = null,
    val scrollAxis: Axis? = null,
    val fit: Fit? = null,
) : Configurable.Preferences {

    operator fun plus(other: PdfiumPreferences) =
        PdfiumPreferences(
            readingProgression = other.readingProgression ?: readingProgression,
            scrollAxis = other.scrollAxis ?: scrollAxis,
            fit = other.fit ?: fit,
        )
}
