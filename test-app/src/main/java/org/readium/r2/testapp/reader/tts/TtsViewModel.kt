/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.tts.AndroidTtsEngine
import org.readium.r2.navigator.tts.TtsController
import org.readium.r2.navigator.tts.TtsEngine
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.createViewModelFactory
import java.util.*
import org.readium.r2.navigator.tts.TtsController.State as TtsState

@OptIn(ExperimentalReadiumApi::class, ExperimentalDecorator::class)
class TtsViewModel(
    application: Application,
    publication: Publication
) : AndroidViewModel(application) {

    data class State(
        val showControls: Boolean = false,
        val isPlaying: Boolean = false,
        val playingRange: Locator? = null,
        val config: TtsEngine.Configuration = TtsEngine.Configuration(),
        val decorations: List<Decoration> = emptyList(),
    )

    sealed class Event {
        class OnError(val error: UserException) : Event()
        class OnMissingVoiceData(val locale: Locale) : Event()
    }

    val isAvailable: Boolean
        get() = ::controller.isInitialized

    val state: StateFlow<State>

    private val _events: Channel<Event> = Channel(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val isEnabled = MutableStateFlow(false)
    private lateinit var controller: TtsController<AndroidTtsEngine>

    init {
        val tts = TtsController(application, publication)
        if (tts == null) {
            state = MutableStateFlow(State())

        } else {
            controller = tts

            tts.listener = object : TtsController.Listener {
                override fun onUtteranceError(utterance: TtsEngine.Utterance, error: TtsEngine.Exception) {
                    handleTtsException(error)

                    // When the voice data is incomplete, the user will be requested to install it.
                    // For other errors, we jump to the next utterance.
                    if (error !is TtsEngine.Exception.LanguageSupportIncomplete) {
                        next()
                    }
                }
            }

            tts.state
                .filterIsInstance<TtsState.Failure>()
                .map(::error)
                .onEach(::handleTtsException)
                .launchIn(viewModelScope)

            state = combine(
                isEnabled,
                controller.state,
                controller.config
            ) { isEnabled, state, config ->
                val playing = (state as? TtsState.Playing)

                State(
                    showControls = isEnabled,
                    isPlaying = (playing != null),
                    playingRange = playing?.range,
                    config = config,
                    decorations = listOfNotNull(playing?.run {
                        Decoration(
                            id = "tts",
                            locator = utterance.locator,
                            style = Decoration.Style.Highlight(tint = Color.RED)
                        )
                    })
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = State())
        }
    }

    private fun handleTtsException(error: TtsEngine.Exception) = viewModelScope.launch {
        if (error is TtsEngine.Exception.LanguageSupportIncomplete) {
            _events.send(Event.OnMissingVoiceData(error.locale))
        } else {
            _events.send(Event.OnError(error.toUserException()))
        }
    }

    private fun TtsEngine.Exception.toUserException(): UserException =
        when (this) {
            is TtsEngine.Exception.InitializationFailed ->
                UserException(R.string.tts_error_initialization)
            is TtsEngine.Exception.LanguageNotSupported ->
                UserException(R.string.tts_error_language_not_supported, locale.displayLanguage)
            is TtsEngine.Exception.LanguageSupportIncomplete ->
                UserException(R.string.tts_error_language_support_incomplete, locale.displayLanguage)
            is TtsEngine.Exception.Network ->
                UserException(R.string.tts_error_network)
            is TtsEngine.Exception.Other ->
                UserException(R.string.tts_error_other)
        }

    override fun onCleared() {
        super.onCleared()

        if (isAvailable) {
            runBlocking {
                controller.close()
            }
        }
    }

//    val ttsAvailableLocales: StateFlow<Set<Locale>>? get() = tts?.availableLocales
//
//    val ttsAvailableVoices: StateFlow<Set<TtsEngine.Voice>>? get() = tts?.run {
//        combine(config, availableVoices) { config, voices ->
//            voices
//                .filter { it.locale.language == config.defaultLocale?.language }
//                .toSet()
//        }
//    }

    fun setConfig(config: TtsEngine.Configuration) {
        controller.setConfig(config)
    }

    @OptIn(InternalReadiumApi::class) // FIXME
    fun play(navigator: Navigator) = viewModelScope.launch {
        controller.play(
            start = (navigator as? EpubNavigatorFragment)?.firstVisibleElementLocator()
                ?: navigator.currentLocator.value
        )

        isEnabled.value = true
    }

    fun playPause() {
        controller.playPause()
    }

    fun pause() {
        controller.pause()
    }

    fun previous() {
        controller.previous()
    }

    fun next() {
        controller.next()
    }

    fun stop() {
        controller.pause()
        isEnabled.value = false
    }

    @OptIn(DelicateReadiumApi::class)
    fun requestInstallVoice(context: Context) {
        controller.engine.requestInstallMissingVoice(context)
    }

    companion object {
        fun createFactory(application: Application, publication: Publication) =
            createViewModelFactory {
                TtsViewModel(application, publication)
            }
    }
}
