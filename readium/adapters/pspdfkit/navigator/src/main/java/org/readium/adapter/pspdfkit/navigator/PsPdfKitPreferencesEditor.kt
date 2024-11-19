/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.pspdfkit.navigator

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.DoubleIncrement
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.EnumPreferenceDelegate
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.PreferenceDelegate
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.preferences.RangePreference
import org.readium.r2.navigator.preferences.RangePreferenceDelegate
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata

/**
 * Editor for a set of [PsPdfKitPreferences].
 *
 * Use [PsPdfKitPreferencesEditor] to assist you in building a preferences user interface or modifying
 * existing preferences. It includes rules for adjusting preferences, such as the supported values
 * or ranges.
 */
public class PsPdfKitPreferencesEditor internal constructor(
    initialPreferences: PsPdfKitPreferences,
    publicationMetadata: Metadata,
    defaults: PsPdfKitDefaults,
) : PreferencesEditor<PsPdfKitPreferences> {

    private data class State(
        val preferences: PsPdfKitPreferences,
        val settings: PsPdfKitSettings,
    )

    private val settingsResolver: PsPdfKitSettingsResolver =
        PsPdfKitSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: PsPdfKitPreferences
        get() = state.preferences

    /**
     * Reset all preferences.
     */
    override fun clear() {
        updateValues { PsPdfKitPreferences() }
    }

    /**
     * Indicates how pages should be laid out within the viewport.
     */
    public val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { state.settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH)
        )

    /**
     * Indicates if the first page should be displayed in its own spread.
     *
     * Only effective when:
     *  - [scroll] is off
     *  - [spread] are not disabled
     */
    public val offsetFirstPage: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.offsetFirstPage },
            getEffectiveValue = { state.settings.offsetFirstPage },
            getIsEffective = { !state.settings.scroll && state.settings.spread != Spread.NEVER },
            updateValue = { value -> updateValues { it.copy(offsetFirstPage = value) } }
        )

    /**
     * Space between pages in dp.
     */
    public val pageSpacing: RangePreference<Double> =
        RangePreferenceDelegate(
            getValue = { preferences.pageSpacing },
            getEffectiveValue = { state.settings.pageSpacing },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(pageSpacing = value) } },
            supportedRange = 0.0..50.0,
            progressionStrategy = DoubleIncrement(5.0),
            valueFormatter = { "${it.format(1)} dp" }
        )

    /**
     * Direction of the horizontal progression across pages.
     */
    public val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    /**
     * Indicates if pages should be handled using scrolling instead of pagination.
     */
    public val scroll: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.scroll },
            getEffectiveValue = { state.settings.scroll },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(scroll = value) } }
        )

    /**
     * Indicates the axis along which pages should be laid out in scroll mode.
     *
     * Only effective when [scroll] is on.
     */
    public val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceDelegate(
            getValue = { preferences.scrollAxis },
            getEffectiveValue = { state.settings.scrollAxis },
            getIsEffective = { state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(scrollAxis = value) } },
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL)
        )

    /**
     * Indicates if the publication should be rendered with a synthetic spread (dual-page).
     *
     * Only effective when [scroll] is off.
     */
    public val spread: EnumPreference<Spread> =
        EnumPreferenceDelegate(
            getValue = { preferences.spread },
            getEffectiveValue = { state.settings.spread },
            getIsEffective = { !state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(spread = value) } },
            supportedValues = listOf(Spread.AUTO, Spread.NEVER, Spread.ALWAYS)
        )

    private fun updateValues(updater: (PsPdfKitPreferences) -> PsPdfKitPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun PsPdfKitPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
