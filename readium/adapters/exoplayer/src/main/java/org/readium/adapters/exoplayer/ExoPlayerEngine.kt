/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.exoplayer

import android.app.Application
import androidx.media3.common.*
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.navigators.media.audio.AudioEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.toUri
import org.readium.r2.shared.util.units.Hz
import org.readium.r2.shared.util.units.hz

/**
 * An [AudioEngine] based on Media3 ExoPlayer.
 */
@ExperimentalReadiumApi
@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
public class ExoPlayerEngine private constructor(
    private val exoPlayer: ExoAudiobookPlayer,
    private val settingsResolver: SettingsResolver,
    private val configuration: Configuration,
    initialPreferences: ExoPlayerPreferences
) : AudioEngine<ExoPlayerSettings, ExoPlayerPreferences> {

    public companion object {

        public suspend operator fun invoke(
            application: Application,
            settingsResolver: SettingsResolver,
            dataSourceFactory: DataSource.Factory,
            playlist: Playlist,
            configuration: Configuration,
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
                .setSeekBackIncrementMs(configuration.seekBackwardIncrement.inWholeMilliseconds)
                .setSeekForwardIncrementMs(configuration.seekForwardIncrement.inWholeMilliseconds)
                .build()

            exoPlayer.setMediaItems(
                playlist.items.map { item ->
                    MediaItem.Builder()
                        .setUri(item.url.toUri())
                        .setMediaMetadata(item.mediaMetadata)
                        .build()
                }
            )

            val durations: List<Duration>? =
                playlist.items.mapNotNull { it.duration }
                    .takeIf { it.size == playlist.items.size }

            exoPlayer.playlistMetadata = playlist.mediaMetadata

            exoPlayer.seekTo(initialIndex, initialPosition.inWholeMilliseconds)

            prepareExoPlayer(exoPlayer)

            val customizedPlayer =
                ExoAudiobookPlayer(
                    exoPlayer,
                    durations,
                    configuration.seekForwardIncrement,
                    configuration.seekBackwardIncrement
                )

            return ExoPlayerEngine(
                customizedPlayer,
                settingsResolver,
                configuration,
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

    public data class Configuration(
        val positionRefreshRate: Hz = 2.0.hz,
        val seekBackwardIncrement: Duration = 15.seconds,
        val seekForwardIncrement: Duration = 30.seconds
    )

    public data class Playlist(
        val mediaMetadata: MediaMetadata,
        val duration: Duration?,
        val items: List<Item>
    ) {
        public data class Item(
            val url: Url,
            val mediaMetadata: MediaMetadata,
            val duration: Duration?
        )
    }

    public fun interface SettingsResolver {

        /**
         * Computes a set of engine settings from the engine preferences.
         */
        public fun settings(preferences: ExoPlayerPreferences): ExoPlayerSettings
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
        }
    }

    public data class Error(val error: ExoPlaybackException) : AudioEngine.Error

    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        exoPlayer.addListener(Listener())
    }

    private val _settings: MutableStateFlow<ExoPlayerSettings> =
        MutableStateFlow(settingsResolver.settings(initialPreferences))

    private val _playback: MutableStateFlow<AudioEngine.Playback> =
        MutableStateFlow(exoPlayer.playback)

    init {
        coroutineScope.launch {
            val positionRefreshDelay = (1.0 / configuration.positionRefreshRate.value).seconds
            while (isActive) {
                delay(positionRefreshDelay)
                _playback.value = exoPlayer.playback
            }
        }

        submitPreferences(initialPreferences)
    }

    override val playback: StateFlow<AudioEngine.Playback>
        get() = _playback.asStateFlow()

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

    override fun seekBy(offset: Duration) {
        exoPlayer.seekBy(offset)
    }

    override fun seekForward() {
        exoPlayer.seekForward()
    }

    override fun seekBackward() {
        exoPlayer.seekBack()
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
            newSettings.speed.toFloat(),
            newSettings.pitch.toFloat()
        )
    }

    private val ExoAudiobookPlayer.playback: AudioEngine.Playback get() =
        AudioEngine.Playback(
            state = engineState,
            playWhenReady = playWhenReady,
            index = currentMediaItemIndex,
            offset = currentPosition.milliseconds,
            buffered = bufferedPosition.milliseconds
        )

    private val ExoAudiobookPlayer.engineState: AudioEngine.State get() =
        when (this.playbackState) {
            Player.STATE_READY -> AudioEngine.State.Ready
            Player.STATE_BUFFERING -> AudioEngine.State.Buffering
            Player.STATE_ENDED -> AudioEngine.State.Ended
            else -> AudioEngine.State.Error(Error(playerError!!))
        }
}
