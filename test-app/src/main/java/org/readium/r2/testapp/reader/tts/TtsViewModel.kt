/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.tts.AndroidTtsNavigator
import org.readium.r2.navigator.media3.tts.AndroidTtsNavigatorFactory
import org.readium.r2.navigator.media3.tts.TtsNavigator
import org.readium.r2.navigator.media3.tts.android.AndroidTtsEngine
import org.readium.r2.navigator.media3.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.media3.tts.android.AndroidTtsSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.ReaderInitData
import org.readium.r2.testapp.reader.VisualReaderInitData
import org.readium.r2.testapp.reader.preferences.PreferencesManager
import org.readium.r2.testapp.reader.preferences.UserPreferencesViewModel
import org.readium.r2.testapp.utils.extensions.mapStateIn

/**
 * View model controlling a [TtsNavigator] to read a publication aloud.
 *
 * Note: This is not an Android ViewModel, but it is a component of ReaderViewModel.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalCoroutinesApi::class)
class TtsViewModel private constructor(
    private val viewModelScope: CoroutineScope,
    private val bookId: Long,
    private val publication: Publication,
    private val ttsNavigatorFactory: AndroidTtsNavigatorFactory,
    private val ttsServiceFacade: TtsServiceFacade,
    private val preferencesManager: PreferencesManager<AndroidTtsPreferences>,
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
                preferencesManager = readerInitData.ttsInitData.preferencesManager
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

    private val navigatorNow: AndroidTtsNavigator? get() =
        ttsServiceFacade.session.value?.navigator

    private val _events: Channel<Event> =
        Channel(Channel.BUFFERED)

    val events: Flow<Event> =
        _events.receiveAsFlow()

    val preferencesModel: UserPreferencesViewModel<AndroidTtsSettings, AndroidTtsPreferences>
        get() = UserPreferencesViewModel(
            viewModelScope = viewModelScope,
            bookId = bookId,
            preferencesManager = preferencesManager
        ) { preferences ->
            val baseEditor = ttsNavigatorFactory.createTtsPreferencesEditor(preferences)
            TtsPreferencesEditor(baseEditor, voices)
        }

    val showControls: StateFlow<Boolean> =
        ttsServiceFacade.session.mapStateIn(viewModelScope) {
            it != null
        }

    val isPlaying: StateFlow<Boolean> =
        ttsServiceFacade.session.flatMapLatest { session ->
            session?.navigator?.playback?.map { playback -> playback.playWhenReady }
                ?: MutableStateFlow(false)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val position: StateFlow<Locator?> =
        ttsServiceFacade.session.flatMapLatest { session ->
            session?.navigator?.currentLocator ?: MutableStateFlow(null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val highlight: StateFlow<Locator?> =
        ttsServiceFacade.session.flatMapLatest { session ->
            session?.navigator?.utterance?.map { it.utteranceLocator }
                ?: MutableStateFlow(null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val voices: Set<AndroidTtsEngine.Voice> get() =
        ttsServiceFacade.session.value?.navigator?.voices.orEmpty()

    init {
        ttsServiceFacade.session
            .flatMapLatest { it?.navigator?.playback ?: MutableStateFlow(null) }
            .onEach { playback ->
                when (playback?.state) {
                    null -> {
                    }
                    is MediaNavigator.State.Ended -> {
                        stop()
                    }
                    is MediaNavigator.State.Error -> {
                        onPlaybackError(playback.state as TtsNavigator.State.Error)
                    }
                    is MediaNavigator.State.Ready -> {}
                    is MediaNavigator.State.Buffering -> {}
                }
            }.launchIn(viewModelScope)

        preferencesManager.preferences
            .onEach { ttsServiceFacade.session.value?.navigator?.submitPreferences(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the TTS using the first visible locator in the given [navigator].
     */
    fun start(navigator: Navigator) {
        viewModelScope.launch {
            if (ttsServiceFacade.session.value != null)
                return@launch

            openSession(navigator)
        }
    }

    private suspend fun openSession(navigator: Navigator) {
        val start = (navigator as? VisualNavigator)?.firstVisibleElementLocator()

        val ttsNavigator = ttsNavigatorFactory.createNavigator(
            this,
            preferencesManager.preferences.value,
            start
        ) ?: run {
            val exception = UserException(R.string.tts_error_initialization)
            _events.send(Event.OnError(exception))
            return
        }

        // playWhenReady must be true for the MediaSessionService to call Service.startForeground
        // and prevent crashing
        ttsNavigator.play()
        ttsServiceFacade.openSession(bookId, ttsNavigator)
    }

    fun stop() {
        viewModelScope.launch {
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

    override fun onStopRequested() {
        stop()
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
