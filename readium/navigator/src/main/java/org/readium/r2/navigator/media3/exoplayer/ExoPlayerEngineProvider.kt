/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import org.readium.r2.navigator.media3.player.ExoPlayerDataSource
import org.readium.r2.navigator.media3.player.MediaEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoPlayerEngineProvider(
    private val context: Context,
) : MediaEngineProvider<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerPreferencesEditor> {

    override suspend fun createPlayer(publication: Publication): ExoPlayer {
        val dataSourceFactory: DataSource.Factory = ExoPlayerDataSource.Factory(publication)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
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
