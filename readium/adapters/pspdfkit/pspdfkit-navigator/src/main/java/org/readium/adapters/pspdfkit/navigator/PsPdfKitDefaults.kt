/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
data class PsPdfKitDefaults(
    val pageSpacing: Double = 16.0,
    val pageSpacingRange: ClosedRange<Double> = 0.0..50.0,
    val readingProgression: ReadingProgression = ReadingProgression.LTR,
    val scroll: Boolean = false,
    val spread: Spread = Spread.AUTO,
    val offset: Boolean = true
)
