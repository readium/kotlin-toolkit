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

    private val settingsResolver: PsPdfKitSettingsResolver =
        PsPdfKitSettingsResolver(publicationMetadata, defaults)

    private fun requireScroll(value: Boolean) = EnforceableRequirement(
        isSatisfied = { settingsResolver.settings(preferences).scroll == value },
        enforce = { scroll.value = value }
    )

    private val requireSpreadPossible = EnforceableRequirement(
        isSatisfied = { settingsResolver.settings(preferences).spread != Spread.NEVER },
        enforce = { spread.value = Spread.AUTO }
    )

    override val preferences: PsPdfKitPreferences
        get() = PsPdfKitPreferences(
            readingProgression = readingProgression.value,
            scroll = scroll.value,
            scrollAxis = scrollAxis.value,
            fit = fit.value,
            spread = spread.value,
            pageSpacing = pageSpacing.value,
            offset = offset.value
        )

    override fun clear() {
        readingProgression.value = null
        scroll.value = null
        scrollAxis.value = null
        fit.value = null
        spread.value = null
        pageSpacing.value = null
        offset.value = null
    }

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceImpl(
            value = initialPreferences.readingProgression,
            effectiveValue = currentSettings.readingProgression,
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scroll: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.scroll,
            effectiveValue = currentSettings.scroll,
        )

    val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceImpl(
            value = initialPreferences.scrollAxis,
            effectiveValue = currentSettings.scrollAxis,
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
            enforceableRequirement = requireScroll(true),
        )

    val spread: EnumPreference<Spread> =
        EnumPreferenceImpl(
            value = initialPreferences.spread,
            effectiveValue = currentSettings.spread,
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.ALWAYS),
            enforceableRequirement = requireScroll(false),
        )

    val offset: SwitchPreference =
        SwitchPreferenceImpl(
            value = initialPreferences.offset,
            effectiveValue = currentSettings.offset,
            enforceableRequirement = requireSpreadPossible
        )

    val fit: EnumPreference<Fit> =
        EnumPreferenceImpl(
            value = initialPreferences.fit,
            effectiveValue = currentSettings.fit,
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH),
        )

    val pageSpacing: RangePreference<Double> =
        RangePreferenceImpl(
            value = initialPreferences.pageSpacing,
            effectiveValue = currentSettings.pageSpacing,
            supportedRange = configuration.pageSpacingRange,
            progressionStrategy = configuration.pageSpacingProgression,
            valueFormatter = { it.format(1) },
            enforceableRequirement = requireScroll(true),
        )
}
