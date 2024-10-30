/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import org.readium.r2.navigator.preferences.PreferencesFilter

/**
 * Suggested filter to keep only shared [PdfiumPreferences].
 */
public object PdfiumSharedPreferencesFilter : PreferencesFilter<PdfiumPreferences> {

    override fun filter(preferences: PdfiumPreferences): PdfiumPreferences =
        preferences.copy(
            readingProgression = null
        )
}

/**
 * Suggested filter to keep only publication-specific [PdfiumPreferences].
 */
public object PdfiumPublicationPreferencesFilter : PreferencesFilter<PdfiumPreferences> {

    override fun filter(preferences: PdfiumPreferences): PdfiumPreferences =
        PdfiumPreferences(
            readingProgression = preferences.readingProgression
        )
}
