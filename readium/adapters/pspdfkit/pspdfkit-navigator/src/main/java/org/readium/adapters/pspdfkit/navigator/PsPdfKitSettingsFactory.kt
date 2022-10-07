/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

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
            spread = spreadSetting(values.spread),
            pageSpacing = pageSpacingSetting(values.pageSpacing),
            offset = offsetSetting(values.offset)
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

    private fun scrollAxisSetting(value: Axis): EnumSetting<Axis> =
        EnumSetting(
            key = PsPdfKitSettings.SCROLL_AXIS,
            value = value,
            values = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
            activator = forcesScroll(true)
        )

    private fun fitSetting(value: Fit, ): EnumSetting<Fit> =
        EnumSetting(
            key = PsPdfKitSettings.FIT,
            value = value,
            values = listOf(Fit.CONTAIN, Fit.WIDTH)
        )

    private fun spreadSetting(value: Spread): EnumSetting<Spread> =
        EnumSetting(
            key = PsPdfKitSettings.SPREAD,
            value = value,
            values = listOf(Spread.AUTO, Spread.NEVER, Spread.PREFERRED),
            activator = requiresScroll(false)
        )

    private fun offsetSetting(value: Boolean): Setting<Boolean> =
        Setting(
            key = PsPdfKitSettings.OFFSET,
            value = value,
            activator = requiresSpreadPossible()
        )

    private fun pageSpacingSetting(value: Double): RangeSetting<Double> =
        RangeSetting(
            key = PsPdfKitSettings.PAGE_SPACING,
            value = value,
            range = 0.0..50.0,
            suggestedProgression = DoubleIncrement(5.0)
        )

    private fun requiresSpreadPossible() = object : SettingActivator {

            override fun isActiveWithPreferences(preferences: Preferences): Boolean {
                val value = settingsPolicy.settings(metadata, preferences).spread
                return value in listOf(Spread.AUTO, Spread.PREFERRED)
            }

            override fun activateInPreferences(preferences: MutablePreferences) {
                // Do nothing
            }
        }

    private fun requiresScroll(scroll: Boolean) =
        RequirePreferenceSettingActivator(value = scroll) { preferences ->
            settingsPolicy.settings(metadata, preferences).scroll
        }

    private fun forcesScroll(scroll: Boolean) =
        ForcePreferenceSettingActivator(key = EpubSettings.SCROLL, value = scroll) { preferences ->
            settingsPolicy.settings(metadata, preferences).scroll
        }
}
