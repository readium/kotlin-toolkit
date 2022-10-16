/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@OptIn(ExperimentalReadiumApi::class)
class PdfiumPreferencesEditor(
    private val currentSettings: PdfiumSettings,
    private val initialPreferences: PdfiumPreferences,
    private val publicationMetadata: Metadata
) : PreferencesEditor<PdfiumPreferences> {

    override val preferences: PdfiumPreferences
        get() = TODO("Not yet implemented")

    override fun clear() {
        TODO("Not yet implemented")
    }


}
