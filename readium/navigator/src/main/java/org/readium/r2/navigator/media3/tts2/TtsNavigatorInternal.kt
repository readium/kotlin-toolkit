/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalReadiumApi
internal class TtsNavigatorInternal<S : TtsSettings, P : TtsPreferences<P>>(
    private val ttsPlayer: TtsPlayer<S, P>,
    private val sessionAdapter: TtsSessionAdapter,
) : MediaNavigatorInternal<TtsLocator, TtsPlayback>, Configurable<S, P> by ttsPlayer {

    companion object {

        suspend operator fun <S : TtsSettings, P : TtsPreferences<P>> invoke(
            application: Application,
            ttsEngine: TtsEngine<S, P>,
            ttsContentIterator: TtsContentIterator,
            playlistMetadata: MediaMetadata,
            mediaItems: List<MediaItem>,
            listener: TtsNavigatorListener
        ): TtsNavigatorInternal<S, P>? {

            val playerListener = object : TtsPlayer.Listener {

                override fun onPlaybackException() {
                    listener.onPlaybackException()
                }
            }

            val ttsPlayer =
                TtsPlayer(ttsEngine, ttsContentIterator, playerListener)
                    ?: return null

            val sessionAdapter =
                TtsSessionAdapter(application, ttsPlayer, playlistMetadata, mediaItems, listener::onStopRequested)

            return TtsNavigatorInternal(ttsPlayer, sessionAdapter)
        }
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    override val playback: StateFlow<TtsPlayback> =
        ttsPlayer.playback
            .mapStateIn(coroutineScope) { playback ->
                val state = when (playback.state) {
                    TtsPlayer.Playback.State.READY ->
                        if (playback.playWhenReady) MediaNavigatorInternal.State.Playing
                        else MediaNavigatorInternal.State.Paused
                    TtsPlayer.Playback.State.ENDED ->
                        MediaNavigatorInternal.State.Ended
                }

                val token = playback.range
                    ?.let { playback.locator.substring(playback.range) }

                TtsPlayback(
                    state = state,
                    locator = playback.locator,
                    token = token
                )
            }

    override fun play() {
        ttsPlayer.play()
    }

    override fun pause() {
        ttsPlayer.pause()
    }

    override fun go(locator: TtsLocator) {
        ttsPlayer.go(locator)
    }

    override fun goForward() {
        ttsPlayer.nextUtterance()
    }

    override fun goBackward() {
        ttsPlayer.previousUtterance()
    }

    override fun asPlayer(): Player {
        return sessionAdapter
    }

    fun close() {
        ttsPlayer.close()
    }
}
