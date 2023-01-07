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
    application: Application,
    private val ttsEngineFacade: TtsEngineFacade<S, P>,
    private val player: TtsPlayer,
) : MediaNavigatorInternal<TtsLocator, TtsPlayback>, Configurable<S, P> by ttsEngineFacade {

    companion object {

        suspend operator fun <S : TtsSettings, P : TtsPreferences<P>> invoke(
            application: Application,
            ttsEngine: TtsEngine<S, P>,
            ttsContentIterator: TtsContentIterator,
            playlistMetadata: MediaMetadata,
            mediaItems: List<MediaItem>,
            listener: TtsNavigatorListener
        ): TtsNavigatorInternal<S, P>? {

            val facadeListener = object : TtsEngineFacadeListener {

                override fun onNavigatorStopped() {
                    listener.onStopRequested()
                }

                override fun onPlaybackException() {
                    listener.onPlaybackException()
                }
            }

            val ttsEngineFacade =
                TtsEngineFacade(ttsEngine, ttsContentIterator, facadeListener)
                    ?: return null

            val player =
                TtsPlayer(application, ttsEngineFacade, playlistMetadata, mediaItems)

            return TtsNavigatorInternal(application, ttsEngineFacade, player)
        }
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    override val playback: StateFlow<TtsPlayback> =
        ttsEngineFacade.playback
            .mapStateIn(coroutineScope) { playback ->
                val state = when (playback.state) {
                    TtsEngineFacadePlayback.State.READY ->
                        if (playback.playWhenReady) MediaNavigatorInternal.State.Playing
                        else MediaNavigatorInternal.State.Paused
                    TtsEngineFacadePlayback.State.ENDED ->
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
        ttsEngineFacade.play()
    }

    override fun pause() {
        ttsEngineFacade.pause()
    }

    override fun go(locator: TtsLocator) {
        ttsEngineFacade.go(locator)
    }

    override fun goForward() {
        ttsEngineFacade.nextUtterance()
    }

    override fun goBackward() {
        ttsEngineFacade.previousUtterance()
    }

    override fun asPlayer(): Player {
        return player
    }

    fun close() {
        ttsEngineFacade.close()
    }
}
