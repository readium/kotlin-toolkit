/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation

/**
 * Factory of the EPUB navigator and related components.
 *
 * @param publication EPUB publication to render in the navigator.
 * @param configuration Configuration of the factory to create.
 */
@ExperimentalReadiumApi
public class EpubNavigatorFactory(
    private val publication: Publication,
    private val configuration: Configuration = Configuration()
) {

    /**
     * Configuration for the [EpubNavigatorFactory].
     *
     * @param defaults navigator fallbacks for some preferences
     */
    public data class Configuration(
        val defaults: EpubDefaults = EpubDefaults()
    )

    private val layout: EpubLayout =
        publication.metadata.presentation.layout ?: EpubLayout.REFLOWABLE

    @OptIn(ExperimentalDecorator::class)
    public fun createFragmentFactory(
        initialLocator: Locator?,
        initialPreferences: EpubPreferences = EpubPreferences(),
        listener: EpubNavigatorFragment.Listener? = null,
        paginationListener: EpubNavigatorFragment.PaginationListener? = null,
        configuration: EpubNavigatorFragment.Configuration = EpubNavigatorFragment.Configuration()
    ): FragmentFactory = org.readium.r2.navigator.util.createFragmentFactory {
        EpubNavigatorFragment(
            publication = publication,
            initialLocator = initialLocator,
            initialPreferences = initialPreferences,
            listener = listener,
            paginationListener = paginationListener,
            epubLayout = layout,
            defaults = this.configuration.defaults,
            configuration = configuration
        )
    }

    public fun createPreferencesEditor(
        currentPreferences: EpubPreferences
    ): EpubPreferencesEditor =
        EpubPreferencesEditor(
            initialPreferences = currentPreferences,
            publicationMetadata = publication.metadata,
            layout = layout,
            defaults = configuration.defaults
        )
}
