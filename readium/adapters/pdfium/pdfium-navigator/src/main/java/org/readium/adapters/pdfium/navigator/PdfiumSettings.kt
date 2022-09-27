/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.pdf.PdfSettings
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

/**
 * @param readingProgression Direction of the reading progression across resources.
 * @param scroll Indicates if the overflow of resources should be handled using scrolling
 *   instead of synthetic pagination.
 */
@ExperimentalReadiumApi
@PdfSupport
data class PdfiumSettings(
    val readingProgression: EnumSetting<ReadingProgression> = readingProgressionSetting(),
    val scroll: ToggleSetting = scrollSetting(),
    val scrollAxis: EnumSetting<Setting.ScrollAxis> = scrollAxisSetting(),
    val fit: EnumSetting<Presentation.Fit> = fitSetting()
) : PdfSettings {

    override fun update(metadata: Metadata, preferences: Preferences, defaults: Preferences): PdfiumSettings {
        val layoutResolver = PdfiumLayoutResolver(metadata, defaults)
        val layout = layoutResolver.resolve(preferences)

        return PdfiumSettings(
            readingProgression = readingProgressionSetting(layout.readingProgression),
            scroll = scrollSetting(layout.scroll),
            scrollAxis = scrollAxisSetting(layout.scrollAxis),
            fit = fitSetting(layout.fit)
        )
    }

    override val readingProgressionValue: ReadingProgression
        get() = readingProgression.value

    companion object {

        private val defaultLayout: PdfiumLayoutResolver.Layout =
            PdfiumLayoutResolver.Layout.create(
                readingProgression = ReadingProgression.LTR,
                scroll = false
            )

        /** Direction of the reading progression across resources. */
        internal fun readingProgressionSetting(
            value: ReadingProgression? = null
        ): EnumSetting<ReadingProgression> = EnumSetting(
            key = Setting.READING_PROGRESSION,
            value = value ?: defaultLayout.readingProgression,
            values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

        /**
         * Indicates if the overflow of resources should be handled using scrolling instead
         * of synthetic pagination.
         */
        internal fun scrollSetting(
            value: Boolean? = null
        ): ToggleSetting = ToggleSetting(
            key = Setting.SCROLL,
            value = value ?: defaultLayout.scroll
        )

        internal fun scrollAxisSetting(
            value: Setting.ScrollAxis? = null
        ): EnumSetting<Setting.ScrollAxis> = EnumSetting(
            key = Setting.SCROLL_AXIS,
            value = value ?: defaultLayout.scrollAxis,
            values = Setting.ScrollAxis.values().toList()
        )

        internal fun fitSetting(
            value: Presentation.Fit? = null,
        ): EnumSetting<Presentation.Fit> = EnumSetting(
            key = Setting.FIT,
            value = value ?: defaultLayout.fit,
            values = Presentation.Fit.values().toList()
        )
    }
}
