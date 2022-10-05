/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

@ExperimentalReadiumApi
internal class PsPdfKitSettingsFactory(
    private val metadata: Metadata,
    private val settingsPolicy: PsPdfKitSettingsPolicy
) {

    fun createSettings(preferences: Preferences): PsPdfKitSettings {
        val values = settingsPolicy.settings(metadata, preferences)

        return PsPdfKitSettings(
            readingProgression = readingProgressionSetting(values.readingProgression),
            scroll = scrollSetting(values.scroll),
            scrollAxis = scrollAxisSetting(values.scrollAxis),
            fit = fitSetting(values.fit),
            spread = spreadSetting(values.spread)
        )
    }

    private fun readingProgressionSetting(value: ReadingProgression): EnumSetting<ReadingProgression> =
        EnumSetting(
            key = PsPdfKitSettings.READING_PROGRESSION,
            value = value,
            values = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    private fun scrollSetting(value: Boolean): Setting<Boolean> =
        Setting(
            key = PsPdfKitSettings.SCROLL,
            value = value
        )

    private fun scrollAxisSetting(value: ScrollAxis): EnumSetting<ScrollAxis> =
        EnumSetting(
            key = PsPdfKitSettings.SCROLL_AXIS,
            value = value,
            values = ScrollAxis.values().toList(),
            activator = forcesScroll(true)
        )

    private fun fitSetting(value: Presentation.Fit, ): EnumSetting<Presentation.Fit> =
        EnumSetting(
            key = PsPdfKitSettings.FIT,
            value = value,
            values = listOf(Presentation.Fit.CONTAIN, Presentation.Fit.WIDTH)
        )

    private fun spreadSetting(value: Presentation.Spread): EnumSetting<Presentation.Spread> =
        EnumSetting(
            key = PsPdfKitSettings.SPREAD,
            value = value,
            values = listOf(Presentation.Spread.NONE, Presentation.Spread.AUTO, Presentation.Spread.BOTH),
            activator = requiresScroll(false)
        )

    private fun requiresScroll(scroll: Boolean) =
        RequirePreferenceSettingActivator(key = EpubSettings.SCROLL, value = scroll) { preferences ->
            settingsPolicy.settings(metadata, preferences).scroll
        }

    private fun forcesScroll(scroll: Boolean) =
        ForcePreferenceSettingActivator(key = EpubSettings.SCROLL, value = scroll) { preferences ->
            settingsPolicy.settings(metadata, preferences).scroll
        }
}
