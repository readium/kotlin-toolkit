/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Fit
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression

@ExperimentalReadiumApi
class PdfiumPreferencesEditor internal constructor(
    initialPreferences: PdfiumPreferences,
    publicationMetadata: Metadata,
    defaults: PdfiumDefaults,
) : PreferencesEditor<PdfiumPreferences> {

    private val settingsResolver: PdfiumSettingsResolver =
        PdfiumSettingsResolver(publicationMetadata, defaults)

    private var settings: PdfiumSettings =
        settingsResolver.settings(initialPreferences)

    override var preferences: PdfiumPreferences =
        initialPreferences
        private set

    override fun clear() {
        updateValues { PdfiumPreferences() }
    }

    val fit: EnumPreference<Fit> =
        EnumPreferenceDelegate(
            getValue = { preferences.fit },
            getEffectiveValue = { settings.fit },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(fit = value) } },
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH),
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceDelegate(
            getValue = { preferences.readingProgression },
            getEffectiveValue = { settings.readingProgression },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(readingProgression = value) } },
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceDelegate(
            getValue = { preferences.scrollAxis},
            getEffectiveValue = { settings.scrollAxis },
            getIsEffective = { true },
            updateValue = { value -> updateValues { it.copy(scrollAxis = value) } },
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
        )

    private fun updateValues(updater: (PdfiumPreferences) -> PdfiumPreferences) {
        preferences = updater(preferences)
        settings = settingsResolver.settings(preferences)
    }
}
