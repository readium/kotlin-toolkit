/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.FontFamilyDeclaration
import org.readium.r2.navigator.NavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation

@ExperimentalReadiumApi
class EpubNavigatorFactory(
    private val publication: Publication,
    private val configuration: Configuration
) : NavigatorFactory<EpubSettings, EpubPreferences, EpubPreferencesEditor> {

    data class Configuration(
        val defaults: EpubDefaults = EpubDefaults(),
        val preferencesEditorConfiguration: EpubPreferencesEditor.Configuration = EpubPreferencesEditor.Configuration(),
        val fontDeclarations:  List<FontFamilyDeclaration> = emptyList(),
        val ignoreDefaultFontDeclarations: Boolean = false,
    )

    private val epubLayout: EpubLayout =
        publication.metadata.presentation.layout ?: EpubLayout.REFLOWABLE

    private val fontDeclarations: List<FontFamilyDeclaration> =
        EpubNavigatorFragment.DEFAULT_FONT_DECLARATIONS
            .takeUnless { configuration.ignoreDefaultFontDeclarations }
            .orEmpty()
            .plus(configuration.fontDeclarations)

    fun createFragmentFactory(
        initialLocator: Locator?,
        initialPreferences: EpubPreferences = EpubPreferences(),
        listener: EpubNavigatorFragment.Listener? = null,
        paginationListener: EpubNavigatorFragment.PaginationListener? = null,
        configuration: EpubNavigatorFragment.Configuration = EpubNavigatorFragment.Configuration(),
    ) = org.readium.r2.navigator.util.createFragmentFactory {
            EpubNavigatorFragment(
                publication = publication,
                baseUrl = null,
                initialLocator = initialLocator,
                initialPreferences = initialPreferences,
                listener = listener,
                paginationListener = paginationListener,
                epubLayout = epubLayout,
                fontFamilyDeclarations = fontDeclarations,
                defaults = this.configuration.defaults,
                configuration = configuration
            )
        }

    override fun createPreferencesEditor(
        currentSettings: EpubSettings,
        currentPreferences: EpubPreferences,
    ): EpubPreferencesEditor = EpubPreferencesEditor(
            currentSettings = currentSettings,
            initialPreferences = currentPreferences,
            publicationMetadata = publication.metadata,
            epubLayout = epubLayout,
            defaults = configuration.defaults,
            configuration = configuration.preferencesEditorConfiguration
        )
}
