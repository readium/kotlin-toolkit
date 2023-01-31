/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Locator

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

            val initialContext = tryOrNull { contentIterator.startContext() }
                ?: return null

            val ttsEngineFacade = TtsEngineFacade(engine)

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
                val actualCurrentUtterance = previousUtterance ?: return null
                val actualPreviousUtterance = previousUtterance()

                // Go back to the end of the iterator.
                nextUtterance()

                Context(
                    previousUtterance = actualPreviousUtterance,
                    currentUtterance = actualCurrentUtterance,
                    nextUtterance = null,
                    ended = true
                )
            }

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

    private var context: Context =
        initialContext

    private var playbackJob: Job? =
        null

    private val mutex: Mutex =
        Mutex()

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

    override val settings: StateFlow<S> =
        engineFacade.settings

    val voices: Set<V> =
        engineFacade.voices

    val playback: StateFlow<Playback> =
        playbackMutable.asStateFlow()

    val utterance: StateFlow<Utterance> =
        utteranceMutable.asStateFlow()

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
        playbackMutable.value = playbackMutable.value.copy(error = null)
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
        if (playbackMutable.value.state == Playback.State.Ended) {
            playbackMutable.value = playbackMutable.value.copy(state = Playback.State.Ready)
        }
        utteranceMutable.value = utteranceMutable.value.copy(range = null)
        playbackJob?.join()
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

        playbackJob?.cancel()
        tryLoadNextContext()
        playbackJob?.join()
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
        if (playback.value.playWhenReady && playback.value.state == Playback.State.Ready) {
            playbackJob = coroutineScope.launch {
                playContinuous()
            }
        }
    }

    private suspend fun tryLoadPreviousContext() {
        val contextNow = context

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

        context = Context(
            previousUtterance = previousUtterance,
            currentUtterance = checkNotNull(contextNow.previousUtterance),
            nextUtterance = contextNow.currentUtterance
        )
        utteranceMutable.value = context.currentUtterance.ttsPlayerUtterance()
    }

    private suspend fun tryLoadNextContext() {
        val contextNow = context

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

        context = Context(
            previousUtterance = contextNow.currentUtterance,
            currentUtterance = contextNow.nextUtterance,
            nextUtterance = nextUtterance
        )
        utteranceMutable.value = context.currentUtterance.ttsPlayerUtterance()
        if (playbackMutable.value.state == Playback.State.Ended) {
            playbackMutable.value = playbackMutable.value.copy(state = Playback.State.Ready)
        }
    }

    private suspend fun resetContext() {
        val startContext = try {
            contentIterator.startContext()
        } catch (e: Exception) {
            onContentError(e)
            return
        }
        context = checkNotNull(startContext)
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
        if (!coroutineContext.isActive) {
            return
        }

        val error = speakUtterance(context.currentUtterance)

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
            state = Playback.State.Error,
            error = Error.EngineError(error)
        )
        playbackJob?.cancel()
    }

    private fun onContentError(exception: Exception) {
        playbackMutable.value = playbackMutable.value.copy(
            state = Playback.State.Error,
            error = Error.ContentError(exception)
        )
        playbackJob?.cancel()
    }

    private fun onRangeChanged(range: IntRange) {
        val newUtterance = utteranceMutable.value.copy(range = range)
        utteranceMutable.value = newUtterance
    }

    fun close() {
        engineFacade.close()
    }

    override fun submitPreferences(preferences: P) {
        lastPreferences = preferences
        engineFacade.submitPreferences(preferences)
        contentIterator.language = engineFacade.settings.value.language
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
