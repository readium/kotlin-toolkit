/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pspdfkit.navigator

import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread

/**
 * Default values for the PDF navigator with the PSPDFKit adapter.
 *
 * These values will be used when no publication metadata or user preference takes precedence.
 *
 * @see PsPdfKitPreferences
 */
public data class PsPdfKitDefaults(
    val offsetFirstPage: Boolean? = null,
    val pageSpacing: Double? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val spread: Spread? = null,
)
