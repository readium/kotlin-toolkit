/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.settings

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

interface ReadingProgressionEditor {

    var readingProgression: ReadingProgression?

    val isReadingProgressionPreferenceActive: Boolean

    val supportedReadingProgressionValues: List<ReadingProgression>
}

interface ScrollEditor {

    var scroll: Boolean?

    val isScrollPreferenceActive: Boolean

    fun toggleScroll()
}

interface ScrollAxisEditor {

    var scrollAxis: Axis?

    val isScrollAxisPreferenceActive: Boolean

    val supportedScrollAxes: List<Axis>
}

interface SpreadEditor {

    var spread: Spread?

    val isSpreadPreferenceActive: Boolean

    val supportedSpreadValues: List<Spread>

}

interface FitEditor {

    var fit: Fit?

    val isFitPreferenceActive: Boolean

    val supportedFitValues: List<Fit>

}

interface OffsetEditor {

    var offset: Boolean?

    val isOffsetPreferenceActive: Boolean

    fun toggleOffset()
}

interface LanguageEditor {

    var language: Language?

    val isLanguagePreferenceActive: Boolean
}

interface PageSpacingEditor {

    var pageSpacing: Double?

    val isPageSpacingPreferenceActive: Boolean

    val pageSpacingRange: ClosedRange<Double>

   fun incrementPageSpacing()

   fun decrementPageSpacing()

   fun formatPageSpacing(value: Double): String
}
