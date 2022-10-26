/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.NavigatorFactory
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * @param publication PDF publication to render in the navigator.
 */
@ExperimentalReadiumApi
class PdfNavigatorFactory<S: Configurable.Settings, P: Configurable.Preferences, E: PreferencesEditor<P>>(
    private val publication: Publication,
    private val pdfEngineProvider: PdfEngineProvider<S, P, E>
) : NavigatorFactory<S, P, E> {

    /**
     * Creates a factory for [PdfNavigatorFragment].
     *
     * @param initialLocator The first location which should be visible when rendering the
     * publication. Can be used to restore the last reading location.
     * @param preferences Initial set of user preferences.
     * @param listener Optional listener to implement to observe events, such as user taps.
     * @param pdfEngineProvider provider for third-party PDF engine adapter.
     */
    @ExperimentalReadiumApi
    fun createFragmentFactory(
        initialLocator: Locator? = null,
        initialPreferences: P? = null,
        listener: PdfNavigatorFragment.Listener? = null,
    ): FragmentFactory = createFragmentFactory<PdfNavigatorFragment<S, P, E>> {
        PdfNavigatorFragment(
            publication = publication,
            initialLocator = initialLocator,
            initialPreferences = initialPreferences ?: pdfEngineProvider.createEmptyPreferences(),
            listener = listener,
            pdfEngineProvider = pdfEngineProvider
        )
    }

    override fun createPreferencesEditor(
        currentSettings: S,
        currentPreferences: P
    ): E =
       pdfEngineProvider.createPreferenceEditor(
           publication,
           currentSettings,
           currentPreferences
       )
}
