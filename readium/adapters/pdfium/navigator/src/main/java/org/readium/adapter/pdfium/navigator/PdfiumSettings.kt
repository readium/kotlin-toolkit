/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import org.readium.r2.navigator.preferences.*

/**
 *  Settings values of the PDF navigator with the PDFium adapter.
 *
 *  @see PdfiumPreferences
 */
public data class PdfiumSettings(
    val fit: Fit,
    val pageSpacing: Double,
    val readingProgression: ReadingProgression,
    val scrollAxis: Axis,
) : Configurable.Settings
