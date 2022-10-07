/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.pdf.PdfSettingsValues
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression

/**
 * @param readingProgression
 * @param scroll
 * @param scrollAxis
 * @param fit
 * @param spread
 * @param pageSpacing
 * @param offset
 */
@ExperimentalReadiumApi
data class PsPdfKitSettings internal constructor(
    val readingProgression: EnumSetting<ReadingProgression>,
    val scroll: Setting<Boolean>,
    val scrollAxis: EnumSetting<Axis>,
    val fit: EnumSetting<Fit>,
    val spread: EnumSetting<Spread>,
    val pageSpacing: RangeSetting<Double>,
    val offset: Setting<Boolean>
) : Configurable.Settings {

    companion object {

        val FIT = Setting.Key<Fit>("fit")
        val READING_PROGRESSION = Setting.Key<ReadingProgression>("readingProgression")
        val SCROLL = Setting.Key<Boolean>("scroll")
        val SCROLL_AXIS = Setting.Key<Axis>("scrollAxis")
        val SPREAD = Setting.Key<Spread>("spread")
        val PAGE_SPACING = Setting.Key<Double>("pageSpacing")
        val OFFSET = Setting.Key<Boolean>("offset")
    }
}

@ExperimentalReadiumApi
data class PsPdfKitSettingsValues(
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val scrollAxis: Axis,
    val fit: Fit,
    val spread: Spread,
    val pageSpacing: Double,
    val offset: Boolean
) : PdfSettingsValues
