/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.navigator.media3.api.SynchronizedMediaNavigatorInternal
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
internal class TtsNavigatorInternal<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice>(
    private val ttsPlayer: TtsPlayer<S, P, E, V>,
    private val sessionAdapter: TtsSessionAdapter<E>,
    coroutineScope: CoroutineScope,
) : SynchronizedMediaNavigatorInternal<TtsNavigatorInternal.Position,
        TtsNavigatorInternal.RelaxedPosition, TtsNavigatorInternal.Error>,
    Configurable<S, P> by ttsPlayer {

    companion object {

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            application: Application,
            ttsEngine: TtsEngine<S, P, E, V>,
            ttsContentIterator: TtsContentIterator,
            playlistMetadata: MediaMetadata,
            mediaItems: List<MediaItem>,
            getPlaybackParameters: (S) -> PlaybackParameters,
            updatePlaybackParameters: (P, PlaybackParameters) -> P,
            mapEngineError: (E) -> PlaybackException,
            initialPreferences: P,
            listener: TtsNavigatorListener
        ): TtsNavigatorInternal<S, P, E, V>? {
            val ttsPlayer =
                TtsPlayer(ttsEngine, ttsContentIterator, initialPreferences)
                    ?: return null

            val coroutineScope =
                MainScope()

            val playbackParameters =
                ttsPlayer.settings.mapStateIn(coroutineScope) {
                    getPlaybackParameters(it)
                }

            val onSetPlaybackParameters = { parameters: PlaybackParameters ->
                val newPreferences = updatePlaybackParameters(ttsPlayer.lastPreferences, parameters)
                ttsPlayer.submitPreferences(newPreferences)
            }

            val sessionAdapter =
                TtsSessionAdapter(
                    application,
                    ttsPlayer,
                    playlistMetadata,
                    mediaItems,
                    listener::onStopRequested,
                    playbackParameters,
                    onSetPlaybackParameters,
                    mapEngineError
                )

            return TtsNavigatorInternal(ttsPlayer, sessionAdapter, coroutineScope)
        }
    }

    data class RelaxedPosition(
        val locator: Locator
    ) : MediaNavigatorInternal.RelaxedPosition

    data class Position(
        val resourceIndex: Int,
        val cssSelector: String,
        val textBefore: String?,
        val textAfter: String?,
    ) : MediaNavigatorInternal.Position

    sealed class Error : MediaNavigatorInternal.Error {

        data class EngineError<E : TtsEngine.Error> (val error: E) : Error()

        data class ContentError(val exception: Exception) : Error()
    }

    override val playback: StateFlow<MediaNavigatorInternal.Playback<Error>> =
        ttsPlayer.playback.mapStateIn(coroutineScope) { it.toPlayback() }

    override val utterance: StateFlow<SynchronizedMediaNavigatorInternal.Utterance<Position>> =
        ttsPlayer.utterance.mapStateIn(coroutineScope) { it.toUtterance() }

    override val progression: StateFlow<Position> =
        utterance.mapStateIn(coroutineScope) { it.position }

    override fun play() {
        ttsPlayer.play()
    }

    override fun pause() {
        ttsPlayer.pause()
    }

    override fun go(position: RelaxedPosition) {
        ttsPlayer.go(position.locator)
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

    private fun TtsPlayer.Utterance.toUtterance() =
        SynchronizedMediaNavigatorInternal.Utterance(
            text = text,
            range = range,
            position = Position(
                resourceIndex = position.resourceIndex,
                cssSelector = position.cssSelector,
                textBefore = position.textBefore,
                textAfter = position.textAfter
            )
        )

    private fun TtsPlayer.Playback.toPlayback() =
        MediaNavigatorInternal.Playback(
            state = state.toState(),
            playWhenReady = playWhenReady,
            error = error?.toError()
        )

    private fun TtsPlayer.Playback.State.toState() =
        when (this) {
            TtsPlayer.Playback.State.Ready -> MediaNavigatorInternal.State.Ready
            TtsPlayer.Playback.State.Ended -> MediaNavigatorInternal.State.Ended
            TtsPlayer.Playback.State.Error -> MediaNavigatorInternal.State.Error
        }

    private fun TtsPlayer.Error.toError(): Error =
        when (this) {
            is TtsPlayer.Error.ContentError -> Error.ContentError(exception)
            is TtsPlayer.Error.EngineError<*> -> Error.EngineError(error)
        }
}
