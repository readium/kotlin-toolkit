/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.pdf.PdfSettings
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Language

/**
 * @param readingProgression
 * @param scrollAxis
 * @param fit
 */
@ExperimentalReadiumApi
data class PdfiumSettings internal constructor(
    override val readingProgression: EnumSetting<ReadingProgression>,
    override val scrollAxis: EnumSetting<Axis>,
    override val fit: EnumSetting<Fit>
) : Configurable.Settings, FixedLayoutSettings {

    override val scroll: Setting<Boolean>? = null
    override val spread: EnumSetting<Spread>? = null
    override val offset: Setting<Boolean>? = null
    override val language: Setting<Language?>? = null
    override val pageSpacing: RangeSetting<Double>? = null

    companion object {

        val FIT = Setting.Key<Fit>("fit")
        val READING_PROGRESSION = Setting.Key<ReadingProgression>("readingProgression")
        val SCROLL_AXIS = Setting.Key<Axis>("scrollAxis")
    }

}

/**
 * @param readingProgression
 */
@ExperimentalReadiumApi
data class PdfiumSettingsDefaults(
    val readingProgression: ReadingProgression = ReadingProgression.LTR,
)

internal data class PdfiumSettingsValues(
    val readingProgression: ReadingProgression,
    val scrollAxis: Axis,
    val fit: Fit
) : PdfSettings
