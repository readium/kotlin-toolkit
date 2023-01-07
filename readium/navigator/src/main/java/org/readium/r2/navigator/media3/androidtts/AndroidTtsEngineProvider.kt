/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import android.content.Context
import org.readium.r2.navigator.media3.tts2.TtsEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
class AndroidTtsEngineProvider(
    private val context: Context,
) : TtsEngineProvider<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsPreferencesEditor> {

    override suspend fun createEngine(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences
    ): AndroidTtsEngine? {
        return AndroidTtsEngine(context, publication.metadata, initialPreferences)
    }

    fun computeSettings(
        metadata: Metadata,
        preferences: AndroidTtsPreferences
    ): AndroidTtsSettings =
        AndroidTtsSettingsResolver(metadata).settings(preferences)

    override fun createPreferencesEditor(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences
    ): AndroidTtsPreferencesEditor =
        AndroidTtsPreferencesEditor(initialPreferences, publication.metadata)

    override fun createEmptyPreferences(): AndroidTtsPreferences =
        AndroidTtsPreferences()
}
