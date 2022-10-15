/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.NavigatorFactory
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
        val editorConfiguration: EpubPreferencesEditor.Configuration = EpubPreferencesEditor.Configuration(),
        val fontFamilyDeclarations: List<FontFamilyDeclaration> = DEFAULT_FONT_DECLARATIONS
    )

    private val epubLayout: EpubLayout =
        publication.metadata.presentation.layout ?: EpubLayout.REFLOWABLE

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
                fontFamilyDeclarations = this.configuration.fontFamilyDeclarations,
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
            configuration = configuration.editorConfiguration
        )

    companion object {

        val DEFAULT_FONT_DECLARATIONS: List<FontFamilyDeclaration> = listOf(
            FontFamily.LITERATA.fromGoogleFonts,
            FontFamily.PT_SERIF.fromGoogleFonts,
            FontFamily.VOLLKORN.fromGoogleFonts,
            FontFamily.ROBOTO.fromGoogleFonts,
            FontFamily.SOURCE_SANS_PRO.fromGoogleFonts,
            FontFamily.OPEN_DYSLEXIC.fromAsset("readium/fonts/OpenDyslexic-Regular.otf")
        )
    }
}
