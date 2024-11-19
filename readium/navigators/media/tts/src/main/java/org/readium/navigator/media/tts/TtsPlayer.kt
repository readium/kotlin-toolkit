/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
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
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.ThrowableError
import timber.log.Timber

/**
 * Plays the content from a [TtsUtteranceIterator] with a [TtsEngine].
 */
@ExperimentalReadiumApi
internal class TtsPlayer<
    S : TtsEngine.Settings,
    P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error,
    V : TtsEngine.Voice,
    > private constructor(
    private val engineFacade: TtsEngineFacade<S, P, E, V>,
    private val contentIterator: TtsUtteranceIterator,
    initialWindow: UtteranceWindow,
    initialPreferences: P,
) : Configurable<S, P> {

    companion object {

        suspend operator fun <
            S : TtsEngine.Settings,
            P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error,
            V : TtsEngine.Voice,
            > invoke(
            engine: TtsEngine<S, P, E, V>,
            contentIterator: TtsUtteranceIterator,
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

        private suspend fun TtsUtteranceIterator.startContext(): UtteranceWindow? {
            val previousUtterance = previous()
            val currentUtterance = next()

            val startWindow = if (currentUtterance != null) {
                UtteranceWindow(
                    previousUtterance = previousUtterance,
                    currentUtterance = currentUtterance,
                    nextUtterance = next(),
                    ended = false
                )
            } else {
                val actualCurrentUtterance = previousUtterance ?: return null
                val actualPreviousUtterance = previous()

                // Go back to the end of the iterator.
                next()

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
        data object Ready : State

        /**
         * The end of the media has been reached.
         */
        data object Ended : State

        /**
         * The player cannot play because an error occurred.
         */
        sealed class Failure : State {

            data class Engine<E : TtsEngine.Error>(val error: E) : Failure()

            data class Content(val error: Error) : Failure()
        }
    }

    data class Playback(
        val state: State,
        val playWhenReady: Boolean,
    )

    data class Utterance(
        val text: String,
        val position: Position,
        val range: IntRange?,
    ) {

        data class Position(
            val resourceIndex: Int,
            val locations: Locator.Locations,
            val text: Locator.Text,
        )
    }

    private data class UtteranceWindow(
        val previousUtterance: TtsUtteranceIterator.Utterance?,
        val currentUtterance: TtsUtteranceIterator.Utterance,
        val nextUtterance: TtsUtteranceIterator.Utterance?,
        val ended: Boolean = false,
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
        submitPreferencesForSure(initialPreferences)
    }

    fun play() {
        // This can be called by the session adapter with a pending intent for a foreground service
        // if playWhenReady is false or the state is Ended.
        // We must keep or transition to a state which will be translated by media3 to a
        // foreground state.
        // If the state was State.Ended, it will get back to its initial value later.

        if (playbackMutable.value.playWhenReady && playback.value.state == State.Ready) {
            return
        }

        playbackMutable.value =
            playbackMutable.value.copy(state = State.Ready, playWhenReady = true)

        coroutineScope.launch {
            mutex.withLock {
                // WORKAROUND to get the media buttons correctly working when an audio player was
                // running before.
                fakePlayingAudio()
                playbackJob?.cancelAndJoin()
                playIfReadyAndNotPaused()
            }
        }
    }

    private fun fakePlayingAudio() {
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

        val audioFormat =
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()

        val bufferSize = 8092

        val audioTrack =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tryOrNull {
                    AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                }
            } else {
                AudioTrack(
                    audioAttributes,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                ).takeIf { it.state == AudioTrack.STATE_INITIALIZED }
            }

        audioTrack
            ?.play()
            ?: run { Timber.e("Couldn't fake playing audio.") }
    }

    fun pause() {
        if (!playbackMutable.value.playWhenReady) {
            return
        }

        playbackMutable.value = playbackMutable.value.copy(playWhenReady = false)
        utteranceMutable.value = utteranceMutable.value.copy(range = null)

        coroutineScope.launch {
            mutex.withLock {
                playbackJob?.cancelAndJoin()
            }
        }
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
        if (playbackMutable.value.state == State.Ended) {
            playbackMutable.value = playbackMutable.value.copy(state = State.Ready)
        }

        coroutineScope.launch {
            mutex.withLock {
                playbackJob?.cancel()
                playbackJob?.join()
                utteranceMutable.value = utteranceMutable.value.copy(range = null)
                playIfReadyAndNotPaused()
            }
        }
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

    @Suppress("unused")
    fun hasNextResource(): Boolean =
        utteranceMutable.value.position.resourceIndex + 1 < contentIterator.resourceCount

    @Suppress("unused")
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

    @Suppress("MemberVisibilityCanBePrivate")
    fun hasPreviousResource(): Boolean =
        utteranceMutable.value.position.resourceIndex > 0

    @Suppress("unused")
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
                contentIterator.previous()

                // Get previously previousUtterance once more
                contentIterator.previous()

                // Get new previous utterance
                val previousUtterance = contentIterator.previous()

                // Go to currentUtterance position
                contentIterator.next()

                // Go to nextUtterance position
                contentIterator.next()

                previousUtterance
            } catch (e: Exception) {
                onContentException(e)
                return
            }

        utteranceWindow = UtteranceWindow(
            previousUtterance = previousUtterance,
            currentUtterance = checkNotNull(contextNow.previousUtterance),
            nextUtterance = contextNow.currentUtterance
        )
        utteranceMutable.value = utteranceWindow.currentUtterance.ttsPlayerUtterance()

        if (playbackMutable.value.state == State.Ended) {
            playbackMutable.value = playbackMutable.value.copy(state = State.Ready)
        }
    }

    private suspend fun tryLoadNextContext() {
        val contextNow = utteranceWindow

        if (contextNow.nextUtterance == null) {
            onEndReached()
            return
        }

        val nextUtterance = try {
            contentIterator.next()
        } catch (e: Exception) {
            onContentException(e)
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
            onContentException(e)
            return
        }
        utteranceWindow = checkNotNull(startContext)
        if (utteranceWindow.nextUtterance == null && utteranceWindow.ended) {
            onEndReached()
        }
    }

    private fun onEndReached() {
        playbackMutable.value = playbackMutable.value.copy(
            state = State.Ended
        )
        playbackJob?.cancel()
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

    private suspend fun speakUtterance(utterance: TtsUtteranceIterator.Utterance): E? =
        engineFacade.speak(utterance.utterance, utterance.language, ::onRangeChanged)

    private fun onEngineError(error: E) {
        playbackMutable.value = playbackMutable.value.copy(
            state = State.Failure.Engine(error)
        )
        playbackJob?.cancel()
    }

    private fun onContentException(exception: Exception) {
        val error =
            if (exception is ErrorException) {
                exception.error
            } else {
                ThrowableError(exception)
            }
        onContentError(error)
    }

    private fun onContentError(error: Error) {
        playbackMutable.value = playbackMutable.value.copy(
            state = State.Failure.Content(error)
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
        if (preferences == lastPreferences) {
            return
        }

        submitPreferencesForSure(preferences)
        restartUtterance()
    }

    private fun submitPreferencesForSure(preferences: P) {
        lastPreferences = preferences
        engineFacade.submitPreferences(preferences)
        contentIterator.language = engineFacade.settings.value.language
        contentIterator.overrideContentLanguage = engineFacade.settings.value.overrideContentLanguage
    }

    private fun TtsUtteranceIterator.Utterance.ttsPlayerUtterance(): Utterance =
        Utterance(
            text = utterance,
            range = null,
            position = Utterance.Position(
                resourceIndex = resourceIndex,
                locations = locations,
                text = text
            )
        )
}
