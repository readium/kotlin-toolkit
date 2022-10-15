/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import org.readium.r2.navigator.preferences.NavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
class PsPdfKitNavigatorFactory(
    private val publication: Publication,
    private val configuration: Configuration
) : NavigatorFactory<PsPdfKitSettings, PsPdfKitPreferences, PsPdfKitPreferencesEditor> {

    data class Configuration(
        val navigatorDefaults: PsPdfKitDefaults = PsPdfKitDefaults(),
        val editorConfiguration: PsPdfKitPreferencesEditor.Configuration = PsPdfKitPreferencesEditor.Configuration()
    )

    override fun createPreferencesEditor(
        currentSettings: PsPdfKitSettings,
        currentPreferences: PsPdfKitPreferences,
    ): PsPdfKitPreferencesEditor = PsPdfKitPreferencesEditor(
        currentSettings = currentSettings,
        initialPreferences = currentPreferences,
        publicationMetadata = publication.metadata,
        configuration = configuration.editorConfiguration,
        defaults = configuration.navigatorDefaults
    )
}
