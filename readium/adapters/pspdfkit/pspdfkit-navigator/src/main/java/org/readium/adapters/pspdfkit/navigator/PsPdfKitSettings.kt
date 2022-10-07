/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.pdf.PdfSettingsValues
import org.readium.r2.navigator.settings.EnumSetting
import org.readium.r2.navigator.settings.Axis
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.navigator.settings.Setting
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
    val scroll: Setting<Boolean>,
    val scrollAxis: EnumSetting<Axis>,
    val fit: EnumSetting<Presentation.Fit>,
    val spread: EnumSetting<Presentation.Spread>
) : Configurable.Settings {

    companion object {

        val FIT = Setting.Key<Presentation.Fit>("fit")
        val READING_PROGRESSION = Setting.Key<ReadingProgression>("readingProgression")
        val SCROLL = Setting.Key<Boolean>("scroll")
        val SCROLL_AXIS = Setting.Key<Axis>("scrollAxis")
        val SPREAD = Setting.Key<Presentation.Spread>("spread")
    }
}

@ExperimentalReadiumApi
data class PsPdfKitSettingsValues(
    val readingProgression: ReadingProgression,
    val scroll: Boolean,
    val scrollAxis: Axis,
    val fit: Presentation.Fit,
    val spread: Presentation.Spread
) : PdfSettingsValues
