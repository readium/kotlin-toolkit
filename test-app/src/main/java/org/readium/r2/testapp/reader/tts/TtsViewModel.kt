/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.navigator.media.tts.android.AndroidTtsSettings
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.testapp.reader.MediaService
import org.readium.r2.testapp.reader.MediaServiceFacade
import org.readium.r2.testapp.reader.ReaderInitData
import org.readium.r2.testapp.reader.VisualReaderInitData
import org.readium.r2.testapp.reader.preferences.PreferencesManager
import org.readium.r2.testapp.reader.preferences.UserPreferencesViewModel
import org.readium.r2.testapp.utils.extensions.mapStateIn
import timber.log.Timber

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
    private val mediaServiceFacade: MediaServiceFacade,
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
                ttsNavigatorFactory = readerInitData.ttsInitData.navigatorFactory,
                mediaServiceFacade = readerInitData.ttsInitData.mediaServiceFacade,
                preferencesManager = readerInitData.ttsInitData.preferencesManager
            )
        }
    }

    sealed class Event {
        /**
         * Emitted when the [TtsNavigator] fails with an error.
         */
        class OnError(val error: TtsError) : Event()

        /**
         * Emitted when the selected language cannot be played because it is missing voice data.
         */
        class OnMissingVoiceData(val language: Language) : Event()
    }

    @Suppress("Unchecked_cast")
    private val MediaService.Session.ttsNavigator
        get() = navigator as? AndroidTtsNavigator

    private val navigatorNow: AndroidTtsNavigator? get() =
        mediaServiceFacade.session.value?.ttsNavigator

    private var launchJob: Job? = null

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
            val baseEditor = ttsNavigatorFactory.createPreferencesEditor(preferences)
            val voices = navigatorNow?.voices.orEmpty()
            TtsPreferencesEditor(baseEditor, voices)
        }

    val showControls: StateFlow<Boolean> =
        mediaServiceFacade.session.mapStateIn(viewModelScope) {
            it != null
        }

    val isPlaying: StateFlow<Boolean> =
        mediaServiceFacade.session.flatMapLatest { session ->
            session?.navigator?.playback?.map { playback -> playback.playWhenReady }
                ?: MutableStateFlow(false)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val position: StateFlow<Locator?> =
        mediaServiceFacade.session.flatMapLatest { session ->
            session?.navigator?.currentLocator ?: MutableStateFlow(null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val highlight: StateFlow<Locator?> =
        mediaServiceFacade.session.flatMapLatest { session ->
            session?.ttsNavigator?.location?.map { it.utteranceLocator }
                ?: MutableStateFlow(null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        mediaServiceFacade.session
            .flatMapLatest { it?.navigator?.playback ?: MutableStateFlow(null) }
            .onEach { playback ->
                when (val state = (playback?.state as? TtsNavigator.State)) {
                    null, TtsNavigator.State.Ready -> {
                        // Do nothing
                    }
                    is TtsNavigator.State.Ended -> {
                        stop()
                    }
                    is TtsNavigator.State.Failure -> {
                        onPlaybackError(state.error)
                    }
                }
            }
            .launchIn(viewModelScope)

        preferencesManager.preferences
            .onEach { navigatorNow?.submitPreferences(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Starts the TTS using the first visible locator in the given [navigator].
     */
    fun start(navigator: Navigator) {
        if (launchJob != null) {
            return
        }

        launchJob = viewModelScope.launch {
            openSession(navigator)
        }
    }

    private suspend fun openSession(navigator: Navigator) {
        val start = (navigator as? VisualNavigator)?.firstVisibleElementLocator()

        val ttsNavigator = ttsNavigatorFactory.createNavigator(
            this,
            initialLocator = start,
            initialPreferences = preferencesManager.preferences.value
        ).getOrElse {
            val error = TtsError.Initialization(it)
            _events.send(Event.OnError(error))
            return
        }

        try {
            mediaServiceFacade.openSession(bookId, ttsNavigator)
        } catch (e: Exception) {
            ttsNavigator.close()
            val error = TtsError.ServiceError(e)
            _events.trySend(Event.OnError(error))
            launchJob = null
            return
        }

        ttsNavigator.play()
    }

    fun stop() {
        launchJob = null
        mediaServiceFacade.closeSession()
    }

    fun play() {
        navigatorNow?.play()
    }

    fun pause() {
        navigatorNow?.pause()
    }

    fun previous() {
        navigatorNow?.skipToPreviousUtterance()
    }

    fun next() {
        navigatorNow?.skipToNextUtterance()
    }

    override fun onStopRequested() {
        stop()
    }

    private fun onPlaybackError(error: TtsNavigator.Error) {
        val event = when (error) {
            is TtsNavigator.Error.ContentError -> {
                Event.OnError(TtsError.ContentError(error))
            }
            is TtsNavigator.Error.EngineError<*> -> {
                val engineError = (error.cause as AndroidTtsEngine.Error)
                when (engineError) {
                    is AndroidTtsEngine.Error.LanguageMissingData ->
                        Event.OnMissingVoiceData(engineError.language)
                    is AndroidTtsEngine.Error.Network -> {
                        val ttsError = TtsError.EngineError.Network(engineError)
                        Event.OnError(ttsError)
                    }
                    else -> {
                        val ttsError = TtsError.EngineError.Other(engineError)
                        Event.OnError(ttsError)
                    }
                }.also { Timber.e("Error type: $error") }
            }
        }

        viewModelScope.launch {
            _events.send(event)
        }
    }
}
