/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
data class PdfiumSettings(
    val readingProgression: ReadingProgression,
    val scrollAxis: Axis,
    val fit: Fit
) : Configurable.Settings
