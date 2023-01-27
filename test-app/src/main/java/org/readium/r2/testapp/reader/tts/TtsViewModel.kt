/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.media3.androidtts.AndroidTtsEngine
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferences
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferencesEditor
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.api.SynchronizedMediaNavigator
import org.readium.r2.navigator.media3.tts.TtsNavigator
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.reader.ReaderInitData
import org.readium.r2.testapp.reader.VisualReaderInitData
import org.readium.r2.testapp.reader.preferences.PreferencesManager
import org.readium.r2.testapp.utils.extensions.mapStateIn
import timber.log.Timber

/**
 * View model controlling a [TtsNavigator] to read a publication aloud.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 */
@OptIn(ExperimentalReadiumApi::class)
class TtsViewModel private constructor(
    private val viewModelScope: CoroutineScope,
    private val bookId: Long,
    private val publication: Publication,
    private val ttsNavigatorFactory: AndroidTtsNavigatorFactory,
    private val ttsServiceFacade: TtsServiceFacade,
    private val preferencesManager: PreferencesManager<AndroidTtsPreferences>,
    private val createPreferencesEditor: (AndroidTtsPreferences) -> AndroidTtsPreferencesEditor
) {

    companion object {
        /**
         * Returns an instance of [TtsViewModel] if the given [publication] can be played with the
         * TTS engine.
         */
        operator fun invoke(
            viewModelScope: CoroutineScope,
            readerInitData: ReaderInitData,
        ): TtsViewModel? {
            if (readerInitData !is VisualReaderInitData || readerInitData.ttsInitData == null) {
                return null
            }

            return TtsViewModel(
                viewModelScope = viewModelScope,
                bookId = readerInitData.bookId,
                publication = readerInitData.publication,
                ttsNavigatorFactory = readerInitData.ttsInitData.ttsNavigatorFactory,
                ttsServiceFacade = readerInitData.ttsInitData.ttsServiceFacade,
                preferencesManager = readerInitData.ttsInitData.preferencesManager,
                createPreferencesEditor = readerInitData.ttsInitData.ttsNavigatorFactory::createTtsPreferencesEditor
            )
        }
    }

    /**
     * @param showControls Whether the TTS was enabled by the user.
     * @param isPlaying Whether the TTS is currently speaking.
     * @param playingWordRange Locator to the currently spoken word.
     * @param playingUtterance Locator for the currently spoken utterance (e.g. sentence).
     */
    data class State(
        val showControls: Boolean = false,
        val isPlaying: Boolean = false,
        val playingWordRange: Locator? = null,
        val playingUtterance: Locator? = null,
    )

    data class Binding(
        val playbackJob: Job,
        val submitSettingsJob: Job
    )

    sealed class Event {
        /**
         * Emitted when the [PublicationSpeechSynthesizer] fails with an error.
         */
        class OnError(val error: UserException) : Event()

        /**
         * Emitted when the selected language cannot be played because it is missing voice data.
         */
        class OnMissingVoiceData(val language: Language) : Event()
    }

    val editor: StateFlow<AndroidTtsPreferencesEditor> = preferencesManager.preferences
        .mapStateIn(viewModelScope, createPreferencesEditor)

    /**
     * Current state of the view model.
     */
    private val _state: MutableStateFlow<State> = MutableStateFlow(
        stateFromPlayback(navigatorNow?.playback?.value, navigatorNow?.utterance?.value)
    )

    val state: StateFlow<State> = _state.asStateFlow()

    private val _events: Channel<Event> = Channel(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val navigatorNow: AndroidTtsNavigator? get() =
        ttsServiceFacade.sessionNow()?.navigator

    val voices: Set<AndroidTtsEngine.Voice> get() =
        navigatorNow!!.voices

    private var binding: Deferred<Binding?> =
        viewModelScope.async {
            ttsServiceFacade.getSession()?.let { bindSession(it) }
        }

    /**
     * Starts the TTS using the first visible locator in the given [navigator].
     */
    fun start(navigator: Navigator) {
        viewModelScope.launch {
            if (ttsServiceFacade.getSession() != null)
                return@launch

            val session = openSession(navigator)
            binding.cancelAndJoin()
            binding = async { bindSession(session) }
        }
    }

    private suspend fun openSession(navigator: Navigator): TtsService.Session {
        val start = (navigator as? VisualNavigator)?.firstVisibleElementLocator()

        val listener = object : TtsNavigator.Listener {

            override fun onStopRequested() {
                stop()
            }
        }

        val ttsNavigator = ttsNavigatorFactory.createNavigator(
            listener,
            preferencesManager.preferences.value,
            start
        )

        // playWhenReady must be true for the MediaSessionService to call Service.startForeground
        // and prevent crashing
        ttsNavigator.play()
        return ttsServiceFacade.openSession(bookId, ttsNavigator)
    }

    private fun bindSession(
        ttsSession: TtsService.Session
    ): Binding {
        val playbackJob = ttsSession.navigator.playback
            .combine(ttsSession.navigator.utterance) { playback, utterance ->
                stateFromPlayback(playback, utterance)
            }.onEach { state ->
                _state.value = state
                Timber.d("new TTS state ${_state.value}")
            }.launchIn(viewModelScope)

        val preferencesJob = preferencesManager.preferences
            .onEach { ttsSession.navigator.submitPreferences(it) }
            .launchIn(viewModelScope)

        return Binding(playbackJob, preferencesJob)
    }

    private fun stateFromPlayback(
        playback: MediaNavigator.Playback<TtsNavigator.Error>?,
        utterance: SynchronizedMediaNavigator.Utterance<TtsNavigator.Position>?
    ): State {
        if (playback == null || utterance == null)
            return State()

        return State(
            showControls = playback.state != MediaNavigator.State.Ended,
            isPlaying = playback.playWhenReady,
            playingWordRange = utterance.rangeLocator,
            playingUtterance = utterance.locator
        )
    }

    fun stop() {
        viewModelScope.launch {
            binding.await()?.apply {
                playbackJob.cancel()
                submitSettingsJob.cancel()
            }

            ttsServiceFacade.closeSession()

            _state.value = State(
                showControls = false,
                isPlaying = false,
                playingWordRange = null,
                playingUtterance = null,
            )
        }
    }

    fun play() {
        navigatorNow?.play()
    }

    fun pause() {
        navigatorNow?.pause()
    }

    fun previous() {
        navigatorNow?.goBackward()
    }

    fun next() {
        navigatorNow?.goForward()
    }

    fun commit() {
        viewModelScope.launch {
            preferencesManager.setPreferences(editor.value.preferences)
        }
    }
    /**
     * Starts the activity to install additional voice data.
     */
    @OptIn(DelicateReadiumApi::class)
    fun requestInstallVoice(context: Context) {
        // synthesizer.engine.requestInstallMissingVoice(context)
    }

    /*private inner class SynthesizerListener : PublicationSpeechSynthesizer.Listener {
        override fun onUtteranceError(
            utterance: PublicationSpeechSynthesizer.Utterance,
            error: PublicationSpeechSynthesizer.Exception
        ) {
            viewModelScope.launch {
                // The synthesizer is paused when encountering an error while playing an
                // utterance. Here we will skip to the next utterance unless the exception is
                // recoverable.
                val shouldContinuePlayback = !handleTtsException(error)
                if (shouldContinuePlayback) {
                    next()
                }
            }
        }

        override fun onError(error: PublicationSpeechSynthesizer.Exception) {
            viewModelScope.launch {
                handleTtsException(error)
            }
        }

        /**
         * Handles the given error and returns whether it was recovered from.
         */
        private suspend fun handleTtsException(error: PublicationSpeechSynthesizer.Exception): Boolean =
            when (error) {
                is PublicationSpeechSynthesizer.Exception.Engine -> when (val err = error.error) {
                    // The `LanguageSupportIncomplete` exception is a special case. We can recover from
                    // it by asking the user to download the missing voice data.
                    is TtsEngine.Exception.LanguageSupportIncomplete -> {
                        _events.send(Event.OnMissingVoiceData(err.language))
                        true
                    }

                    else -> {
                        _events.send(Event.OnError(err.toUserException()))
                        false
                    }
                }
            }

        private fun TtsEngine.Exception.toUserException(): UserException =
            when (this) {
                is TtsEngine.Exception.InitializationFailed ->
                    UserException(R.string.tts_error_initialization)
                is TtsEngine.Exception.LanguageNotSupported ->
                    UserException(
                        R.string.tts_error_language_not_supported,
                        language.locale.displayName
                    )
                is TtsEngine.Exception.LanguageSupportIncomplete ->
                    UserException(
                        R.string.tts_error_language_support_incomplete,
                        language.locale.displayName
                    )
                is TtsEngine.Exception.Network ->
                    UserException(R.string.tts_error_network)
                is TtsEngine.Exception.Other ->
                    UserException(R.string.tts_error_other)
            }
    }*/
}
