/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.SimplePresentation
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.pdf.PdfDocumentFragmentInput
import org.readium.r2.navigator.pdf.PdfEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
class PdfiumEngineProvider(
    private val listener: PdfiumDocumentFragment.Listener? = null,
    private val defaults: PdfiumDefaults = PdfiumDefaults()
) : PdfEngineProvider<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor> {

    override suspend fun createDocumentFragment(input: PdfDocumentFragmentInput<PdfiumSettings>) =
        PdfiumDocumentFragment(
            publication = input.publication,
            link = input.link,
            initialPageIndex = input.initialPageIndex,
            settings = input.settings,
            appListener = listener,
            navigatorListener = input.listener
        )

    override fun computeSettings(metadata: Metadata, preferences: PdfiumPreferences): PdfiumSettings {
        val settingsPolicy = PdfiumSettingsResolver(metadata, defaults)
        return settingsPolicy.settings(preferences)
    }

    override fun computePresentation(settings: PdfiumSettings): VisualNavigator.Presentation =
        SimplePresentation(
            readingProgression = settings.readingProgression,
            scroll = true,
            axis = settings.scrollAxis
        )

    override fun createPreferenceEditor(
        publication: Publication,
        currentSettings: PdfiumSettings,
        currentPreferences: PdfiumPreferences
    ): PdfiumPreferencesEditor =
        PdfiumPreferencesEditor(
            currentSettings,
            currentPreferences,
            publication.metadata,
            defaults
        )

    override fun createEmptyPreferences(): PdfiumPreferences =
        PdfiumPreferences()
}
