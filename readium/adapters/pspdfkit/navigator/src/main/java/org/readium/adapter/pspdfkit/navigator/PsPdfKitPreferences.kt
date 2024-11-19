/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pspdfkit.navigator

import kotlinx.serialization.Serializable
import org.readium.r2.navigator.preferences.*

/**
 * Preferences for the PDF navigator with the PSPDFKit adapter.
 *
 *  @param fit Indicates how pages should be laid out within the viewport.
 *  @param offsetFirstPage Indicates if the first page should be displayed in its own spread.
 *  @param pageSpacing Space between pages in dp.
 *  @param readingProgression Direction of the horizontal progression across pages.
 *  @param scroll Indicates if pages should be handled using scrolling instead of pagination.
 *  @param scrollAxis Indicates the axis along which pages should be laid out in scroll mode.
 *  @param spread Indicates if the publication should be rendered with a synthetic spread (dual-page).
 */
@Serializable
public data class PsPdfKitPreferences(
    val fit: Fit? = null,
    val offsetFirstPage: Boolean? = null,
    val pageSpacing: Double? = null,
    val readingProgression: ReadingProgression? = null,
    val scroll: Boolean? = null,
    val scrollAxis: Axis? = null,
    val spread: Spread? = null,
) : Configurable.Preferences<PsPdfKitPreferences> {

    init {
        require(fit in listOf(null, Fit.CONTAIN, Fit.WIDTH))
        require(pageSpacing == null || pageSpacing >= 0)
    }

    override operator fun plus(other: PsPdfKitPreferences): PsPdfKitPreferences =
        PsPdfKitPreferences(
            fit = other.fit ?: fit,
            offsetFirstPage = other.offsetFirstPage ?: offsetFirstPage,
            pageSpacing = other.pageSpacing ?: pageSpacing,
            readingProgression = other.readingProgression ?: readingProgression,
            scroll = other.scroll ?: scroll,
            scrollAxis = other.scrollAxis ?: scrollAxis,
            spread = other.spread ?: spread
        )
}
