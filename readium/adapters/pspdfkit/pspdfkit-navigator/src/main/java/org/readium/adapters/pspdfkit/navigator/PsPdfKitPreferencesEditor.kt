/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
class PsPdfKitPreferencesEditor internal constructor(
    initialPreferences: PsPdfKitPreferences,
    publicationMetadata: Metadata,
    defaults: PsPdfKitDefaults,
    configuration: Configuration
) : PreferencesEditor<PsPdfKitPreferences> {

    data class Configuration(
        val pageSpacingRange: ClosedRange<Double> = 0.0..50.0,
        val pageSpacingProgression: ProgressionStrategy<Double> = DoubleIncrement(5.0),
    )

    private val settingsResolver: PsPdfKitSettingsResolver =
        PsPdfKitSettingsResolver(publicationMetadata, defaults)

    private var settings: PsPdfKitSettings =
        settingsResolver.settings(initialPreferences)

    override var preferences: PsPdfKitPreferences =
        initialPreferences
        private set

    override fun clear() {
        updateValues { PsPdfKitPreferences() }
    }

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scroll: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.scroll },
            getEffectiveValue = { settings.scroll },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(scroll = value) } },
        )

    val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceDelegate(
            getValue = { preferences.scrollAxis },
            getEffectiveValue = { settings.scrollAxis },
            getIsEffective = { settings.scroll },
            updateValue = { value -> updateValues { it.copy(scrollAxis = value) } },
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
        )

    val spread: EnumPreference<Spread> =
        EnumPreferenceDelegate(
            getValue = { preferences.spread },
            getEffectiveValue = { settings.spread },
            getIsEffective = { !settings.scroll },
            updateValue = { value -> updateValues { it.copy(spread = value) } },
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.ALWAYS),
        )

    val offset: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.offset },
            getEffectiveValue = { settings.offset },
            getIsEffective = { settings.spread != Spread.NEVER},
            updateValue = { value -> updateValues { it.copy(offset = value) } },
        )

    val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH),
        )

    val pageSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pageSpacing },
            getEffectiveValue = { settings.pageSpacing },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(pageSpacing = value) } },
            supportedRange = configuration.pageSpacingRange,
            progressionStrategy = configuration.pageSpacingProgression,
            valueFormatter = { it.format(1) },
        )

    private fun updateValues(updater: (PsPdfKitPreferences) -> PsPdfKitPreferences) {
        preferences = updater(preferences)
        settings = settingsResolver.settings(preferences)
    }
}
