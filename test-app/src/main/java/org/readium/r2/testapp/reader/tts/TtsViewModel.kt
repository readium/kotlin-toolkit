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
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.tts.AndroidTtsEngine
import org.readium.r2.navigator.tts.PublicationSpeechSynthesizer
import org.readium.r2.navigator.tts.PublicationSpeechSynthesizer.Configuration
import org.readium.r2.navigator.tts.TtsEngine
import org.readium.r2.navigator.tts.TtsEngine.Voice
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.navigator.tts.PublicationSpeechSynthesizer.State as TtsState

/**
 * View model controlling a [PublicationSpeechSynthesizer] to read a publication aloud.
 *
 * Note: This is not an Android [ViewModel], but it is a component of [ReaderViewModel].
 */
@OptIn(ExperimentalReadiumApi::class)
class TtsViewModel private constructor(
    private val synthesizer: PublicationSpeechSynthesizer<AndroidTtsEngine>,
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
            PublicationSpeechSynthesizer(context, publication)
                ?.let { TtsViewModel(it, scope) }
    }

    /**
     * @param showControls Whether the TTS was enabled by the user.
     * @param isPlaying Whether the TTS is currently speaking.
     * @param playingWordRange Locator to the currently spoken word.
     * @param playingUtterance Locator for the currently spoken utterance (e.g. sentence).
     * @param settings Current user settings and their constraints.
     */
    data class State(
        val showControls: Boolean = false,
        val isPlaying: Boolean = false,
        val playingWordRange: Locator? = null,
        val playingUtterance: Locator? = null,
        val settings: Settings = Settings()
    )

    /**
     * @param config Currently selected user settings.
     * @param rateRange Supported range for the rate setting.
     * @param availableLanguages Languages supported by the TTS engine.
     * @param availableVoices Voices supported by the TTS engine, for the selected language.
     */
    data class Settings(
        val config: Configuration = Configuration(),
        val rateRange: ClosedRange<Double> = 1.0..1.0,
        val availableLanguages: List<Language> = emptyList(),
        val availableVoices: List<Voice> = emptyList(),
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

    /**
     * Current state of the view model.
     */
    val state: StateFlow<State>

    private val _events: Channel<Event> = Channel(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    /**
     * Indicates whether the TTS is in the Stopped state.
     */
    private val isStopped: StateFlow<Boolean>

    init {
        synthesizer.listener = SynthesizerListener()

        // Automatically close the TTS when reaching the Stopped state.
        isStopped = synthesizer.state
            .map { it == PublicationSpeechSynthesizer.State.Stopped }
            .stateIn(scope, SharingStarted.Lazily, initialValue = true)

        // Supported voices grouped by their language.
        val voicesByLanguage: Flow<Map<Language, List<Voice>>> =
            synthesizer.availableVoices
                .map { voices -> voices.groupBy { it.language } }

        // All supported languages.
        val languages: Flow<List<Language>> = voicesByLanguage
            .map { voices ->
                voices.keys.sortedBy { it.locale.displayName }
            }

        // Supported voices for the language selected in the synthesizer configuration.
        val voicesForSelectedLanguage: Flow<List<Voice>> =
            combine(
                synthesizer.config.map { it.defaultLanguage },
                voicesByLanguage,
            ) { language, voices ->
                language
                    ?.let { voices[it] }
                    ?.sortedBy { it.name ?: it.id }
                    ?: emptyList()
            }

        // Settings model for the current configuration.
        val settings: Flow<Settings> = combine(
            synthesizer.config,
            languages,
            voicesForSelectedLanguage,
        ) { config, langs, voices ->
            Settings(
                config = config,
                rateRange = synthesizer.rateMultiplierRange,
                availableLanguages = langs,
                availableVoices = voices
            )
        }

        // Current view model state.
        state = combine(
            isStopped,
            synthesizer.state,
            settings
        ) { isStopped, state, currentSettings ->
            val playing = (state as? TtsState.Playing)
            val paused = (state as? TtsState.Paused)

            State(
                showControls = !isStopped,
                isPlaying = (playing != null),
                playingWordRange = playing?.range,
                playingUtterance = (playing?.utterance ?: paused?.utterance)?.locator,
                settings = currentSettings
            )
        }.stateIn(scope, SharingStarted.Eagerly, initialValue = State())
    }

    fun onCleared() {
        runBlocking {
            synthesizer.close()
        }
    }

    /**
     * Starts the TTS using the first visible locator in the given [navigator].
     */
    fun start(navigator: Navigator) {
        if (!isStopped.value) return

        scope.launch {
            val start = (navigator as? VisualNavigator)?.firstVisibleElementLocator()
            synthesizer.start(fromLocator = start)
        }
    }

    fun stop() {
        if (isStopped.value) return
        synthesizer.stop()
    }

    fun pauseOrResume() {
        synthesizer.pauseOrResume()
    }

    fun pause() {
        synthesizer.pause()
    }

    fun previous() {
        synthesizer.previous()
    }

    fun next() {
        synthesizer.next()
    }

    fun setConfig(config: Configuration) {
        synthesizer.setConfig(config)
    }

    /**
     * Starts the activity to install additional voice data.
     */
    @OptIn(DelicateReadiumApi::class)
    fun requestInstallVoice(context: Context) {
        synthesizer.engine.requestInstallMissingVoice(context)
    }

    private inner class SynthesizerListener : PublicationSpeechSynthesizer.Listener {
        override fun onUtteranceError(
            utterance: PublicationSpeechSynthesizer.Utterance,
            error: PublicationSpeechSynthesizer.Exception
        ) {
            scope.launch {
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
            scope.launch {
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
    }
}
