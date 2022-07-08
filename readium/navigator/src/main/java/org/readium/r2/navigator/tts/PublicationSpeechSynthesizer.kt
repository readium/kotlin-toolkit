/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.*
import org.readium.r2.shared.util.*
import org.readium.r2.shared.util.tokenizer.TextUnit
import java.util.*

/**
 * [PublicationSpeechSynthesizer] orchestrates the rendition of a [publication] by iterating through
 * its content, splitting it into individual utterances using a [ContentTokenizer], then using a
 * [TtsEngine] to read them aloud.
 *
 * Don't forget to call [close] when you are done using the [PublicationSpeechSynthesizer].
 */
@OptIn(DelicateReadiumApi::class)
@ExperimentalReadiumApi
class PublicationSpeechSynthesizer<E : TtsEngine> private constructor(
    private val publication: Publication,
    config: Configuration,
    engineFactory: (listener: TtsEngine.Listener) -> E,
    private val tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer,
    var listener: Listener? = null,
) : SuspendingCloseable {

    companion object {

        /**
         * Creates a [PublicationSpeechSynthesizer] using the default native [AndroidTtsEngine].
         *
         * @param publication Publication which will be iterated through and synthesized.
         * @param config Initial TTS configuration.
         * @param tokenizerFactory Factory to create a [ContentTokenizer] which will be used to
         * split each [Content.Element] item into smaller chunks. Splits by sentences by default.
         * @param listener Optional callbacks listener.
         */
        operator fun invoke(
            context: Context,
            publication: Publication,
            config: Configuration = Configuration(),
            tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer = defaultTokenizerFactory,
            listener: Listener? = null,
        ): PublicationSpeechSynthesizer<AndroidTtsEngine>? = invoke(
            publication,
            config = config,
            engineFactory = { AndroidTtsEngine(context, listener = it) },
            tokenizerFactory = tokenizerFactory,
            listener = listener
        )

        /**
         * Creates a [PublicationSpeechSynthesizer] using a custom [TtsEngine].
         *
         * @param publication Publication which will be iterated through and synthesized.
         * @param config Initial TTS configuration.
         * @param engineFactory Factory to create an instance of [TtsEngine].
         * @param tokenizerFactory Factory to create a [ContentTokenizer] which will be used to
         * split each [Content.Element] item into smaller chunks. Splits by sentences by default.
         * @param listener Optional callbacks listener.
         */
        operator fun <E : TtsEngine> invoke(
            publication: Publication,
            config: Configuration = Configuration(),
            engineFactory: (TtsEngine.Listener) -> E,
            tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer = defaultTokenizerFactory,
            listener: Listener? = null,
        ): PublicationSpeechSynthesizer<E>? {
            if (!canSpeak(publication)) return null

            return PublicationSpeechSynthesizer(publication, config, engineFactory, tokenizerFactory, listener)
        }

        /**
         * The default content tokenizer will split the [Content.Element] items into individual sentences.
         */
        val defaultTokenizerFactory: (Language?) -> ContentTokenizer = { language ->
            TextContentTokenizer(
                unit = TextUnit.Sentence,
                defaultLanguage = language
            )
        }

        /**
         * Returns whether the [publication] can be played with a [PublicationSpeechSynthesizer].
         */
        fun canSpeak(publication: Publication): Boolean =
            publication.content() != null
    }

    @ExperimentalReadiumApi
    interface Listener {
        /** Called when an [error] occurs while speaking [utterance]. */
        fun onUtteranceError(utterance: Utterance, error: Exception)

        /** Called when a global [error] occurs. */
        fun onError(error: Exception)
    }

    @ExperimentalReadiumApi
    sealed class Exception private constructor(
        override val message: String,
        cause: Throwable? = null
    ) : kotlin.Exception(message, cause) {

        /** Underlying [TtsEngine] error. */
        class Engine(val error: TtsEngine.Exception)
            : Exception(error.message, error)
    }

    /**
     * An utterance is an arbitrary text (e.g. sentence) extracted from the [publication], that can
     * be synthesized by the TTS [engine].
     *
     * @param text Text to be spoken.
     * @param locator Locator to the utterance in the [publication].
     * @param language Language of this utterance, if it differs from the default publication
     * language.
     */
    @ExperimentalReadiumApi
    data class Utterance(
        val text: String,
        val locator: Locator,
        val language: Language?,
    )

    /**
     * Represents a state of the [PublicationSpeechSynthesizer].
     */
    sealed class State {
        /** The [PublicationSpeechSynthesizer] is completely stopped and must be (re)started from a given locator. */
        object Stopped : State()

        /** The [PublicationSpeechSynthesizer] is paused at the given utterance. */
        data class Paused(val utterance: Utterance) : State()

        /**
         * The TTS engine is synthesizing [utterance].
         *
         * [range] will be regularly updated while the [utterance] is being played.
         */
        data class Playing(val utterance: Utterance, val range: Locator? = null) : State()
    }

    private val _state = MutableStateFlow<State>(State.Stopped)

    /**
     * Current state of the [PublicationSpeechSynthesizer].
     */
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = MainScope()

    init {
        require(canSpeak(publication)) {
            "The content of the publication cannot be synthesized, as it is not iterable"
        }
    }

    /**
     * Underlying [TtsEngine] instance.
     *
     * WARNING: Don't control the playback or set the config directly with the engine. Use the
     * [PublicationSpeechSynthesizer] APIs instead. This property is used to access engine-specific APIs such as
     * [AndroidTtsEngine.requestInstallMissingVoice].
     */
    @DelicateReadiumApi
    val engine: E by lazy {
        engineFactory(object : TtsEngine.Listener {
            override fun onEngineError(error: TtsEngine.Exception) {
                listener?.onError(Exception.Engine(error))
                stop()
            }

            override fun onAvailableVoicesChange(voices: List<TtsEngine.Voice>) {
                _availableVoices.value = voices
            }
        })
    }

    /**
     * Interrupts the [TtsEngine] and closes this [PublicationSpeechSynthesizer].
     */
    override suspend fun close() {
        tryOrLog {
            scope.cancel()
            if (::engine.isLazyInitialized) {
                engine.close()
            }
        }
    }

    /**
     * User configuration for the text-to-speech engine.
     *
     * @param defaultLanguage Language overriding the publication one.
     * @param voiceId Identifier for the voice used to speak the utterances.
     * @param rateMultiplier Multiplier for the voice speech rate. Normal is 1.0. See [rateMultiplierRange]
     * for the range of values supported by the [TtsEngine].
     * @param extras Extensibility for custom TTS engines.
     */
    @ExperimentalReadiumApi
    data class Configuration(
        val defaultLanguage: Language? = null,
        val voiceId: String? = null,
        val rateMultiplier: Double = 1.0,
        val extras: Any? = null
    )

    private val _config = MutableStateFlow(config)

    /**
     * Current user configuration.
     */
    val config: StateFlow<Configuration> = _config.asStateFlow()

    /**
     * Updates the user configuration.
     *
     * The change is not immediate, it will be applied for the next utterance.
     */
    fun setConfig(config: Configuration) {
        _config.value = config.copy(
            rateMultiplier = config.rateMultiplier.coerceIn(engine.rateMultiplierRange),
        )
    }

    /**
     * Range for the speech rate multiplier. Normal is 1.0.
     */
    val rateMultiplierRange: ClosedRange<Double>
        get() = engine.rateMultiplierRange

    private val _availableVoices = MutableStateFlow<List<TtsEngine.Voice>>(emptyList())

    /**
     * List of synthesizer voices supported by the TTS [engine].
     */
    val availableVoices: StateFlow<List<TtsEngine.Voice>> = _availableVoices.asStateFlow()

    /**
     * Returns the first voice with the given [id] supported by the TTS [engine].
     *
     * This can be used to restore the user selected voice after storing it in the shared
     * preferences.
     */
    fun voiceWithId(id: String): TtsEngine.Voice? {
        val voice = lastUsedVoice?.takeIf { it.id == id }
            ?: engine.voiceWithId(id)
            ?: return null

        lastUsedVoice = voice
        return voice
    }

    /**
     * Cache for the last requested voice, for performance.
     */
    private var lastUsedVoice: TtsEngine.Voice? = null

    /**
     * (Re)starts the TTS from the given locator or the beginning of the publication.
     */
    fun start(fromLocator: Locator? = null) {
        replacePlaybackJob {
            publicationIterator = publication.content(fromLocator)?.iterator()
            playNextUtterance(Direction.Forward)
        }
    }

    /**
     * Stops the synthesizer.
     *
     * Use [start] to restart it.
     */
    fun stop() {
        replacePlaybackJob {
            _state.value = State.Stopped
            publicationIterator = null
        }
    }

    /**
     * Interrupts a played utterance.
     *
     * Use [resume] to restart the playback from the same utterance.
     */
    fun pause() {
        replacePlaybackJob {
            _state.update { state ->
                when (state) {
                    is State.Playing -> State.Paused(state.utterance)
                    else -> state
                }
            }
        }
    }

    /**
     * Resumes an utterance interrupted with [pause].
     */
    fun resume() {
        replacePlaybackJob {
            (state.value as? State.Paused)?.let { paused ->
                play(paused.utterance)
            }
        }
    }

    /**
     * Pauses or resumes the playback of the current utterance.
     */
    fun pauseOrResume() {
        when (state.value) {
            is State.Stopped -> return
            is State.Playing -> pause()
            is State.Paused -> resume()
        }
    }

    /**
     * Skips to the previous utterance.
     */
    fun previous() {
        replacePlaybackJob {
            playNextUtterance(Direction.Backward)
        }
    }

    /**
     * Skips to the next utterance.
     */
    fun next() {
        replacePlaybackJob {
            playNextUtterance(Direction.Forward)
        }
    }

    /**
     * [Content.Iterator] used to iterate through the [publication].
     */
    private var publicationIterator: Content.Iterator? = null
        set(value) {
            field = value
            utterances = CursorList()
        }

    /**
     * Utterances for the current publication [Content.Element] item.
     */
    private var utterances: CursorList<Utterance> = CursorList()

    /**
     * Plays the next utterance in the given [direction].
     */
    private suspend fun playNextUtterance(direction: Direction) {
        val utterance = nextUtterance(direction)
        if (utterance == null) {
            _state.value = State.Stopped
            return
        }
        play(utterance)
    }

    /**
     * Plays the given [utterance] with the TTS [engine].
     */
    private suspend fun play(utterance: Utterance) {
        _state.value = State.Playing(utterance)

        engine
            .speak(
                utterance = TtsEngine.Utterance(
                    text = utterance.text,
                    rateMultiplier = config.value.rateMultiplier,
                    voiceOrLanguage = utterance.voiceOrLanguage()
                ),
                onSpeakRange = { range ->
                    _state.value = State.Playing(
                        utterance = utterance,
                        range = utterance.locator.copy(
                            text = utterance.locator.text.substring(range)
                        )
                    )
                }
            )
            .onSuccess {
                playNextUtterance(Direction.Forward)
            }
            .onFailure {
                _state.value = State.Paused(utterance)
                listener?.onUtteranceError(utterance, Exception.Engine(it))
            }
    }

    /**
     * Returns the user selected voice if it's compatible with the utterance language. Otherwise,
     * falls back on the languages.
     */
    private fun Utterance.voiceOrLanguage(): Either<TtsEngine.Voice, Language> {
        // User selected voice, if it's compatible with the utterance language.
        // Or fallback on the languages.
        val voice = config.value.voiceId
            ?.let { voiceWithId(it) }
            ?.takeIf { language == null || it.language.removeRegion() == language.removeRegion() }

        return (
            if (voice != null) Either.Left(voice)
            else Either.Right(language
                ?: config.value.defaultLanguage
                ?: publication.metadata.language
                ?: Language(Locale.getDefault())
            )
        )
    }

    /**
     * Gets the next utterance in the given [direction], or null when reaching the beginning or the
     * end.
     */
    private suspend fun nextUtterance(direction: Direction): Utterance? {
        val utterance = utterances.nextIn(direction)
        if (utterance == null && loadNextUtterances(direction)) {
            return nextUtterance(direction)
        }
        return utterance
    }

    /**
     * Loads the utterances for the next publication [Content.Element] item in the given [direction].
     */
    private suspend fun loadNextUtterances(direction: Direction): Boolean {
        val content = publicationIterator?.nextIn(direction)
            ?: return false

        val nextUtterances = content
            .tokenize()
            .flatMap { it.utterances() }

        if (nextUtterances.isEmpty()) {
            return loadNextUtterances(direction)
        }

        utterances = CursorList(
            list = nextUtterances,
            startIndex = when (direction) {
                Direction.Forward -> 0
                Direction.Backward -> nextUtterances.size - 1
            }
        )

        return true
    }

    /**
     * Splits a publication [Content.Element] item into smaller chunks using the provided tokenizer.
     *
     * This is used to split a paragraph into sentences, for example.
     */
    private fun Content.Element.tokenize(): List<Content.Element> =
        tokenizerFactory(config.value.defaultLanguage ?: publication.metadata.language)
            .tokenize(this)

    /**
     * Splits a publication [Content.Element] item into the utterances to be spoken.
     */
    private fun Content.Element.utterances(): List<Utterance> {
        fun utterance(text: String, locator: Locator, language: Language? = null): Utterance? {
            if (!text.any { it.isLetterOrDigit() })
                return null

            return Utterance(
                text = text,
                locator = locator,
                language = language
                    // If the language is the same as the one declared globally in the publication,
                    // we omit it. This way, the app can customize the default language used in the
                    // configuration.
                    ?.takeIf { it != publication.metadata.language }
            )
        }

        return when (this) {
            is Content.TextElement -> {
                segments.mapNotNull { segment ->
                    utterance(
                        text = segment.text,
                        locator = segment.locator,
                        language = segment.language
                    )
                }
            }

            is Content.TextualElement -> {
                listOfNotNull(
                    text
                        ?.takeIf { it.isNotBlank() }
                        ?.let { utterance(text = it, locator = locator) }
                )
            }

            else -> emptyList()
        }
    }

    /**
     * Cancels the previous playback-related job and starts a new one with the given suspending
     * [block].
     *
     * This is used to interrupt on-going commands.
     */
    private fun replacePlaybackJob(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            playbackJob?.cancelAndJoin()
            playbackJob = launch {
                block()
            }
        }
    }

    private var playbackJob: Job? = null

    private enum class Direction {
        Forward, Backward;
    }

    private fun <E> CursorList<E>.nextIn(direction: Direction): E? =
        when (direction) {
            Direction.Forward -> next()
            Direction.Backward -> previous()
        }

    private suspend fun Content.Iterator.nextIn(direction: Direction): Content.Element? =
        when (direction) {
            Direction.Forward -> nextOrNull()
            Direction.Backward -> previousOrNull()
        }
}