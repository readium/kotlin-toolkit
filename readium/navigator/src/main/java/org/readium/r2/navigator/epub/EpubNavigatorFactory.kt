/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(org.readium.r2.shared.InternalReadiumApi::class)

package org.readium.r2.navigator.epub

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
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
class EpubNavigatorFactory(
    private val publication: Publication,
    private val configuration: Configuration = Configuration()
) {

    /**
     * Configuration for the [EpubNavigatorFactory].
     *
     * @param defaults navigator fallbacks for some preferences
     */
    data class Configuration(
        val defaults: EpubDefaults = EpubDefaults(),
    )

    private val layout: EpubLayout =
        publication.metadata.presentation.layout ?: EpubLayout.REFLOWABLE

    fun createFragmentFactory(
        initialLocator: Locator?,
        readingOrder: List<Link>? = null,
        initialPreferences: EpubPreferences = EpubPreferences(),
        listener: EpubNavigatorFragment.Listener? = null,
        paginationListener: EpubNavigatorFragment.PaginationListener? = null,
        configuration: EpubNavigatorFragment.Configuration = EpubNavigatorFragment.Configuration(),
    ) = org.readium.r2.navigator.util.createFragmentFactory {
        EpubNavigatorFragment(
            publication = publication,
            baseUrl = null,
            initialLocator = initialLocator,
            readingOrder = readingOrder,
            initialPreferences = initialPreferences,
            listener = listener,
            paginationListener = paginationListener,
            epubLayout = layout,
            defaults = this.configuration.defaults,
            configuration = configuration
        )
    }

    fun createPreferencesEditor(
        currentPreferences: EpubPreferences,
    ): EpubPreferencesEditor =
        EpubPreferencesEditor(
            initialPreferences = currentPreferences,
            publicationMetadata = publication.metadata,
            layout = layout,
            defaults = configuration.defaults
        )
}
