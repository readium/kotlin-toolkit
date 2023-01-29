/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import org.readium.r2.navigator.media3.audio.AudioEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoPlayerEngineProvider() : AudioEngineProvider<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerPreferencesEditor, ExoPlayerEngine.Error> {

    override suspend fun createEngine(publication: Publication): ExoPlayerEngine {
        TODO("Not yet implemented")
    }

    override fun computeSettings(
        metadata: Metadata,
        preferences: ExoPlayerPreferences
    ): ExoPlayerSettings =
        ExoPlayerSettingsResolver(metadata).settings(preferences)

    override fun createPreferenceEditor(
        publication: Publication,
        initialPreferences: ExoPlayerPreferences
    ): ExoPlayerPreferencesEditor =
        ExoPlayerPreferencesEditor()

    override fun createEmptyPreferences(): ExoPlayerPreferences =
        ExoPlayerPreferences()
}
