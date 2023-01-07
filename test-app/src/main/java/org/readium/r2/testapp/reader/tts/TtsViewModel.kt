/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferences
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferencesEditor
import org.readium.r2.navigator.media3.androidtts.AndroidTtsSettings
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.tts2.TtsNavigator
import org.readium.r2.navigator.media3.tts2.TtsNavigatorFactory
import org.readium.r2.navigator.media3.tts2.TtsNavigatorListener
import org.readium.r2.navigator.tts.PublicationSpeechSynthesizer
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.reader.ReaderInitData
import org.readium.r2.testapp.reader.VisualReaderInitData
import org.readium.r2.testapp.reader.preferences.PreferencesManager
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
    private val ttsNavigatorFactory: TtsNavigatorFactory<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsPreferencesEditor>,
    private val ttsSessionBinder: TtsService.Binder,
    private val preferencesManager: PreferencesManager<AndroidTtsPreferences>,
    val preferencesEditor: AndroidTtsPreferencesEditor
) {

    companion object {
        /**
         * Returns an instance of [TtsViewModel] if the given [publication] can be played with the
         * TTS engine.
         */
        operator fun invoke(
            viewModelScope: CoroutineScope,
            readerInitData: ReaderInitData
        ): TtsViewModel? {
            if (readerInitData !is VisualReaderInitData || readerInitData.ttsInitData == null) {
                return null
            }

            val preferencesEditor =
                readerInitData.ttsInitData.ttsNavigatorFactory.createTtsPreferencesEditor(
                    readerInitData.ttsInitData.preferencesManager.preferences.value
                )

            return TtsViewModel(
                viewModelScope = viewModelScope,
                bookId = readerInitData.bookId,
                publication = readerInitData.publication,
                ttsNavigatorFactory = readerInitData.ttsInitData.ttsNavigatorFactory,
                ttsSessionBinder = readerInitData.ttsInitData.sessionBinder,
                preferencesManager = readerInitData.ttsInitData.preferencesManager,
                preferencesEditor = preferencesEditor
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

    private val ttsNavigator: TtsNavigator<AndroidTtsSettings, AndroidTtsPreferences>? get() =
        ttsSessionBinder.mediaNavigator

    /**
     * Current state of the view model.
     */
    private val _state: MutableStateFlow<State> = MutableStateFlow(
        stateFromPlayback(ttsNavigator?.playback?.value)
    )

    init {
        ttsNavigator?.let { bindStateToNavigator(it) }
    }
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events: Channel<Event> = Channel(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    /**
     * Starts the TTS using the first visible locator in the given [navigator].
     */
    fun start(navigator: Navigator) {
        if (ttsNavigator != null) return

        viewModelScope.launch {
            val ttsNavigator = createTtsNavigator(navigator)
            bindStateToNavigator(ttsNavigator)
            ttsNavigator.play()
        }
    }

    private suspend fun createTtsNavigator(navigator: Navigator): TtsNavigator<AndroidTtsSettings, AndroidTtsPreferences> {
        val start = (navigator as? VisualNavigator)?.firstVisibleElementLocator()

        val listener = object : TtsNavigatorListener {

            override fun onStopRequested() {
                stop()
            }

            override fun onPlaybackException() {
                TODO("Not yet implemented")
            }
        }

        val ttsNavigator = ttsNavigatorFactory.createNavigator(
            listener,
            preferencesManager.preferences.value,
            start
        )

        ttsSessionBinder.bindNavigator(ttsNavigator, bookId)
        return ttsNavigator
    }

    private fun bindStateToNavigator(
        ttsNavigator: TtsNavigator<AndroidTtsSettings, AndroidTtsPreferences>
    ) {
        ttsNavigator.playback
            .onEach { playback ->
                Timber.d("new TTS playback $playback")
                _state.value = stateFromPlayback(playback)
                Timber.d("new TTS state ${_state.value}")
            }.launchIn(viewModelScope)
    }

    private fun stateFromPlayback(playback: TtsNavigator.Playback?): State {
        if (playback == null)
            return State()

        return State(
            showControls = playback.state != MediaNavigator.State.Ended,
            isPlaying = playback.state == MediaNavigator.State.Playing,
            playingWordRange = playback.token,
            playingUtterance = playback.locator
        )
    }

    fun stop() {
        if (ttsNavigator == null) return

        _state.value = State(
            showControls = false,
            isPlaying = false,
            playingWordRange = null,
            playingUtterance = null,
        )
        ttsSessionBinder.closeNavigator()
    }

    fun play() {
        ttsNavigator?.play()
    }

    fun pause() {
        ttsNavigator?.pause()
    }

    fun previous() {
        ttsNavigator?.goBackward()
    }

    fun next() {
        ttsNavigator?.goForward()
    }

    fun commitPreferences() {
        viewModelScope.launch {
            preferencesManager.setPreferences(preferencesEditor.preferences)
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
