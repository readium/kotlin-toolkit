/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
internal class TtsEngineFacade<S : TtsSettings, P : TtsPreferences<P>>(
    private val ttsEngine: TtsEngine<S, P>,
    private val ttsContentIterator: TtsContentIterator,
    private val listener: TtsEngineFacadeListener,
    firstUtterance: TtsUtterance,
) : Configurable<S, P> by ttsEngine {

    companion object {

        suspend operator fun <S : TtsSettings, P : TtsPreferences<P>> invoke(
            ttsEngine: TtsEngine<S, P>,
            ttsContentIterator: TtsContentIterator,
            listener: TtsEngineFacadeListener
        ): TtsEngineFacade<S, P>? {

            val firstUtterance = ttsContentIterator.nextUtterance(TtsContentIterator.Direction.Forward)
                ?: run {
                    ttsContentIterator.restart()
                    ttsContentIterator.nextUtterance(TtsContentIterator.Direction.Forward)
                } ?: return null

            return TtsEngineFacade(ttsEngine, ttsContentIterator, listener, firstUtterance)
        }
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private var pendingUtterance: TtsUtterance? =
        firstUtterance

    private var playbackJob: Job? = null

    private val ttsEngineListener = TtsEngineListener()

    private val playbackMutable: MutableStateFlow<TtsEngineFacadePlayback> =
        MutableStateFlow(
            TtsEngineFacadePlayback(
                index = firstUtterance.locator.resourceIndex,
                state = TtsEngineFacadePlayback.State.READY,
                isPlaying = false,
                playWhenReady = false,
                locator = firstUtterance.locator,
                range = null
            )
        )

    init {
        ttsEngine.setListener(ttsEngineListener)
        ttsContentIterator.setLanguage(ttsEngine.settings.value.language)

        ttsEngineListener.state
            .onEach { engineState -> onEngineStateChanged(engineState) }
            .launchIn(coroutineScope)
    }

    private fun onEngineStateChanged(state: TtsEngineListener.EngineState) {
        val newPlayback = playbackMutable.value.copy(
            isPlaying = state.utteranceId != null,
            range = state.range
        )

        playbackMutable.value = newPlayback
    }

    val playback: StateFlow<TtsEngineFacadePlayback> =
        playbackMutable.asStateFlow()

    fun play() {
        replacePlaybackJob {
            playbackMutable.value =
                playbackMutable.value.copy(
                    state = TtsEngineFacadePlayback.State.READY,
                    playWhenReady = true,
                    isPlaying = true
                )
            playContinuous()
        }
    }

    fun pause() {
        replacePlaybackJob {
            playbackMutable.value =
                playbackMutable.value.copy(
                    playWhenReady = false,
                    isPlaying = false
                )
        }
    }

    fun go(locator: TtsLocator) {
        replacePlaybackJob {
            pendingUtterance = null
            ttsContentIterator.seek(locator)
            playContinuous()
        }
    }

    fun nextUtterance() {
        replacePlaybackJob {
            pendingUtterance = null
            ttsContentIterator.nextUtterance(TtsContentIterator.Direction.Forward)
            playContinuous()
        }
    }

    fun previousUtterance() {
        replacePlaybackJob {
            pendingUtterance = null
            ttsContentIterator.nextUtterance(TtsContentIterator.Direction.Backward)
            playContinuous()
        }
    }

    private fun replacePlaybackJob(block: suspend CoroutineScope.() -> Unit) {
        coroutineScope.launch {
            ttsEngine.stop()
            playbackJob?.cancelAndJoin()
            ttsEngineListener.removeAllCallbacks()
            playbackJob = launch {
                block()
            }
        }
    }

    private suspend fun playContinuous() {
        if (pendingUtterance == null) {
            pendingUtterance = ttsContentIterator.nextUtterance(TtsContentIterator.Direction.Forward)
        }
        pendingUtterance?.let {
            playbackMutable.value = playbackMutable.value.copy(range = null, locator = it.locator)
            speakUtterance(it)
            pendingUtterance = null
            playContinuous()
        } ?: run {
            playbackMutable.value = playbackMutable.value.copy(
                isPlaying = false, playWhenReady = false, state = TtsEngineFacadePlayback.State.ENDED
            )
        }
    }

    private suspend fun speakUtterance(utterance: TtsUtterance) =
        suspendCancellableCoroutine { continuation ->
            val id = UUID.randomUUID().toString()
            ttsEngineListener.addCallback(id, continuation)
            ttsEngine.speak(utterance, id)
        }

    fun stop() {
        listener.onNavigatorStopped()
        pause()
    }

    fun close() {
        ttsEngine.close()
    }
}
