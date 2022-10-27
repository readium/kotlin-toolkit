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

@OptIn(ExperimentalReadiumApi::class)
class PdfiumPreferencesEditor(
    currentSettings: PdfiumSettings,
    initialPreferences: PdfiumPreferences,
    publicationMetadata: Metadata,
    defaults: PdfiumDefaults,
) : PreferencesEditor<PdfiumPreferences> {

    override val preferences: PdfiumPreferences
        get() = PdfiumPreferences(
            readingProgression = readingProgression.value,
            scrollAxis = scrollAxis.value,
            fit = fit.value
        )

    override fun clear() {
        readingProgression.value = null
        scrollAxis.value = null
        fit.value = null
    }

    val fit: EnumPreference<Fit> =
        EnumPreferenceImpl(
            value = initialPreferences.fit,
            effectiveValue = currentSettings.fit,
            supportedValues = listOf(Fit.CONTAIN, Fit.WIDTH),
        )

    val readingProgression: EnumPreference<ReadingProgression> =
        EnumPreferenceImpl(
            value = initialPreferences.readingProgression,
            effectiveValue = currentSettings.readingProgression,
            supportedValues = listOf(ReadingProgression.LTR, ReadingProgression.RTL),
        )

    val scrollAxis: EnumPreference<Axis> =
        EnumPreferenceImpl(
            value = initialPreferences.scrollAxis,
            effectiveValue = currentSettings.scrollAxis,
            supportedValues = listOf(Axis.VERTICAL, Axis.HORIZONTAL),
        )
}
