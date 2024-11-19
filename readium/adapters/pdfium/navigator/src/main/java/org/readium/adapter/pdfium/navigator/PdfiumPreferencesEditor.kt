/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.pdfium.navigator

import org.readium.r2.navigator.extensions.format
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata

/**
 * Editor for a set of [PdfiumPreferences].
 *
 * Use [PdfiumPreferencesEditor] to assist you in building a preferences user interface or modifying
 * existing preferences. It includes rules for adjusting preferences, such as the supported values
 * or ranges.
 */
public class PdfiumPreferencesEditor internal constructor(
    initialPreferences: PdfiumPreferences,
    publicationMetadata: Metadata,
    defaults: PdfiumDefaults,
) : PreferencesEditor<PdfiumPreferences> {

    private data class State(
        val preferences: PdfiumPreferences,
        val settings: PdfiumSettings,
    )

    private val settingsResolver: PdfiumSettingsResolver =
        PdfiumSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: PdfiumPreferences
        get() = state.preferences

    /**
     * Reset all preferences.
     */
    override fun clear() {
        updateValues { PdfiumPreferences() }
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
     * Indicates the axis along which pages should be laid out in scroll mode.
     */
    public val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceDelegate(
            getValue = { preferences.scrollAxis },
            getEffectiveValue = { state.settings.scrollAxis },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(scrollAxis = value) } },
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL)
        )

    private fun updateValues(updater: (PdfiumPreferences) -> PdfiumPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun PdfiumPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
