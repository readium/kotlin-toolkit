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
    val scroll: Boolean,
    val scrollAxis: Axis,
) : Configurable.Settings {

    internal companion object
}

/**
 * Indicates whether the pages are laid out along the horizontal axis.
 */
internal val PdfiumSettings.isHorizontal: Boolean get() = PdfiumSettings.isHorizontalLayout(scroll, scrollAxis)

/**
 * Indicates whether the pages are laid out along the horizontal axis, for the given [scroll] and
 * [scrollAxis] settings.
 *
 * Paginated layouts are always horizontal, [scrollAxis] is only taken into account in scroll mode.
 */
internal fun PdfiumSettings.Companion.isHorizontalLayout(scroll: Boolean, scrollAxis: Axis): Boolean =
    !scroll || scrollAxis == Axis.HORIZONTAL
