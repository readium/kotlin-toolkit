/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.tts.TtsEngine.Configuration
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.Content.Data
import org.readium.r2.shared.publication.services.content.ContentIterator
import org.readium.r2.shared.publication.services.content.contentIterator
import org.readium.r2.shared.publication.services.content.isContentIterable
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.tokenizer.ContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextUnit
import timber.log.Timber
import java.util.*
import kotlin.time.Duration.Companion.seconds

@ExperimentalReadiumApi
fun interface TtsEngineFactory<E : TtsEngine> {
    fun create(listener: TtsEngine.Listener): E
}

@ExperimentalReadiumApi
fun interface TtsTokenizerFactory {
    fun create(defaultLocale: Locale?): ContentTokenizer
}

@ExperimentalReadiumApi
class TtsController<E : TtsEngine> private constructor(
    private val publication: Publication,
    engineFactory: TtsEngineFactory<E>,
    private val tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory,
    var listener: Listener? = null
) : SuspendingCloseable {

    interface Listener {
        /**
         * Notifies an [error] occurred while speaking [utterance].
         */
        fun onUtteranceError(utterance: TtsEngine.Utterance, error: TtsEngine.Exception)
    }

    companion object {
        val defaultTokenizerFactory: TtsTokenizerFactory = TtsTokenizerFactory { locale -> TextContentTokenizer(unit = TextUnit.Sentence, defaultLocale = locale) }

        operator fun invoke(
            context: Context,
            publication: Publication,
            config: Configuration = Configuration(
                defaultLocale = publication.metadata.locale
            ),
            tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory
        ): TtsController<AndroidTtsEngine>? = invoke(
            publication,
            engineFactory = { listener -> AndroidTtsEngine(context, config, listener) },
            tokenizerFactory = tokenizerFactory
        )

        operator fun <E : TtsEngine> invoke(
            publication: Publication,
            engineFactory: TtsEngineFactory<E>,
            tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory
        ): TtsController<E>? {
            if (!canSpeak(publication)) return null

            return TtsController(publication, engineFactory, tokenizerFactory)
        }

        fun canSpeak(publication: Publication): Boolean =
            publication.isContentIterable
    }

    sealed class State {
        object Idle : State()
        class Playing(val utterance: TtsEngine.Utterance, val range: Locator? = null) : State()
        class Failure(val error: TtsEngine.Exception) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Underlying [TtsEngine] instance.
     *
     * WARNING: Don't control the playback or set the config directly with the engine. Use the
     * [TtsController] APIs instead. This property is used to access engine-specific APIs such as
     * [AndroidTtsEngine.requestInstallMissingVoice],
     */
    val engine: E by lazy { engineFactory.create(EngineListener()) }
    private val scope = MainScope()

    init {
        require(canSpeak(publication)) { "The publication cannot be spoken with TtsController, as its content is not iterable" }
    }

    override suspend fun close() {
        tryOrLog {
            engine.close()
        }
        scope.cancel()
    }

    val config: StateFlow<Configuration> get() = engine.config

    fun setConfig(config: Configuration): Configuration =
        engine.setConfig(config)

    fun playPause() {
        when (state.value) {
            is State.Failure -> return
            State.Idle -> play()
            is State.Playing -> pause()
        }
    }

    fun play(start: Locator? = null) {
        replacePlaybackJob {
            if (start != null) {
                speakingUtteranceIndex = null
                utterances = emptyList()
                contentIterator = publication.contentIterator(start)
            }

            if (contentIterator == null) {
                contentIterator = publication.contentIterator(null)
            }

            val utterance = currentUtterance
            if (utterance != null) {
                play(utterance)
            } else {
                playNextUtterance(Direction.Forward)
            }
        }
    }

    fun pause() {
        replacePlaybackJob {
            _state.value = State.Idle
            engine.stop()
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

    private var contentIterator: ContentIterator? = null
        set(value) {
            contentIterator?.let { previous ->
                scope.launch { previous.close() }
            }
            field = value
        }

    private var speakingUtteranceIndex: Int? = null

    private var utterances = emptyList<TtsEngine.Utterance>()

    private val currentUtterance: TtsEngine.Utterance? get() =
        speakingUtteranceIndex?.let { utterances[it] }

    private suspend fun playNextUtterance(direction: Direction) {
        val utterance = nextUtterance(direction)
        if (utterance != null) {
            play(utterance)
        } else {
            _state.value = State.Idle
        }
    }

    private fun play(utterance: TtsEngine.Utterance) {
        _state.value = State.Playing(utterance)
        engine.speak(utterance)
    }

    private suspend fun nextUtterance(direction: Direction): TtsEngine.Utterance? {
        val nextIndex = nextUtteranceIndex(direction)
        if (nextIndex == null) {
            return if (loadNextUtterances(direction)) {
                nextUtterance(direction)
            } else {
                null
            }
        }

        speakingUtteranceIndex = nextIndex
        return utterances[nextIndex]
    }

    private fun nextUtteranceIndex(direction: Direction): Int? {
        val index = when (direction) {
            Direction.Forward -> (speakingUtteranceIndex ?: -1) + 1
            Direction.Backward -> (speakingUtteranceIndex ?: utterances.size) - 1
        }

        return index
            .takeIf { utterances.indices.contains(it) }
    }

    private suspend fun loadNextUtterances(direction: Direction): Boolean {
        val content = when (direction) {
            Direction.Forward -> contentIterator?.next()
            Direction.Backward -> contentIterator?.previous()
        }

        utterances = content
            ?.tokenize()
            ?.flatMap { it.utterances() }
            ?: emptyList()

        speakingUtteranceIndex = null

        return utterances.isNotEmpty()
    }

    private fun Content.tokenize(): List<Content> =
        tokenizerFactory.create(config.value.defaultLocale)
            .tokenize(this)

    private fun Content.utterances(): List<TtsEngine.Utterance> {
        fun utterance(text: String, locator: Locator, language: Locale? = null): TtsEngine.Utterance? {
            if (!text.any { it.isLetterOrDigit() })
                return null

            return TtsEngine.Utterance(
                text = text,
                locator = locator,
                language = language
            )
        }

        return when (val data = data) {
            is Data.Image -> {
                listOfNotNull(
                    data.description
                        ?.takeIf { it.isNotBlank() }
                        ?.let { utterance(text = it, locator = locator) }
                )
            }

            is Data.Text -> {
                data.spans.mapNotNull { span ->
                    utterance(
                        text = span.text,
                        locator = span.locator,
                        language = span.locale
                    )
                }
            }

            else -> emptyList()
        }
    }

    private var playbackJob: Job? = null

    /**
     * Cancels the previous playback-related job and starts a new one witht he given suspending
     * [block].
     */
    private fun replacePlaybackJob(block: suspend CoroutineScope.() -> Unit) {
        playbackJob?.cancel()
        playbackJob = scope.launch(block = block)
    }

    private inner class EngineListener : TtsEngine.Listener {

        override fun onSpeakRangeAt(locator: Locator, utterance: TtsEngine.Utterance) {
            scope.launch {
                _state.value = State.Playing(utterance, range = locator)
            }
        }

        override fun onStop() {
            scope.launch {
                if (state.value is State.Playing) {
                    next()
                }
            }
        }

        override fun onEngineError(error: TtsEngine.Exception) {
            scope.launch {
                _state.value = State.Idle
            }
        }

        override fun onUtteranceError(utterance: TtsEngine.Utterance, error: TtsEngine.Exception) {
            scope.launch {
                listener?.onUtteranceError(utterance, error)

                _state.update { state ->
                    if (state is State.Playing) State.Idle
                    else state
                }
            }
        }
    }
}