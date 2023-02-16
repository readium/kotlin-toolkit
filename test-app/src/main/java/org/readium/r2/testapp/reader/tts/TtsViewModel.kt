/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.tts.AndroidTtsNavigator
import org.readium.r2.navigator.media3.tts.AndroidTtsNavigatorFactory
import org.readium.r2.navigator.media3.tts.TtsNavigator
import org.readium.r2.navigator.media3.tts.android.AndroidTtsEngine
import org.readium.r2.navigator.media3.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.media3.tts.android.AndroidTtsPreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.ReaderInitData
import org.readium.r2.testapp.reader.VisualReaderInitData
import org.readium.r2.testapp.reader.preferences.PreferencesManager
import org.readium.r2.testapp.utils.extensions.mapStateIn

/**
 * View model controlling a [TtsNavigator] to read a publication aloud.
 *
 * Note: This is not an Android ViewModel, but it is a component of ReaderViewModel.
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
) : TtsNavigator.Listener {

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

    sealed class Event {
        /**
         * Emitted when the [TtsNavigator] fails with an error.
         */
        class OnError(val error: UserException) : Event()

        /**
         * Emitted when the selected language cannot be played because it is missing voice data.
         */
        class OnMissingVoiceData(val language: Language) : Event()
    }

    private var binding: Deferred<Job?> =
        viewModelScope.async {
            ttsServiceFacade.getSession()
                ?.let { bindSession(it) }
        }

    private val _showControls: MutableStateFlow<Boolean> =
        MutableStateFlow(navigatorNow != null)

    private val _isPlaying: MutableStateFlow<Boolean> =
        MutableStateFlow(navigatorNow?.playback?.value?.playWhenReady ?: false)

    private val _position: MutableStateFlow<Locator?> =
        MutableStateFlow(navigatorNow?.currentLocator?.value)

    private val _highlight: MutableStateFlow<Locator?> =
        MutableStateFlow(navigatorNow?.utterance?.value?.utteranceLocator)

    private val _events: Channel<Event> =
        Channel(Channel.BUFFERED)

    val editor: StateFlow<AndroidTtsPreferencesEditor> = preferencesManager.preferences
        .mapStateIn(viewModelScope, createPreferencesEditor)

    val showControls: StateFlow<Boolean> =
        _showControls.asStateFlow()

    val isPlaying: StateFlow<Boolean> =
        _isPlaying.asStateFlow()

    val position: StateFlow<Locator?> =
        _position.asStateFlow()

    val highlight: StateFlow<Locator?> =
        _highlight.asStateFlow()

    val events: Flow<Event> =
        _events.receiveAsFlow()

    private val navigatorNow: AndroidTtsNavigator? get() =
        ttsServiceFacade.sessionNow()?.navigator

    val voices: Set<AndroidTtsEngine.Voice> get() =
        navigatorNow!!.voices

    /**
     * Starts the TTS using the first visible locator in the given [navigator].
     */
    fun start(navigator: Navigator) {
        viewModelScope.launch {
            if (ttsServiceFacade.getSession() != null)
                return@launch

            val session = openSession(navigator)
                ?: run {
                    val exception = UserException(R.string.tts_error_initialization)
                    _events.send(Event.OnError(exception))
                    return@launch
                }

            binding.cancelAndJoin()
            binding = async { bindSession(session) }
        }
    }

    private suspend fun openSession(navigator: Navigator): TtsService.Session? {
        val start = (navigator as? VisualNavigator)?.firstVisibleElementLocator()

        val ttsNavigator = ttsNavigatorFactory.createNavigator(
            this,
            preferencesManager.preferences.value,
            start
        ) ?: return null

        // playWhenReady must be true for the MediaSessionService to call Service.startForeground
        // and prevent crashing
        ttsNavigator.play()
        return ttsServiceFacade.openSession(bookId, ttsNavigator)
    }

    private fun bindSession(
        ttsSession: TtsService.Session
    ): Job {
        val job = Job()
        val scope = viewModelScope + job

        _showControls.value = true

        ttsSession.navigator.playback
            .onEach { playback ->
                _isPlaying.value = playback.playWhenReady
                when (playback.state) {
                    is MediaNavigator.State.Ended -> {
                        stop()
                    }
                    is MediaNavigator.State.Error -> {
                        onPlaybackError(playback.state as TtsNavigator.State.Error)
                    }
                    is MediaNavigator.State.Ready -> {}
                    is MediaNavigator.State.Buffering -> {}
                }
            }.launchIn(scope)

        ttsSession.navigator.utterance
            .onEach { utterance ->
                _highlight.value = utterance.utteranceLocator
            }.launchIn(scope)

        preferencesManager.preferences
            .onEach { ttsSession.navigator.submitPreferences(it) }
            .launchIn(scope)

        ttsSession.navigator.currentLocator
            .onEach { _position.value = it }
            .launchIn(scope)

        return job
    }

    fun stop() {
        viewModelScope.launch {
            binding.cancelAndJoin()
            _highlight.value = null
            _showControls.value = false
            _isPlaying.value = false
            ttsServiceFacade.closeSession()
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

    override fun onStopRequested() {
        stop()
    }

    override fun onMissingLanguageData(language: Language) {
        viewModelScope.launch {
            _events.send(Event.OnMissingVoiceData(language))
        }
    }

    private fun onPlaybackError(error: TtsNavigator.State.Error) {
        val exception = when (error) {
            is TtsNavigator.State.Error.ContentError -> {
                UserException(R.string.tts_error_other, cause = error.exception)
            }
            is TtsNavigator.State.Error.EngineError<*> -> {
                when ((error.error as AndroidTtsEngine.Error).kind) {
                    AndroidTtsEngine.Error.Kind.Network ->
                        UserException(R.string.tts_error_network)
                    else ->
                        UserException(R.string.tts_error_other)
                }
            }
        }

        viewModelScope.launch {
            _events.send(Event.OnError(exception))
        }
    }
}
