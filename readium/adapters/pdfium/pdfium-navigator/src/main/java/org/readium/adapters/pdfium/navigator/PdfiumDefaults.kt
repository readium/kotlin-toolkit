/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression

/**
 * @param readingProgression
 */
@ExperimentalReadiumApi
data class PdfiumDefaults(
    val readingProgression: ReadingProgression? = null,
)
