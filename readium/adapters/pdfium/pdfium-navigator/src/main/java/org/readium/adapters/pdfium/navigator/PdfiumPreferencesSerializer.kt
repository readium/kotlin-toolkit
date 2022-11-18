/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator

import kotlinx.serialization.json.Json
import org.readium.r2.navigator.preferences.PreferencesSerializer
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * JSON serializer of [PdfiumPreferences].
 */
@ExperimentalReadiumApi
class PdfiumPreferencesSerializer : PreferencesSerializer<PdfiumPreferences> {

    override fun serialize(preferences: PdfiumPreferences): String =
        Json.encodeToString(PdfiumPreferences.serializer(), preferences)

    override fun deserialize(preferences: String): PdfiumPreferences =
        Json.decodeFromString(PdfiumPreferences.serializer(), preferences)
}
