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
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.ContentIterator
import org.readium.r2.shared.publication.services.content.contentIterator
import org.readium.r2.shared.publication.services.content.isContentIterable
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.tokenizer.ContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextUnit
import java.util.*

@ExperimentalReadiumApi
fun interface TtsEngineFactory<E : TtsEngine> {
    fun create(listener: TtsEngine.Listener): E
}

@ExperimentalReadiumApi
fun interface TtsTokenizerFactory {
    fun create(defaultLanguage: Language?): ContentTokenizer
}

class CursorList<E>(
    private val list: List<E> = emptyList(),
    private val startIndex: Int = 0
) : List<E> by list {
    private var index: Int? = null

    fun current(): E? =
        moveAndGet(index ?: startIndex)

    fun previous(): E? =
        moveAndGet(index
            ?.let { it - 1}
            ?: startIndex
        )

    fun next(): E? =
        moveAndGet(index?.let { it + 1}
            ?: startIndex
        )

    private fun moveAndGet(index: Int): E? {
        if (!list.indices.contains(index)) {
            return null
        }
        this.index = index
        return get(index)
    }
}

@OptIn(DelicateReadiumApi::class)
@ExperimentalReadiumApi
class TtsController<E : TtsEngine> private constructor(
    private val publication: Publication,
    config: Configuration,
    engineFactory: TtsEngineFactory<E>,
    private val tokenizerFactory: TtsTokenizerFactory,
    var listener: Listener? = null,
) : SuspendingCloseable {

    @ExperimentalReadiumApi
    sealed class Exception private constructor(
        override val message: String,
        cause: Throwable? = null
    ) : kotlin.Exception(message, cause) {
        class Engine(val error: TtsEngine.Exception)
            : Exception(error.message, error)
    }

    data class Configuration(
        val defaultLanguage: Language? = null,
        val voice: TtsEngine.Voice? = null,
        val rate: Double = 1.0,
    )

    data class Utterance(
        val text: String,
        val locator: Locator,

        internal val language: Language?,
        internal val id: String,
    )

    interface Listener {
        /**
         * Notifies an [error] occurred while speaking [utterance].
         */
        fun onUtteranceError(utterance: Utterance, error: Exception)

        /**
         * Notifies a global [error] occurred.
         */
        fun onError(error: Exception)
    }

    companion object {
        val defaultTokenizerFactory: TtsTokenizerFactory = TtsTokenizerFactory { language -> TextContentTokenizer(unit = TextUnit.Sentence, defaultLanguage = language) }

        operator fun invoke(
            context: Context,
            publication: Publication,
            config: Configuration = Configuration(),
            tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory
        ): TtsController<AndroidTtsEngine>? = invoke(
            publication,
            config = config,
            engineFactory = { listener -> AndroidTtsEngine(context, listener) },
            tokenizerFactory = tokenizerFactory
        )

        operator fun <E : TtsEngine> invoke(
            publication: Publication,
            config: Configuration = Configuration(),
            engineFactory: TtsEngineFactory<E>,
            tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory
        ): TtsController<E>? {
            if (!canSpeak(publication)) return null

            return TtsController(publication, config, engineFactory, tokenizerFactory)
        }

        fun canSpeak(publication: Publication): Boolean =
            publication.isContentIterable
    }

    sealed class State {
        object Stopped : State()
        object Paused : State()
        class Playing(val utterance: Utterance, val range: Locator? = null) : State()
        class Failure(val error: Exception) : State()
    }

    private val _state = MutableStateFlow<State>(State.Stopped)
    val state: StateFlow<State> = _state.asStateFlow()

    var publicationIterator: ContentIterator? = null
        set(value) {
            field?.let {
                scope.launch { it.close() }
            }
            field = value
            utterances = CursorList()
        }

    var utterances: CursorList<Utterance> = CursorList()

    /**
     * Underlying [TtsEngine] instance.
     *
     * WARNING: Don't control the playback or set the config directly with the engine. Use the
     * [TtsController] APIs instead. This property is used to access engine-specific APIs such as
     * [AndroidTtsEngine.requestInstallMissingVoice],
     */
    @DelicateReadiumApi
    val engine: E by lazy { engineFactory.create(EngineListener()) }

    private val scope = MainScope()
    private val mutex = Mutex()

    init {
        require(canSpeak(publication)) { "The publication cannot be spoken with TtsController, as its content is not iterable" }
    }

    override suspend fun close() {
        tryOrLog {
            engine.close()
            scope.cancel()
        }
    }

    private val _config = MutableStateFlow(config)
    val config: StateFlow<Configuration> = _config.asStateFlow()

    fun setConfig(config: Configuration): Configuration =
        _config.updateAndGet {
            config.copy(
                rate = config.rate.coerceIn(engine.rateRange),
            )
        }

    val rateRange: ClosedRange<Double>
        get() = engine.rateRange

    private val _availableVoices = MutableStateFlow<List<TtsEngine.Voice>>(emptyList())
    val availableVoices: StateFlow<List<TtsEngine.Voice>> = _availableVoices.asStateFlow()

    fun voiceWithId(id: String): TtsEngine.Voice? =
        engine.voiceWithId(id)

    fun start(fromLocator: Locator? = null) {
        replacePlaybackJob {
            publicationIterator = publication.contentIterator(fromLocator)
            playNextUtterance(Direction.Forward)
        }
    }

    fun stop() {
        replacePlaybackJob {
            _state.value = State.Stopped
            publicationIterator = null
            engine.cancel()
        }
    }

    fun pause() {
        replacePlaybackJob {
            if (state.value is State.Playing) {
                _state.value = State.Paused
                engine.cancel()
            }
        }
    }

    fun resume() {
        replacePlaybackJob {
            if (state.value is State.Paused) {
                utterances.current()
                    ?.let(::play)
            }
        }
    }

    fun resumeOrPause() {
        when (state.value) {
            is State.Failure, State.Stopped -> return
            is State.Playing -> pause()
            is State.Paused -> resume()
        }
    }

    fun previous() {
        replacePlaybackJob {
            playNextUtterance(Direction.Backward)
        }
    }

    fun next() {
        replacePlaybackJob {
            playNextUtterance(Direction.Forward)
        }
    }

    private enum class Direction {
        Forward, Backward;
    }

    private suspend fun playNextUtterance(direction: Direction) {
        val utterance = nextUtterance(direction)
        if (utterance != null) {
            play(utterance)
        } else {
            _state.value = State.Stopped
        }
    }

    private fun play(utterance: Utterance) {
        _state.value = State.Playing(utterance)

        engine.speak(TtsEngine.Utterance(
            id = utterance.id,
            text = utterance.text,
            rate = config.value.rate,
            voiceOrLanguage = utterance.voiceOrLanguage()
        ))
    }

    private fun Utterance.voiceOrLanguage(): Either<TtsEngine.Voice, Language> {
        // User selected voice, if it's compatible with the given language.
        config.value.voice
            ?.takeIf { language == null || it.language.removeRegion() == language.removeRegion() }
            ?.let { return Either.Left(it) }

        // Or fallback on the languages.
        return Either.Right(
            language
                ?.takeIf { it != publication.metadata.language }
                ?: config.value.defaultLanguage
                ?: publication.metadata.language
                ?: Language(Locale.getDefault())
        )
    }

    private suspend fun nextUtterance(direction: Direction): Utterance? =
        utterances.nextIn(direction)
            ?: (
                if (loadUtterances(direction)) nextUtterance(direction)
                else null
            )


    private suspend fun loadUtterances(direction: Direction): Boolean {
        val utterancesList = publicationIterator
            ?.nextIn(direction)
            ?.tokenize()
            ?.flatMap { it.utterances() }
            ?.let { CursorList(it) }
            ?: CursorList()

        utterances = cursorList(utterancesList, direction)
        return utterances.isNotEmpty()
    }

    private fun cursorList(list: List<Utterance>, direction: Direction): CursorList<Utterance> =
        CursorList(
            list = list,
            startIndex = when (direction) {
                Direction.Forward -> 0
                Direction.Backward -> list.size - 1
            }
        )

    private fun Content.tokenize(): List<Content> =
        tokenizerFactory.create(config.value.defaultLanguage)
            .tokenize(this)

    private fun Content.utterances(): List<Utterance> {
        fun utterance(text: String, locator: Locator, language: Language? = null): Utterance? {
            if (!text.any { it.isLetterOrDigit() })
                return null

            return Utterance(
                id = UUID.randomUUID().toString(),
                text = text,
                locator = locator,
                language = language
            )
        }

        return when (val data = data) {
            is Content.Data.Image -> {
                listOfNotNull(
                    data.description
                        ?.takeIf { it.isNotBlank() }
                        ?.let { utterance(text = it, locator = locator) }
                )
            }

            is Content.Data.Text -> {
                data.spans.mapNotNull { span ->
                    utterance(
                        text = span.text,
                        locator = span.locator,
                        language = span.language
                    )
                }
            }

            else -> emptyList()
        }
    }

    private var playbackJob: Job? = null

    /**
     * Cancels the previous playback-related job and starts a new one with the given suspending
     * [block].
     */
    private fun replacePlaybackJob(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            playbackJob?.cancelAndJoin()
            playbackJob = launch {
                mutex.withLock {
                    block()
                }
            }
        }
    }

    private inner class EngineListener : TtsEngine.Listener {

        override fun onSpeakRange(utteranceId: String, range: IntRange) {
            val utterance = utterances.current()?.takeIf { it.id == utteranceId } ?: return
            _state.value = State.Playing(
                utterance = utterance,
                range = utterance.locator.copy(
                    text = utterance.locator.text.substring(range)
                )
            )
        }

        override fun onDone(utteranceId: String) {
            if (state.value is State.Playing) {
                next()
            }
        }

        override fun onEngineError(error: TtsEngine.Exception) {
            listener?.onError(Exception.Engine(error))
            _state.value = State.Failure(Exception.Engine(error))
        }

        override fun onUtteranceError(utteranceId: String, error: TtsEngine.Exception) {
            val utterance = utterances.current()?.takeIf { it.id == utteranceId } ?: return

            listener?.onUtteranceError(utterance, Exception.Engine(error))
            _state.value = State.Paused
        }

        override fun onAvailableVoicesChange(voices: List<TtsEngine.Voice>) {
            _availableVoices.value = voices
        }
    }

    private fun <E> CursorList<E>.nextIn(direction: Direction): E? =
        when (direction) {
            Direction.Forward -> next()
            Direction.Backward -> previous()
        }

    private suspend fun ContentIterator.nextIn(direction: Direction): Content? =
        when (direction) {
            Direction.Forward -> next()
            Direction.Backward -> previous()
        }
}