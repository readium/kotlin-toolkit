/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@Serializable
data class PsPdfKitPreferences(
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val scrollAxis: Axis? = null,
    val fit: Fit? = null,
    val spread: Spread? = null,
    val pageSpacing: Double? = null,
    val offset: Boolean? = null
) : Configurable.Preferences {

    operator fun plus(other: PsPdfKitPreferences) =
        PsPdfKitPreferences(
            readingProgression = other.readingProgression ?: readingProgression,
            scroll = other.scroll ?: scroll,
            scrollAxis = other.scrollAxis ?: scrollAxis,
            fit = other.fit ?: fit,
            spread = other.spread ?: spread,
            pageSpacing = other.pageSpacing ?: pageSpacing,
            offset = other.offset ?: offset
        )
}
