/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.EnumPreferenceDelegate
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.PreferenceDelegate
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata

/**
 * Interactive editor of [PrepaginatedWebNavigatorPreferences].
 *
 * This can be used as a view model for a user preferences screen.
 *
 * @see PrepaginatedWebNavigatorPreferences
 */
@ExperimentalReadiumApi
@OptIn(InternalReadiumApi::class)
public class PrepaginatedWebNavigatorPreferencesEditor internal constructor(
    initialPreferences: PrepaginatedWebNavigatorPreferences,
    publicationMetadata: Metadata,
    defaults: PrepaginatedWebNavigatorDefaults
) : PreferencesEditor<PrepaginatedWebNavigatorPreferences> {

    private data class State(
        val preferences: PrepaginatedWebNavigatorPreferences,
        val settings: PrepaginatedWebNavigatorSettings
    )

    private val settingsResolver: PrepaginatedWebNavigatorSettingsResolver =
        PrepaginatedWebNavigatorSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: PrepaginatedWebNavigatorPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { PrepaginatedWebNavigatorPreferences() }
    }

    public val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { state.settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH, Fit.HEIGHT)
        )

    public val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    public val spreads: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { preferences.spreads },
            getEffectiveValue = { state.settings.spreads },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(spreads = value) } }
        )

    private fun updateValues(
        updater: (PrepaginatedWebNavigatorPreferences) -> PrepaginatedWebNavigatorPreferences
    ) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun PrepaginatedWebNavigatorPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
