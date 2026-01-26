/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.readaloud

import android.app.Application
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlin.properties.Delegates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.readium.navigator.media.readaloud.AudioChunk
import org.readium.navigator.media.readaloud.AudioEngineProgress
import org.readium.navigator.media.readaloud.PlaybackEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.findInstance
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadException

/**
 * A [PlaybackEngine] based on Media3 ExoPlayer.
 */
@ExperimentalReadiumApi
@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
public class ExoPlayerEngine private constructor(
    private val exoPlayer: ExoPlayer,
    private val listener: PlaybackEngine.Listener<AudioEngineProgress, Error>,
) : PlaybackEngine {

    public companion object {

        public operator fun invoke(
            application: Application,
            dataSourceFactory: DataSource.Factory,
            chunks: List<AudioChunk>,
            listener: PlaybackEngine.Listener<AudioEngineProgress, Error>,
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

            exoPlayer.preloadConfiguration = ExoPlayer.PreloadConfiguration(10_000_000L)
            exoPlayer.pauseAtEndOfMediaItems = true

            val mediaItems = chunks.map { item ->
                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .apply {
                        item.interval?.start?.let { setStartPositionMs(it.inWholeMilliseconds) }
                        item.interval?.end?.let { setEndPositionMs(it.inWholeMilliseconds) }
                    }.build()
                MediaItem.Builder()
                    .setUri(item.href.toString())
                    .setClippingConfiguration(clippingConfig)
                    .build()
            }
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()

            return ExoPlayerEngine(exoPlayer, listener)
        }
    }

    private inner class Listener : Player.Listener {

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                state = State.Ended
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            state = when (val stateNow = state) {
                State.Idle -> {
                    stateNow
                }
                State.Ended -> {
                    stateNow
                }
                is State.Running -> {
                    when (playbackState) {
                        Player.STATE_READY ->
                            stateNow.copy(playbackState = PlaybackEngine.PlaybackState.Playing)
                        Player.STATE_BUFFERING ->
                            stateNow.copy(playbackState = PlaybackEngine.PlaybackState.Starved)
                        Player.STATE_ENDED ->
                            State.Idle
                        else -> stateNow
                    }
                }
            }
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?,
    ) : org.readium.r2.shared.util.Error {

        public data class Engine(override val cause: ThrowableError<ExoPlaybackException>) :
            Error("An error occurred in the ExoPlayer engine.", cause)

        public data class Source(override val cause: ReadError) :
            Error("An error occurred while trying to read publication content.", cause)
    }

    private sealed interface State {

        object Idle : State

        object Ended : State

        data class Running(
            val playbackState: PlaybackEngine.PlaybackState,
            val paused: Boolean,
        ) : State
    }

    override var pitch: Double
        get() = exoPlayer.playbackParameters.pitch.toDouble()
        set(value) {
            exoPlayer.playbackParameters =
                exoPlayer.playbackParameters.withPitch(value.toFloat())
        }

    override var speed: Double
        get() = exoPlayer.playbackParameters.speed.toDouble()
        set(value) {
            exoPlayer.playbackParameters =
                exoPlayer.playbackParameters.withSpeed(value.toFloat())
        }

    override var itemToPlay: Int by Delegates.observable(0) { property, oldValue, newValue ->
        when (state) {
            State.Ended -> {
                if (newValue != exoPlayer.currentMediaItemIndex + 1) {
                    exoPlayer.seekTo(newValue, 0)
                }
            }
            State.Idle -> {
                if (newValue != exoPlayer.currentMediaItemIndex) {
                    exoPlayer.seekTo(newValue, 0)
                }
            }
            is State.Running -> {
            }
        }
    }

    private var state: State by Delegates.observable(State.Idle) { property, oldValue, newValue ->
        if (newValue != oldValue) {
            when {
                newValue is State.Ended -> {
                    listener.onPlaybackCompleted()
                }
                newValue is State.Running && oldValue is State.Running &&
                    newValue.playbackState != oldValue.playbackState -> {
                    listener.onPlaybackStateChanged(newValue.playbackState)
                }
            }
        }
    }

    init {
        exoPlayer.addListener(Listener())
    }

    override fun start() {
        val playbackState = when (exoPlayer.playbackState) {
            Player.STATE_READY -> PlaybackEngine.PlaybackState.Playing
            Player.STATE_BUFFERING -> PlaybackEngine.PlaybackState.Starved
            else -> throw IllegalStateException("Unexpected ExoPlayer state ${exoPlayer.playbackState}")
        }

        listener.onStartRequested(playbackState)
        exoPlayer.playWhenReady = true
        state = State.Running(playbackState = playbackState, paused = false)
    }

    override fun stop() {
        exoPlayer.playWhenReady = false
        exoPlayer.seekTo(0)
        state = State.Idle
    }

    override fun resume() {
        state = when (val stateNow = state) {
            State.Idle, State.Ended -> {
                stateNow
            }
            is State.Running -> {
                exoPlayer.playWhenReady = true
                stateNow.copy(paused = false)
            }
        }
    }

    override fun pause() {
        state = when (val stateNow = state) {
            State.Idle, State.Ended -> {
                stateNow
            }
            is State.Running -> {
                exoPlayer.playWhenReady = false
                stateNow.copy(paused = true)
            }
        }
    }

    public override fun release() {
        exoPlayer.release()
    }

    @OptIn(InternalReadiumApi::class)
    private fun ExoPlaybackException.toError(): Error {
        val readError =
            if (type == ExoPlaybackException.TYPE_SOURCE) {
                sourceException.findInstance(ReadException::class.java)?.error
            } else {
                null
            }

        return if (readError == null) {
            Error.Engine(ThrowableError(this))
        } else {
            Error.Source(readError)
        }
    }
}
