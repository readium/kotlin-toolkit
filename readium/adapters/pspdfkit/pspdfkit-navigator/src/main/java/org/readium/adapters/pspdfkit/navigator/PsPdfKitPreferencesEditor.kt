/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression
import java.text.NumberFormat

@ExperimentalReadiumApi
class PsPdfKitPreferencesEditor(
    currentSettings: PsPdfKitSettings,
    initialPreferences: PsPdfKitPreferences,
    publicationMetadata: Metadata,
    defaults: PsPdfKitDefaults,
    configuration: Configuration
) : PreferencesEditor<PsPdfKitPreferences> {

    data class Configuration(
        val pageSpacingRange: ClosedRange<Double> = 0.0..50.0,
        val pageSpacingProgression: ProgressionStrategy<Double> = DoubleIncrement(5.0),
    )

    private val settingsPolicy: PsPdfKitSettingsPolicy =
        PsPdfKitSettingsPolicy(defaults, publicationMetadata)

    override var preferences: PsPdfKitPreferences = initialPreferences
        private set

    override fun clear() {
        preferences = PsPdfKitPreferences()
    }

    val readingProgression: EnumPreference<ReadingProgression> =
        object : AbstractEnumPreference<ReadingProgression>(
            currentSettings.readingProgression,
            listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        ) {
            override fun get(): ReadingProgression? = preferences.readingProgression

            override fun set(value: ReadingProgression?) {
                preferences = preferences.copy(readingProgression = value) }

            override val isActive: Boolean = true
        }

    val scroll: SwitchPreference =
        object : AbstractSwitchPreference(
            currentSettings.scroll
        ) {
            override var value: Boolean?
                get() = preferences.scroll
                set(value) { preferences = preferences.copy(scroll = value) }

            override val isActive: Boolean = true
        }

    val scrollAxis: EnumPreference<Axis> =
        object : AbstractEnumPreference<Axis>(
            currentSettings.scrollAxis,
            listOf(Axis.VERTICAL, Axis.HORIZONTAL),
        ) {
            override fun get(): Axis? = preferences.scrollAxis

            override fun set(value: Axis?) {
                preferences = preferences.copy(scrollAxis = value)
            }

            override val isActive: Boolean
                get() = settingsPolicy.settings(preferences).scroll
        }

    val spread: EnumPreference<Spread> =
        object : AbstractEnumPreference<Spread>(
            currentSettings.spread,
            listOf(Spread.AUTO, Spread.NEVER, Spread.PREFERRED),
        ) {
            override fun get(): Spread? = preferences.spread

            override fun set(value: Spread?) {
                preferences = preferences.copy(spread = value)
            }

            override val isActive: Boolean
                get() = !settingsPolicy.settings(preferences).scroll
        }

    val offset: SwitchPreference =
        object : AbstractSwitchPreference(
            currentSettings.offset
        ) {
            override var value: Boolean?
                get() = preferences.offset
                set(value) { preferences = preferences.copy(offset = value) }

            override val isActive: Boolean
                get() = settingsPolicy.settings(preferences).spread != Spread.NEVER
        }

    val fit: EnumPreference<Fit> =
        object : AbstractEnumPreference<Fit>(
            currentSettings.fit,
            listOf(Fit.CONTAIN, Fit.WIDTH),
        ) {
            override fun get(): Fit? = preferences.fit

            override fun set(value: Fit?) { preferences = preferences.copy(fit = value) }

            override val isActive: Boolean = true
        }

    val pageSpacing: RangePreference<Double> =
        object : AbstractRangePreference<Double>(
            currentSettings.pageSpacing,
            configuration.pageSpacingRange,
            configuration.pageSpacingProgression,
        ) {
            override fun get(): Double? = preferences.pageSpacing

            override fun set(value: Double?) { preferences = preferences.copy(pageSpacing = value) }

            override val isActive: Boolean
                get() = settingsPolicy.settings(preferences).scroll

            override fun formatValue(value: Double): String =
                NumberFormat.getNumberInstance().run {
                    maximumFractionDigits = 1
                    format(value)
                }
        }
}
