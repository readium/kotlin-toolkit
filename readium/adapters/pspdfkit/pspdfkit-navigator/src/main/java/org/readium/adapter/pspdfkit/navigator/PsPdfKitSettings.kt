/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pspdfkit.navigator

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Settings values of the PDF navigator with the PSPDFKit adapter.
 *
 * @see PsPdfKitPreferences
 */
@ExperimentalReadiumApi
public data class PsPdfKitSettings(
    val fit: Fit,
    val offsetFirstPage: Boolean,
    val pageSpacing: Double,
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val scrollAxis: Axis,
    val spread: Spread
) : Configurable.Settings
