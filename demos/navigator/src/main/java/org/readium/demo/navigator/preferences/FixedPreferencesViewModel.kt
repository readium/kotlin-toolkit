/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.preferences

import androidx.compose.runtime.Stable
import org.readium.navigator.common.PreferencesController
import org.readium.navigator.web.fixedlayout.preferences.FixedWebPreferences
import org.readium.navigator.web.fixedlayout.preferences.FixedWebSettings
import org.readium.r2.navigator.preferences.EnumPreference
import org.readium.r2.navigator.preferences.EnumPreferenceDelegate
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.Preference
import org.readium.r2.navigator.preferences.PreferenceDelegate
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
@Stable
class FixedPreferencesViewModel(
    private val controller: PreferencesController<FixedWebPreferences, FixedWebSettings>,
) : PreferencesViewModel<FixedWebPreferences, FixedWebSettings> {

    override fun clear() {
        controller.preferences = FixedWebPreferences()
    }

    val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { controller.preferences.fit },
            getEffectiveValue = { controller.settings.fit },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(fit = value)
            },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH, Fit.HEIGHT)
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { controller.preferences.readingProgression },
            getEffectiveValue = { controller.settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(readingProgression = value)
            },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL)
        )

    val spreads: Preference<Boolean> =
        PreferenceDelegate(
            getValue = { controller.preferences.spreads },
            getEffectiveValue = { controller.settings.spreads },
            getIsEffective = { true },
            updateValue = { value ->
                controller.preferences =
                    controller.preferences.copy(spreads = value)
            }
        )
}
