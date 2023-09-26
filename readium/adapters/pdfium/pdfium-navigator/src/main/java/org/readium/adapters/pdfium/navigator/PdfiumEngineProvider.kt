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
import org.readium.r2.navigator.util.SingleFragmentFactory
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

/**
 * Main component to use the PDF navigator with the PDFium adapter.
 *
 * Provide [PdfiumDefaults] to customize the default values that will be used by
 * the navigator for some preferences.
 */
@ExperimentalReadiumApi
public class PdfiumEngineProvider(
    private val defaults: PdfiumDefaults = PdfiumDefaults()
) : PdfEngineProvider<PdfiumDocumentFragment, PdfiumDocumentFragment.Listener, PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor> {

    override fun createDocumentFragmentFactory(
        input: PdfDocumentFragmentInput<PdfiumDocumentFragment.Listener, PdfiumSettings>
    ): SingleFragmentFactory<PdfiumDocumentFragment> =
        createFragmentFactory {
            PdfiumDocumentFragment(
                publication = input.publication,
                href = input.href,
                initialPageIndex = input.pageIndex,
                initialSettings = input.settings,
                listener = input.listener,
                inputListener = input.inputListener
            )
        }

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
        initialPreferences: PdfiumPreferences
    ): PdfiumPreferencesEditor =
        PdfiumPreferencesEditor(
            initialPreferences,
            publication.metadata,
            defaults
        )

    override fun createEmptyPreferences(): PdfiumPreferences =
        PdfiumPreferences()
}
