/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import timber.log.Timber

@ExperimentalReadiumApi
internal class TtsPlayer<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice> private constructor(
    private val engineFacade: TtsEngineFacade<S, P, E, V>,
    private val contentIterator: TtsContentIterator,
    initialContext: Context,
    initialPreferences: P
) : Configurable<S, P> {

    companion object {

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            engine: TtsEngine<S, P, E, V>,
            contentIterator: TtsContentIterator,
            initialPreferences: P,
        ): TtsPlayer<S, P, E, V>? {

            val initialContext = contentIterator.startContext()
                ?: return null

            val ttsEngineFacade = TtsEngineFacade(engine)
            ttsEngineFacade.submitPreferences(initialPreferences)
            contentIterator.language = ttsEngineFacade.settings.value.language

            return TtsPlayer(ttsEngineFacade, contentIterator, initialContext, initialPreferences)
        }

        private suspend fun TtsContentIterator.startContext(): Context? {
            val previousUtterance = previousUtterance()
            val currentUtterance = nextUtterance()

            val context = if (currentUtterance != null) {
                Context(
                    previousUtterance = previousUtterance,
                    currentUtterance = currentUtterance,
                    nextUtterance = nextUtterance(),
                    ended = false
                )
            } else {
                Context(
                    previousUtterance = previousUtterance(),
                    currentUtterance = nextUtterance() ?: return null,
                    nextUtterance = null,
                    ended = true
                )
            }

            Timber.d("startContext $context")

            return context
        }
    }

    sealed class Error {

        data class EngineError<E : TtsEngine.Error> (val error: E) : Error()

        data class ContentError(val exception: Exception) : Error()
    }

    data class Playback(
        val state: State,
        val playWhenReady: Boolean,
        val error: Error?
    ) {

        enum class State {
            Ready,
            Ended,
            Error;
        }
    }

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

    private data class Context(
        val previousUtterance: TtsContentIterator.Utterance?,
        val currentUtterance: TtsContentIterator.Utterance,
        val nextUtterance: TtsContentIterator.Utterance?,
        val ended: Boolean = false
    )

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val playbackMutable: MutableStateFlow<Playback> =
        MutableStateFlow(
            Playback(
                state = if (initialContext.ended) Playback.State.Ended else Playback.State.Ready,
                playWhenReady = false,
                error = null
            )
        )

    private val utteranceMutable: MutableStateFlow<Utterance> =
        MutableStateFlow(initialContext.currentUtterance.ttsPlayerUtterance())

    private var context: Context =
        initialContext

    private var playbackJob: Job? = null

    private val mutex = Mutex()

    val voices: Set<V> get() =
        engineFacade.voices

    val playback: StateFlow<Playback> =
        playbackMutable.asStateFlow()

    val utterance: StateFlow<Utterance> =
        utteranceMutable.asStateFlow()

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

        playbackJob?.cancelAndJoin()
        playbackMutable.value = playbackMutable.value.copy(playWhenReady = false)
        utteranceMutable.value = utteranceMutable.value.copy(range = null)
    }

    fun go(locator: Locator) {
        coroutineScope.launch {
            goAsync(locator)
        }
    }

    private suspend fun goAsync(locator: Locator) = mutex.withLock {
        playbackJob?.cancelAndJoin()
        contentIterator.seek(locator)
        resetContext()
        playIfReadyAndNotPaused()
    }

    fun go(resourceIndex: Int) {
        coroutineScope.launch {
            goAsync(resourceIndex)
        }
    }

    private suspend fun goAsync(resourceIndex: Int) = mutex.withLock {
        playbackJob?.cancelAndJoin()
        contentIterator.seekToResource(resourceIndex)
        resetContext()
        playIfReadyAndNotPaused()
    }

    fun restartUtterance() {
        coroutineScope.launch {
            restartUtteranceAsync()
        }
    }

    private suspend fun restartUtteranceAsync() = mutex.withLock {
        playbackJob?.cancelAndJoin()
        if (playbackMutable.value.state == Playback.State.Ended) {
            playbackMutable.value = playbackMutable.value.copy(state = Playback.State.Ready)
        }
        playIfReadyAndNotPaused()
    }

    fun hasNextUtterance() =
        context.nextUtterance != null

    fun nextUtterance() {
        coroutineScope.launch {
            nextUtteranceAsync()
        }
    }

    private suspend fun nextUtteranceAsync() = mutex.withLock {
        if (context.nextUtterance == null) {
            return
        }

        playbackJob?.cancelAndJoin()
        tryLoadNextContext()
        playIfReadyAndNotPaused()
    }

    fun hasPreviousUtterance() =
        context.previousUtterance != null

    fun previousUtterance() {
        coroutineScope.launch {
            previousUtteranceAsync()
        }
    }

    private suspend fun previousUtteranceAsync() = mutex.withLock {
        if (context.previousUtterance == null) {
            return
        }
        playbackJob?.cancelAndJoin()
        tryLoadPreviousContext()
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

        playbackJob?.cancelAndJoin()
        val currentIndex = utteranceMutable.value.position.resourceIndex
        contentIterator.seekToResource(currentIndex + 1)
        resetContext()
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
        playbackJob?.cancelAndJoin()
        val currentIndex = utteranceMutable.value.position.resourceIndex
        contentIterator.seekToResource(currentIndex - 1)
        resetContext()
        playIfReadyAndNotPaused()
    }

    private fun playIfReadyAndNotPaused() {
        check(playbackJob?.isCompleted ?: true)
        if (playback.value.playWhenReady && playback.value.state == Playback.State.Ready) {
            playbackJob = coroutineScope.launch {
                playContinuous()
            }
        }
    }

    private suspend fun tryLoadPreviousContext() {
        val contextNow = context
        // Get previously nextUtterance once more
        contentIterator.previousUtterance()

        // Get previously currentUtterance once more
        val currentUtterance = checkNotNull(contentIterator.previousUtterance())

        // Get previous utterance
        val previousUtterance = contentIterator.previousUtterance()

        // Get to nextUtterance position
        contentIterator.nextUtterance()
        contentIterator.nextUtterance()

        context = Context(
            previousUtterance = previousUtterance,
            currentUtterance = currentUtterance,
            nextUtterance = contextNow.currentUtterance
        )
        utteranceMutable.value = context.currentUtterance.ttsPlayerUtterance()
    }

    private suspend fun tryLoadNextContext() {
        Timber.d("tryLoadNextContext")
        val contextNow = context
        Timber.d("contextNow $contextNow")

        if (contextNow.nextUtterance == null) {
            onEndReached()
        } else {
            context = Context(
                previousUtterance = contextNow.currentUtterance,
                currentUtterance = contextNow.nextUtterance,
                nextUtterance = contentIterator.nextUtterance()
            )
            Timber.d("newContext $context")
            utteranceMutable.value = context.currentUtterance.ttsPlayerUtterance()
            Timber.d("utterance ${utteranceMutable.value.text}")
            if (playbackMutable.value.state == Playback.State.Ended) {
                playbackMutable.value = playbackMutable.value.copy(state = Playback.State.Ready)
            }
        }
    }

    private suspend fun resetContext() {
        context = checkNotNull(contentIterator.startContext())
        if (context.nextUtterance == null && context.ended) {
            onEndReached()
        }
    }

    private fun onEndReached() {
        playbackMutable.value = playbackMutable.value.copy(
            state = Playback.State.Ended,
        )
    }

    private suspend fun playContinuous() {
        engineFacade.speak(context.currentUtterance.text, context.currentUtterance.language, ::onRangeChanged)
            ?.let { exception -> onEngineError(exception) }
        mutex.withLock { tryLoadNextContext() }
        playContinuous()
    }

    private fun onEngineError(error: E) {
        Timber.d("onEngineError $error")
        playbackMutable.value = playbackMutable.value.copy(
            state = Playback.State.Error,
            error = Error.EngineError(error)
        )
    }

    private fun onRangeChanged(range: IntRange) {
        val newUtterance = utteranceMutable.value.copy(range = range)
        utteranceMutable.value = newUtterance
    }

    fun close() {
        engineFacade.close()
    }

    var lastPreferences: P =
        initialPreferences

    override val settings: StateFlow<S>
        get() = engineFacade.settings

    override fun submitPreferences(preferences: P) {
        lastPreferences = preferences
        engineFacade.submitPreferences(preferences)
    }

    private fun isPlaying() =
        playbackMutable.value.playWhenReady && playback.value.state == Playback.State.Ready

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
