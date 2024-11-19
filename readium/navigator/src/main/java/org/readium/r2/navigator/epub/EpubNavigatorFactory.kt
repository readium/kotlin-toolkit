/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator.epub

import androidx.fragment.app.FragmentFactory
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
@OptIn(ExperimentalReadiumApi::class)
public class EpubNavigatorFactory(
    private val publication: Publication,
    private val configuration: Configuration = Configuration(),
) {

    /**
     * Configuration for the [EpubNavigatorFactory].
     *
     * @param defaults navigator fallbacks for some preferences
     */
    public data class Configuration(
        val defaults: EpubDefaults = EpubDefaults(),
    )

    private val layout: EpubLayout =
        publication.metadata.presentation.layout ?: EpubLayout.REFLOWABLE

    /**
     * Creates a factory for [EpubNavigatorFragment].
     *
     * @param initialLocator The first location which should be visible when rendering the
     * publication. Can be used to restore the last reading location.
     * @param readingOrder Custom reading order to override the publication's one.
     * @param initialPreferences The set of preferences that should be initially applied to the
     * navigator.
     * @param listener Optional listener to implement to observe navigator events.
     * @param paginationListener Optional listener to implement to observe events related to
     * pagination.
     * @param configuration Additional configuration.
     */
    public fun createFragmentFactory(
        initialLocator: Locator?,
        readingOrder: List<Link>? = null,
        initialPreferences: EpubPreferences = EpubPreferences(),
        listener: EpubNavigatorFragment.Listener? = null,
        paginationListener: EpubNavigatorFragment.PaginationListener? = null,
        configuration: EpubNavigatorFragment.Configuration = EpubNavigatorFragment.Configuration(),
    ): FragmentFactory = org.readium.r2.navigator.util.createFragmentFactory {
        EpubNavigatorFragment(
            publication = publication,
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

    public fun createPreferencesEditor(
        currentPreferences: EpubPreferences,
    ): EpubPreferencesEditor =
        EpubPreferencesEditor(
            initialPreferences = currentPreferences,
            publicationMetadata = publication.metadata,
            layout = layout,
            defaults = configuration.defaults
        )
}
