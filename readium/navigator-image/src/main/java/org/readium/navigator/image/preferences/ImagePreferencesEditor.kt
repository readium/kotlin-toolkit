/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.image.preferences

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

/**
 * Interactive editor of [ImagePreferences].
 *
 * This can be used as a view model for a user preferences screen.
 *
 * @see ImagePreferences
 */
@ExperimentalReadiumApi
class ImagePreferencesEditor internal constructor(
    initialPreferences: ImagePreferences,
    publicationMetadata: Metadata,
    defaults: ImageDefaults,
) : PreferencesEditor<ImagePreferences> {
    
    private data class State(
        val preferences: ImagePreferences,
        val settings: ImageSettings
    )

    private val settingsResolver: ImageSettingsResolver =
        ImageSettingsResolver(publicationMetadata, defaults)

    private var state: State =
        initialPreferences.toState()

    override val preferences: ImagePreferences
        get() = state.preferences

    override fun clear() {
        updateValues { ImagePreferences() }
    }

    val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { state.settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH, Fit.HEIGHT, Fit.COVER),
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { state.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scroll: SwitchPreference =
        SwitchPreferenceDelegate(
            getValue = { preferences.scroll },
            getEffectiveValue = { state.settings.scroll },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(scroll = value) } },
        )

    val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceDelegate(
            getValue = { preferences.scrollAxis },
            getEffectiveValue = { state.settings.scrollAxis },
            getIsEffective = { state.settings.scroll },
            updateValue = { value -> updateValues { it.copy(scrollAxis = value) } },
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
        )

    private fun updateValues(updater: (ImagePreferences) -> ImagePreferences) {
        val newPreferences = updater(preferences)
        state = newPreferences.toState()
    }
    
    private fun ImagePreferences.toState() =
        State(preferences = this, settings = settingsResolver.settings(this))
}
