/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import androidx.media3.common.Player
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

@ExperimentalReadiumApi
internal class TtsPlayer<S : TtsSettings, P : TtsPreferences<P>>(
    private val engineFacade: TtsEngineFacade<S, P>,
    private val contentIterator: TtsContentIterator,
    private val listener: Listener,
    firstUtterance: TtsUtterance,
) : Configurable<S, P> by engineFacade {

    companion object {

        suspend operator fun <S : TtsSettings, P : TtsPreferences<P>> invoke(
            engine: TtsEngine<S, P>,
            contentIterator: TtsContentIterator,
            listener: Listener
        ): TtsPlayer<S, P>? {

            val firstUtterance = contentIterator.nextUtterance()
                ?: run {
                    contentIterator.seekToBeginning()
                    contentIterator.nextUtterance()
                } ?: return null

            val ttsEngineFacade = TtsEngineFacade(engine)

            return TtsPlayer(ttsEngineFacade, contentIterator, listener, firstUtterance)
        }
    }

    interface Listener {

        fun onPlaybackException()
    }

    @ExperimentalReadiumApi
    data class Playback(
        val state: State,
        val isPlaying: Boolean,
        val playWhenReady: Boolean,
        val index: Int,
        val locator: TtsLocator,
        val range: IntRange?
    ) {

        enum class State(val value: Int) {
            READY(Player.STATE_READY),
            ENDED(Player.STATE_ENDED);
        }
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val playbackMutable: MutableStateFlow<Playback> =
        MutableStateFlow(
            Playback(
                index = firstUtterance.locator.resourceIndex,
                state = Playback.State.READY,
                isPlaying = false,
                playWhenReady = false,
                locator = firstUtterance.locator,
                range = null
            )
        )

    private var pendingUtterance: TtsUtterance? =
        firstUtterance

    private var playbackJob: Job? = null

    init {
        contentIterator.language = engineFacade.settings.value.language
    }

    val playback: StateFlow<Playback> =
        playbackMutable.asStateFlow()

    fun play() {
        replacePlaybackJob {
            playbackMutable.value =
                playbackMutable.value.copy(
                    state = Playback.State.READY,
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
            contentIterator.seek(locator)
            playContinuous()
        }
    }

    fun nextUtterance() {
        replacePlaybackJob {
            pendingUtterance = null
            playContinuous()
        }
    }

    fun previousUtterance() {
        replacePlaybackJob {
            pendingUtterance = null
            pendingUtterance = contentIterator.previousUtterance()
            playContinuous()
        }
    }

    private fun replacePlaybackJob(block: suspend CoroutineScope.() -> Unit) {
        coroutineScope.launch {
            playbackJob?.cancelAndJoin()
            playbackJob = launch {
                block()
            }
        }
    }

    private suspend fun playContinuous() {
        if (pendingUtterance == null) {
            pendingUtterance = contentIterator.nextUtterance()
        }
        pendingUtterance?.let {
            Timber.d("Setting playback to locator ${it.locator}")
            playbackMutable.value = playbackMutable.value.copy(range = null, locator = it.locator, isPlaying = true)
            engineFacade.speak(it.text, it.language, ::onRangeChanged)
            pendingUtterance = null
            playContinuous()
        } ?: run {
            playbackMutable.value = playbackMutable.value.copy(
                isPlaying = false, playWhenReady = false, state = Playback.State.ENDED
            )
        }
    }

    private fun onRangeChanged(range: IntRange) {
        val newPlayback = playbackMutable.value.copy(range = range)
        playbackMutable.value = newPlayback
    }

    fun close() {
        engineFacade.close()
    }
}
