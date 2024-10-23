/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pdf

import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.r2.navigator.pdf.PdfEngineProvider
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try

@ExperimentalReadiumApi
public class PdfNavigatorFactory<S : Settings, P : Preferences<P>, E : PreferencesEditor<P>>(
    private val publication: Publication,
    private val pdfEngineProvider: PdfEngineProvider<S, P, E>
) {

    public fun createNavigator(
        initialLocator: Locator? = null,
        initialPreferences: P? = null
    ): Try<PdfNavigatorState<S, P>, Nothing> {
        val readingOrder =
            PdfReadingOrder(
                publication.readingOrder.map { PdfReadingOrderItem(it.url()) }
            )

        val actualInitialLocator = initialLocator
            ?: publication.locatorFromLink(publication.readingOrder[0])!!

        val actualInitialPreferences = initialPreferences
            ?: pdfEngineProvider.createEmptyPreferences()

        val legacyNavigatorFactory =
            PdfNavigatorFactory(publication, pdfEngineProvider)

        val settingsResolver = { preferences: P ->
            pdfEngineProvider.computeSettings(publication.metadata, preferences)
        }

        val navigatorState =
            PdfNavigatorState(
                readingOrder,
                legacyNavigatorFactory,
                settingsResolver,
                actualInitialLocator,
                actualInitialPreferences
            )

        return Try.success(navigatorState)
    }

    public fun createPreferencesEditor(
        currentPreferences: P
    ): E =
        pdfEngineProvider.createPreferenceEditor(publication, currentPreferences)

    public fun createLocatorAdapter(): PdfLocatorAdapter =
        PdfLocatorAdapter(publication)
}
