/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Locator

/**
 * Plays the content from a [TtsContentIterator] with a [TtsEngine].
 */
@ExperimentalReadiumApi
internal class TtsPlayer<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice> private constructor(
    private val engineFacade: TtsEngineFacade<S, P, E, V>,
    private val contentIterator: TtsContentIterator,
    initialWindow: UtteranceWindow,
    initialPreferences: P
) : Configurable<S, P> {

    companion object {

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            engine: TtsEngine<S, P, E, V>,
            contentIterator: TtsContentIterator,
            initialPreferences: P,
        ): TtsPlayer<S, P, E, V>? {

            val initialContext = tryOrNull { contentIterator.startContext() }
                ?: return null

            val ttsEngineFacade =
                TtsEngineFacade(
                    engine
                )

            return TtsPlayer(
                ttsEngineFacade,
                contentIterator,
                initialContext,
                initialPreferences
            )
        }

        private suspend fun TtsContentIterator.startContext(): UtteranceWindow? {
            val previousUtterance = previousUtterance()
            val currentUtterance = nextUtterance()

            val startWindow = if (currentUtterance != null) {
                UtteranceWindow(
                    previousUtterance = previousUtterance,
                    currentUtterance = currentUtterance,
                    nextUtterance = nextUtterance(),
                    ended = false
                )
            } else {
                val actualCurrentUtterance = previousUtterance ?: return null
                val actualPreviousUtterance = previousUtterance()

                // Go back to the end of the iterator.
                nextUtterance()

                UtteranceWindow(
                    previousUtterance = actualPreviousUtterance,
                    currentUtterance = actualCurrentUtterance,
                    nextUtterance = null,
                    ended = true
                )
            }

            return startWindow
        }
    }

    /**
     * State of the player.
     */
    sealed interface State {

        /**
         * The player is ready to play.
         */
        object Ready : State

        /**
         * The end of the media has been reached.
         */
        object Ended : State

        /**
         * The player cannot play because an error occurred.
         */
        sealed class Error : State {

            data class EngineError<E : TtsEngine.Error> (val error: E) : Error()

            data class ContentError(val exception: Exception) : Error()
        }
    }

    data class Playback(
        val state: State,
        val playWhenReady: Boolean,
    )

    data class Utterance(
        val text: String,
        val position: Position,
        val range: IntRange?
    ) {

        data class Position(
            val resourceIndex: Int,
            val cssSelector: String,
            val textBefore: String?,
            val textAfter: String?,
        )
    }

    private data class UtteranceWindow(
        val previousUtterance: TtsContentIterator.Utterance?,
        val currentUtterance: TtsContentIterator.Utterance,
        val nextUtterance: TtsContentIterator.Utterance?,
        val ended: Boolean = false
    )

    private val coroutineScope: CoroutineScope =
        MainScope()

    private var utteranceWindow: UtteranceWindow =
        initialWindow

    private var playbackJob: Job? =
        null

    private val mutex: Mutex =
        Mutex()

    private val playbackMutable: MutableStateFlow<Playback> =
        MutableStateFlow(
            Playback(
                state = if (initialWindow.ended) State.Ended else State.Ready,
                playWhenReady = false
            )
        )

    private val utteranceMutable: MutableStateFlow<Utterance> =
        MutableStateFlow(initialWindow.currentUtterance.ttsPlayerUtterance())

    override val settings: StateFlow<S> =
        engineFacade.settings

    val voices: Set<V> =
        engineFacade.voices

    val playback: StateFlow<Playback> =
        playbackMutable.asStateFlow()

    val utterance: StateFlow<Utterance> =
        utteranceMutable.asStateFlow()

    /**
     * We need to keep the last submitted preferences because TtsSessionAdapter deals with
     * preferences, not settings.
     */
    var lastPreferences: P =
        initialPreferences

    init {
        submitPreferences(initialPreferences)
    }

    fun play() {
        coroutineScope.launch {
            playAsync()
        }
    }

    private suspend fun playAsync() = mutex.withLock {
        if (isPlaying()) {
            return
        }

        playbackMutable.value = playbackMutable.value.copy(playWhenReady = true)
        playIfReadyAndNotPaused()
    }

    fun pause() {
        coroutineScope.launch {
            pauseAsync()
        }
    }

    private suspend fun pauseAsync() = mutex.withLock {
        if (!playbackMutable.value.playWhenReady) {
            return
        }

        playbackMutable.value = playbackMutable.value.copy(playWhenReady = false)
        utteranceMutable.value = utteranceMutable.value.copy(range = null)
        playbackJob?.cancelAndJoin()
        Unit
    }

    fun tryRecover() {
        coroutineScope.launch {
            tryRecoverAsync()
        }
    }

    private suspend fun tryRecoverAsync() = mutex.withLock {
        playbackMutable.value = playbackMutable.value.copy(state = State.Ready)
        utteranceMutable.value = utteranceMutable.value.copy(range = null)
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    fun go(locator: Locator) {
        coroutineScope.launch {
            goAsync(locator)
        }
    }

    private suspend fun goAsync(locator: Locator) = mutex.withLock {
        playbackJob?.cancel()
        contentIterator.seek(locator)
        resetContext()
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    fun go(resourceIndex: Int) {
        coroutineScope.launch {
            goAsync(resourceIndex)
        }
    }

    private suspend fun goAsync(resourceIndex: Int) = mutex.withLock {
        playbackJob?.cancel()
        contentIterator.seekToResource(resourceIndex)
        resetContext()
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    fun restartUtterance() {
        coroutineScope.launch {
            restartUtteranceAsync()
        }
    }

    private suspend fun restartUtteranceAsync() = mutex.withLock {
        playbackJob?.cancel()
        if (playbackMutable.value.state == State.Ended) {
            playbackMutable.value = playbackMutable.value.copy(state = State.Ready)
        }
        utteranceMutable.value = utteranceMutable.value.copy(range = null)
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    fun hasNextUtterance() =
        utteranceWindow.nextUtterance != null

    fun nextUtterance() {
        coroutineScope.launch {
            nextUtteranceAsync()
        }
    }

    private suspend fun nextUtteranceAsync() = mutex.withLock {
        if (utteranceWindow.nextUtterance == null) {
            return
        }

        playbackJob?.cancel()
        tryLoadNextContext()
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    fun hasPreviousUtterance() =
        utteranceWindow.previousUtterance != null

    fun previousUtterance() {
        coroutineScope.launch {
            previousUtteranceAsync()
        }
    }

    private suspend fun previousUtteranceAsync() = mutex.withLock {
        if (utteranceWindow.previousUtterance == null) {
            return
        }
        playbackJob?.cancel()
        tryLoadPreviousContext()
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    fun hasNextResource(): Boolean =
        utteranceMutable.value.position.resourceIndex + 1 < contentIterator.resourceCount

    fun nextResource() {
        coroutineScope.launch {
            nextResourceAsync()
        }
    }

    private suspend fun nextResourceAsync() = mutex.withLock {
        if (!hasNextUtterance()) {
            return
        }

        playbackJob?.cancel()
        val currentIndex = utteranceMutable.value.position.resourceIndex
        contentIterator.seekToResource(currentIndex + 1)
        resetContext()
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    fun hasPreviousResource(): Boolean =
        utteranceMutable.value.position.resourceIndex > 0

    fun previousResource() {
        coroutineScope.launch {
            previousResourceAsync()
        }
    }

    private suspend fun previousResourceAsync() = mutex.withLock {
        if (!hasPreviousResource()) {
            return
        }
        playbackJob?.cancel()
        val currentIndex = utteranceMutable.value.position.resourceIndex
        contentIterator.seekToResource(currentIndex - 1)
        resetContext()
        playbackJob?.join()
        playIfReadyAndNotPaused()
    }

    private fun playIfReadyAndNotPaused() {
        check(playbackJob?.isCompleted ?: true)
        if (playback.value.playWhenReady && playback.value.state == State.Ready) {
            playbackJob = coroutineScope.launch {
                playContinuous()
            }
        }
    }

    private suspend fun tryLoadPreviousContext() {
        val contextNow = utteranceWindow

        val previousUtterance =
            try {
                // Get previously currentUtterance once more
                contentIterator.previousUtterance()

                // Get previously previousUtterance once more
                contentIterator.previousUtterance()

                // Get new previous utterance
                val previousUtterance = contentIterator.previousUtterance()

                // Go to currentUtterance position
                contentIterator.nextUtterance()

                // Go to nextUtterance position
                contentIterator.nextUtterance()

                previousUtterance
            } catch (e: Exception) {
                onContentError(e)
                return
            }

        utteranceWindow = UtteranceWindow(
            previousUtterance = previousUtterance,
            currentUtterance = checkNotNull(contextNow.previousUtterance),
            nextUtterance = contextNow.currentUtterance
        )
        utteranceMutable.value = utteranceWindow.currentUtterance.ttsPlayerUtterance()
    }

    private suspend fun tryLoadNextContext() {
        val contextNow = utteranceWindow

        if (contextNow.nextUtterance == null) {
            onEndReached()
            return
        }

        val nextUtterance = try {
            contentIterator.nextUtterance()
        } catch (e: Exception) {
            onContentError(e)
            return
        }

        utteranceWindow = UtteranceWindow(
            previousUtterance = contextNow.currentUtterance,
            currentUtterance = contextNow.nextUtterance,
            nextUtterance = nextUtterance
        )
        utteranceMutable.value = utteranceWindow.currentUtterance.ttsPlayerUtterance()
        if (playbackMutable.value.state == State.Ended) {
            playbackMutable.value = playbackMutable.value.copy(state = State.Ready)
        }
    }

    private suspend fun resetContext() {
        val startContext = try {
            contentIterator.startContext()
        } catch (e: Exception) {
            onContentError(e)
            return
        }
        utteranceWindow = checkNotNull(startContext)
        if (utteranceWindow.nextUtterance == null && utteranceWindow.ended) {
            onEndReached()
        }
    }

    private fun onEndReached() {
        playbackMutable.value = playbackMutable.value.copy(
            state = State.Ended,
        )
    }

    private suspend fun playContinuous() {
        if (!coroutineContext.isActive) {
            return
        }

        val error = speakUtterance(utteranceWindow.currentUtterance)

        mutex.withLock {
            error?.let { exception -> onEngineError(exception) }
            tryLoadNextContext()
        }
        playContinuous()
    }

    private suspend fun speakUtterance(utterance: TtsContentIterator.Utterance): E? =
        engineFacade.speak(utterance.text, utterance.language, ::onRangeChanged)

    private fun onEngineError(error: E) {
        playbackMutable.value = playbackMutable.value.copy(
            state = State.Error.EngineError(error)
        )
        playbackJob?.cancel()
    }

    private fun onContentError(exception: Exception) {
        playbackMutable.value = playbackMutable.value.copy(
            state = State.Error.ContentError(exception)
        )
        playbackJob?.cancel()
    }

    private fun onRangeChanged(range: IntRange) {
        val newUtterance = utteranceMutable.value.copy(range = range)
        utteranceMutable.value = newUtterance
    }

    fun close() {
        coroutineScope.cancel()
        engineFacade.close()
    }

    override fun submitPreferences(preferences: P) {
        lastPreferences = preferences
        engineFacade.submitPreferences(preferences)
        contentIterator.language = engineFacade.settings.value.language
        contentIterator.overrideContentLanguage = engineFacade.settings.value.overrideContentLanguage
    }

    private fun isPlaying() =
        playbackMutable.value.playWhenReady && playback.value.state == State.Ready

    private fun TtsContentIterator.Utterance.ttsPlayerUtterance(): Utterance =
        Utterance(
            text = text,
            range = null,
            position = Utterance.Position(
                resourceIndex = resourceIndex,
                cssSelector = cssSelector,
                textAfter = textAfter,
                textBefore = textBefore
            )
        )
}
