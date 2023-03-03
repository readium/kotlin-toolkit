/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import android.app.Application
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.audio.AudioEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoPlayerEngine(
    private val application: Application,
    private val publication: Publication,
    private val exoPlayer: ExoPlayer,
) : AudioEngine<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerEngine.Error> {

    companion object {

        private fun createExoPlayer(
            application: Application,
            publication: Publication,
        ): ExoPlayer {
            val dataSourceFactory: DataSource.Factory = ExoPlayerDataSource.Factory(publication)
            return ExoPlayer.Builder(application)
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
    }

    class Error : AudioEngine.Error

    override val playback: StateFlow<AudioEngine.Playback<Error>>
        get() = TODO("Not yet implemented")

    override val position: StateFlow<AudioEngine.Position>
        get() = TODO("Not yet implemented")

    override val settings: StateFlow<ExoPlayerSettings>
        get() = TODO("Not yet implemented")

    override fun play() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun seek(index: Long, position: Duration) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun asPlayer(): Player {
        return exoPlayer
    }

    override fun submitPreferences(preferences: ExoPlayerPreferences) {
        TODO("Not yet implemented")
    }
}
