/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import android.content.Context
import org.readium.adapters.pspdfkit.document.PsPdfKitDocumentFactory
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.pdf.PdfDocumentFragmentInput
import org.readium.r2.navigator.pdf.PdfEngineProvider
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.pdf.cachedIn

@ExperimentalReadiumApi
class PsPdfKitEngineProvider(
    private val context: Context,
    private val settingsPolicy : PsPdfKitSettingsPolicy = PsPdfKitSettingsPolicy()
) : PdfEngineProvider<PsPdfKitSettings> {

    override suspend fun createDocumentFragment(
        input: PdfDocumentFragmentInput<PsPdfKitSettings>
    ): PdfDocumentFragment<PsPdfKitSettings> {

        val publication = input.publication
        val document = PsPdfKitDocumentFactory(context)
            .cachedIn(publication)
            .open(publication.get(input.link), null)

        return PsPdfKitDocumentFragment(
            publication = publication,
            document = document,
            initialPageIndex = input.initialPageIndex,
            settings = input.settings,
            listener = input.listener
        )
    }

    override fun createSettings(metadata: Metadata, preferences: Preferences): PsPdfKitSettings {
        val policy = settingsPolicy ?:  PsPdfKitSettingsPolicy()
        return PsPdfKitSettingsFactory(metadata, policy).createSettings(preferences)
    }
}
