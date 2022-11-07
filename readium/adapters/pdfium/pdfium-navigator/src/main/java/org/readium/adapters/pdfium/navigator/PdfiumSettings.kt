/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 *  Settings values of the PDF navigator with Pdfium adapter.
 */
@ExperimentalReadiumApi
data class PdfiumSettings(
    val readingProgression: ReadingProgression,
    val scrollAxis: Axis,
    val fit: Fit,
    val pageSpacing: Double,
) : Configurable.Settings
