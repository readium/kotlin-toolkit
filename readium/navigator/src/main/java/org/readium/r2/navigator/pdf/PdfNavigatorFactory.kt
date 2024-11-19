/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator.pdf

import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Factory of the PDF navigator and related components.
 *
 * @param publication PDF publication to render in the navigator.
 * @param pdfEngineProvider provider for third-party PDF engine adapter.
 */
@ExperimentalReadiumApi
public class PdfNavigatorFactory<S : Configurable.Settings, P : Configurable.Preferences<P>, E : PreferencesEditor<P>>(
    private val publication: Publication,
    private val pdfEngineProvider: PdfEngineProvider<S, P, E>,
) {

    /**
     * Creates a factory for [PdfNavigatorFragment].
     *
     * @param initialLocator The first location which should be visible when rendering the
     * publication. Can be used to restore the last reading location.
     * @param initialPreferences Initial set of user preferences.
     * @param listener Optional listener to implement to observe events, such as user taps.
     */
    @ExperimentalReadiumApi
    public fun createFragmentFactory(
        initialLocator: Locator? = null,
        initialPreferences: P? = null,
        listener: PdfNavigatorFragment.Listener? = null,
    ): FragmentFactory = createFragmentFactory {
        PdfNavigatorFragment(
            publication = publication,
            initialLocator = initialLocator,
            initialPreferences = initialPreferences ?: pdfEngineProvider.createEmptyPreferences(),
            listener = listener,
            pdfEngineProvider = pdfEngineProvider
        )
    }

    /**
     * Creates a preferences editor for [publication] with [initialPreferences].
     *
     * @param initialPreferences Initial set of preferences for the editor.
     */
    public fun createPreferencesEditor(
        initialPreferences: P,
    ): E =
        pdfEngineProvider.createPreferenceEditor(
            publication,
            initialPreferences
        )
}
