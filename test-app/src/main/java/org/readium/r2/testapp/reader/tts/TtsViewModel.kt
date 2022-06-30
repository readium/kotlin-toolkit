/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import android.content.Context
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.tts.AndroidTtsEngine
import org.readium.r2.navigator.tts.TtsController
import org.readium.r2.navigator.tts.TtsEngine
import org.readium.r2.navigator.tts.TtsEngine.Voice
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.navigator.tts.TtsController.State as TtsState

/**
 * View model driving the text to speech controller.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalDecorator::class)
class TtsViewModel private constructor(
    private val controller: TtsController<AndroidTtsEngine>,
    private val scope: CoroutineScope
) {

    companion object {
        /**
         * Returns an instance of [TtsViewModel] if the given [publication] can be played with the
         * TTS engine.
         */
        operator fun invoke(
            context: Context,
            publication: Publication,
            scope: CoroutineScope
        ): TtsViewModel? =
            TtsController(context, publication)
                ?.let { TtsViewModel(it, scope) }
    }

    /**
     * @param showControls Whether the TTS was enabled by the user.
     * @param isPlaying Whether the TTS is currently speaking.
     * @param playingRange Locator to the currently spoken word.
     * @param playingHighlight Decoration for the currently spoken utterance.
     * @param settings Current user settings and their constraints.
     */
    data class State(
        val showControls: Boolean = false,
        val isPlaying: Boolean = false,
        val playingRange: Locator? = null,
        val playingHighlight: Decoration? = null,
        val settings: Settings = Settings()
    )

    /**
     * @param config Currently selected user settings.
     * @param rateRange Supported range for the rate setting.
     * @param availableLanguages Languages supported by the TTS engine.
     * @param availableVoices Voices supported by the TTS engine, for the selected language.
     */
    data class Settings(
        val config: TtsEngine.Configuration = TtsEngine.Configuration(),
        val rateRange: ClosedRange<Double> = 1.0..1.0,
        val availableLanguages: List<Language> = emptyList(),
        val availableVoices: List<Voice> = emptyList(),
    )

    sealed class Event {
        /**
         * Emitted when the [TtsController] fails with an error.
         */
        class OnError(val error: UserException) : Event()

        /**
         * Emitted when the selected language cannot be played because it is missing voice data.
         */
        class OnMissingVoiceData(val language: Language) : Event()
    }

    /**
     * Current state of the view model.
     */
    val state: StateFlow<State>

    private val _events: Channel<Event> = Channel(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    /**
     * Indicates whether the user enabled the TTS playback.
     * It doesn't mean the TTS is actually speaking utterances at the moment.
     */
    private val isStarted = MutableStateFlow(false)

    init {
        controller.listener = ControllerListener()

        // Handle global [TtsController] failures.
        controller.state
            .filterIsInstance<TtsState.Failure>()
            .map(::error)
            .onEach(::handleTtsException)
            .launchIn(scope)

        val rateRange: Flow<ClosedRange<Double>> =
            controller.configConstraints
                .map { it.rateRange }

        val voicesByLanguage: Flow<Map<Language, List<Voice>>> =
            controller.configConstraints
                .map {
                    it.availableVoices
                        .groupBy { v -> v.language.removeRegion() }
                }

        val languages: Flow<List<Language>> = voicesByLanguage
            .map {
                it.keys
                    .sortedBy { l -> l.locale.displayName }
            }

        val voicesForSelectedLanguage: Flow<List<Voice>> =
            combine(
                controller.config.map { it.defaultLanguage },
                voicesByLanguage,
            ) { language, voices ->
                language
                    ?.let { voices[it.removeRegion()] }
                    ?.sortedBy { it.name ?: it.identifier }
                    ?: emptyList()
            }

        val settings: Flow<Settings> = combine(
            controller.config,
            rateRange,
            languages,
            voicesForSelectedLanguage,
        ) { config, rates, langs, voices ->
            Settings(
                config = config,
                rateRange = rates,
                availableLanguages = langs,
                availableVoices = voices
            )
        }

        state = combine(
            isStarted,
            controller.state,
            settings
        ) { isStarted, state, currentSettings ->
            val playing = (state as? TtsState.Playing)

            State(
                showControls = isStarted,
                isPlaying = (playing != null),
                playingRange = playing?.range,
                playingHighlight = playing?.run {
                    Decoration(
                        id = "tts",
                        locator = utterance.locator,
                        style = Decoration.Style.Highlight(tint = Color.RED)
                    )
                },
                settings = currentSettings
            )
        }.stateIn(scope, SharingStarted.Eagerly, initialValue = State())
    }

    private inner class ControllerListener : TtsController.Listener {
        override fun onUtteranceError(
            utterance: TtsEngine.Utterance,
            error: TtsEngine.Exception
        ) {
            scope.launch {
                val shouldContinuePlayback = handleTtsException(error)

                if (shouldContinuePlayback) {
                    next()
                }
            }

        }
    }

    /**
     * Handles the given error and returns whether the playback should continue.
     */
    private suspend fun handleTtsException(error: TtsEngine.Exception): Boolean =
        when (error) {
            // The `LanguageSupportIncomplete` exception is a special case. We can recover from
            // it by asking the user to download the missing voice data.
            is TtsEngine.Exception.LanguageSupportIncomplete -> {
                _events.send(Event.OnMissingVoiceData(error.language))
                false
            }

            else -> {
                _events.send(Event.OnError(error.toUserException()))
                true
            }
        }

    private fun TtsEngine.Exception.toUserException(): UserException =
        when (this) {
            is TtsEngine.Exception.InitializationFailed ->
                UserException(R.string.tts_error_initialization)
            is TtsEngine.Exception.LanguageNotSupported ->
                UserException(R.string.tts_error_language_not_supported, language.locale.displayName)
            is TtsEngine.Exception.LanguageSupportIncomplete ->
                UserException(R.string.tts_error_language_support_incomplete, language.locale.displayName)
            is TtsEngine.Exception.Network ->
                UserException(R.string.tts_error_network)
            is TtsEngine.Exception.Other ->
                UserException(R.string.tts_error_other)
        }

    fun onCleared() {
        runBlocking {
            controller.close()
        }
    }

    /**
     * Begins the TTS playback in the given [navigator].
     */
    fun start(navigator: Navigator) = scope.launch {
        controller.play(
            start = navigator.firstVisibleElementLocator()
        )
        isStarted.value = true
    }

    fun stop() {
        controller.pause()
        isStarted.value = false
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

    fun setConfig(config: TtsEngine.Configuration) {
        controller.setConfig(config)
    }

    @OptIn(DelicateReadiumApi::class)
    fun requestInstallVoice(context: Context) {
        controller.engine.requestInstallMissingVoice(context)
    }
}
