/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.preferences

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata

/**
 * Interactive editor of [NavigatorPreferences].
 *
 * This can be used as a view model for a user preferences screen.
 *
 * @see NavigatorPreferences
 */
@ExperimentalReadiumApi
@OptIn(InternalReadiumApi::class)
public class NavigatorPreferencesEditor internal constructor(
    initialPreferences: NavigatorPreferences,
    publicationMetadata: Metadata,
    defaults: NavigatorDefaults
) : PreferencesEditor<NavigatorPreferences> {

    private data class State(
        val preferences: NavigatorPreferences,
        val settings: NavigatorSettings
    )

    private val settingsResolver: NavigatorSettingsResolver =
        NavigatorSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: NavigatorPreferences
        get() = state.preferences

    override fun clear() {
        updateValues { NavigatorPreferences() }
    }

    public val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { state.settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH, Fit.HEIGHT, Fit.COVER)
        )

    public val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    private fun updateValues(updater: (NavigatorPreferences) -> NavigatorPreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }

    private fun NavigatorPreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
