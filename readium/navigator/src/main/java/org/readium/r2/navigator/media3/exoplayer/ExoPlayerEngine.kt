/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.exoplayer

import android.app.Application
import androidx.media3.common.*
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.audio.AudioBookNavigator
import org.readium.r2.navigator.media3.audio.AudioEngine
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * An [AudioEngine] based on Media3 ExoPlayer.
 */
@ExperimentalReadiumApi
@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExoPlayerEngine(
    private val exoPlayer: ExoPlayer,
    private val settingsResolver: SettingsResolver,
    private val positionRefreshRate: Double,
    initialPreferences: ExoPlayerPreferences
) : AudioEngine<ExoPlayerSettings, ExoPlayerPreferences, ExoPlayerEngine.Error> {

    companion object {

        suspend operator fun invoke(
            application: Application,
            settingsResolver: SettingsResolver,
            dataSourceFactory: DataSource.Factory,
            playlist: Playlist,
            positionRefreshRate: Double,
            initialIndex: Int,
            initialPosition: Duration,
            initialPreferences: ExoPlayerPreferences
        ): ExoPlayerEngine {
            val exoPlayer = ExoPlayer.Builder(application)
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

            exoPlayer.setMediaItems(
                playlist.items.map { item ->
                    MediaItem.Builder()
                        .setUri(item.uri)
                        .setMediaMetadata(item.mediaMetadata)
                        .build()
                }
            )

            exoPlayer.playlistMetadata = playlist.mediaMetadata

            exoPlayer.seekTo(initialIndex, initialPosition.inWholeMilliseconds)

            prepareExoPlayer(exoPlayer)

            return ExoPlayerEngine(
                exoPlayer,
                settingsResolver,
                positionRefreshRate,
                initialPreferences
            )
        }

        private suspend fun prepareExoPlayer(player: ExoPlayer) {
            lateinit var listener: Player.Listener
            suspendCancellableCoroutine { continuation ->
                listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> continuation.resume(Unit) {}
                            Player.STATE_IDLE -> if (player.playerError != null) {
                                continuation.resume(Unit) {}
                            }
                            else -> {}
                        }
                    }
                }
                continuation.invokeOnCancellation { player.removeListener(listener) }
                player.addListener(listener)
                player.prepare()
            }
            player.removeListener(listener)
        }
    }

    data class Playlist(
        val mediaMetadata: MediaMetadata,
        val duration: Duration?,
        val items: List<Item>
    ) {
        data class Item(
            val uri: String,
            val mediaMetadata: MediaMetadata,
            val duration: Duration?
        )
    }

    fun interface SettingsResolver {

        /**
         * Computes a set of engine settings from the engine preferences.
         */
        fun settings(preferences: ExoPlayerPreferences): ExoPlayerSettings
    }

    private inner class Listener : Player.Listener {

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            submitPreferences(
                ExoPlayerPreferences(
                    pitch = playbackParameters.pitch.toDouble(),
                    speed = playbackParameters.speed.toDouble()
                )
            )
        }

        override fun onEvents(player: Player, events: Player.Events) {
            _playback.value = exoPlayer.playback
            _position.value = exoPlayer.position
        }
    }

    class Error : AudioEngine.Error

    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        exoPlayer.addListener(Listener())
    }

    private val _settings: MutableStateFlow<ExoPlayerSettings> =
        MutableStateFlow(settingsResolver.settings(initialPreferences))

    private val _playback: MutableStateFlow<AudioEngine.Playback<Error>> =
        MutableStateFlow(exoPlayer.playback)

    private val _position: MutableStateFlow<AudioEngine.Position> =
        MutableStateFlow(exoPlayer.position)

    init {
        coroutineScope.launch {
            val positionRefreshDelay = (1.0 / positionRefreshRate).seconds
            while (isActive) {
                delay(positionRefreshDelay)
                _position.value = exoPlayer.position
            }
        }

        submitPreferences(initialPreferences)
    }

    override val playback: StateFlow<AudioEngine.Playback<Error>>
        get() = _playback.asStateFlow()

    override val position: StateFlow<AudioEngine.Position>
        get() = _position.asStateFlow()

    override val settings: StateFlow<ExoPlayerSettings>
        get() = _settings.asStateFlow()

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun seek(index: Int, position: Duration) {
        exoPlayer.seekTo(index, position.inWholeMilliseconds)
    }

    override fun close() {
        coroutineScope.cancel()
        exoPlayer.release()
    }

    override fun asPlayer(): Player {
        return exoPlayer
    }

    override fun submitPreferences(preferences: ExoPlayerPreferences) {
        val newSettings = settingsResolver.settings(preferences)
        exoPlayer.playbackParameters = PlaybackParameters(
            newSettings.speed.toFloat(), newSettings.pitch.toFloat()
        )
    }

    private val ExoPlayer.playback: AudioEngine.Playback<Error> get() =
        AudioEngine.Playback(
            state = engineState,
            playWhenReady = playWhenReady
        )

    private val ExoPlayer.engineState: MediaNavigator.State get() =
        when (this.playbackState) {
            Player.STATE_READY -> AudioBookNavigator.State.Ready
            Player.STATE_BUFFERING -> AudioBookNavigator.State.Buffering
            Player.STATE_ENDED -> AudioBookNavigator.State.Ended
            else -> AudioBookNavigator.State.Error()
        }

    private val ExoPlayer.position: AudioEngine.Position get() =
        AudioEngine.Position(
            index = currentMediaItemIndex,
            position = currentPosition.milliseconds,
            duration = duration.takeIf { it != C.TIME_UNSET }?.milliseconds,
            buffered = bufferedPosition.milliseconds
        )
}
