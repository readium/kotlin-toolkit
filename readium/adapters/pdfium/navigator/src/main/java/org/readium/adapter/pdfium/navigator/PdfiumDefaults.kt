/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import org.readium.r2.navigator.preferences.ReadingProgression

/**
 * Default values for the PDF navigator with the PDFium adapter.
 *
 * These values will be used when no publication metadata or user preference takes precedence.
 *
 * @see PdfiumPreferences
 */
public data class PdfiumDefaults(
    val pageSpacing: Double? = null,
    val readingProgression: ReadingProgression? = null,
)
