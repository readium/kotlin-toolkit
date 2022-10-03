/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.pdf.PdfSettings
import org.readium.r2.navigator.settings.EnumSetting
import org.readium.r2.navigator.settings.ScrollAxis
import org.readium.r2.navigator.settings.Setting
import org.readium.r2.navigator.settings.ToggleSetting
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

/**
 * @param readingProgression
 * @param scroll
 * @param scrollAxis
 * @param fit
 * @param spread
 */
@ExperimentalReadiumApi
data class PsPdfKitSettings internal constructor(
    val readingProgression: EnumSetting<ReadingProgression>,
    val scroll: ToggleSetting,
    val scrollAxis: EnumSetting<ScrollAxis>,
    val fit: EnumSetting<Presentation.Fit>,
    val spread: EnumSetting<Presentation.Spread>
) : PdfSettings {

    override val readingProgressionValue: ReadingProgression
        get() = readingProgression.value

    override val scrollValue: Boolean
        get() = scroll.value

    companion object {

        val FIT = Setting.Key<Presentation.Fit>("fit")
        val READING_PROGRESSION = Setting.Key<ReadingProgression>("readingProgression")
        val SCROLL = Setting.Key<Boolean>("scroll")
        val SCROLL_AXIS = Setting.Key<ScrollAxis>("scrollAxis")
        val SPREAD = Setting.Key<Presentation.Spread>("spread")
    }
}

@ExperimentalReadiumApi
data class PsPdfKitSettingsValues(
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val scrollAxis: ScrollAxis,
    val fit: Presentation.Fit,
    val spread: Presentation.Spread
)
