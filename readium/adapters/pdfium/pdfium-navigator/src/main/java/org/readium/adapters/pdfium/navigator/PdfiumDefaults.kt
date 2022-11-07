/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.navigator.preferences.ReadingProgression

/**
 * Default values for the PDF navigator with Pdfium adapter.
 */
@ExperimentalReadiumApi
data class PdfiumDefaults(
    val readingProgression: ReadingProgression? = null,
    val pageSpacing: Double? = null
)
