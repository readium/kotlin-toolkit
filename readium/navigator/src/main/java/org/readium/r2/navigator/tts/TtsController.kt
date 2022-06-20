/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
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
import java.util.*

@ExperimentalReadiumApi
typealias TtsTokenizerFactory = (defaultLocale: Locale?) -> ContentTokenizer

@ExperimentalReadiumApi
class TtsController private constructor(
    private val publication: Publication,
    engineFactory: TtsEngineFactory,
    private val tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory
) : SuspendingCloseable {

    companion object {
        val defaultTokenizerFactory: TtsTokenizerFactory = { locale -> TextContentTokenizer(unit = TextUnit.Sentence, defaultLocale = locale) }

        operator fun invoke(
            context: Context,
            publication: Publication,
            config: TtsEngine.Configuration = TtsEngine.Configuration(
                defaultLocale = publication.metadata.locale
            ),
            tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory
        ): TtsController? = invoke(
            publication,
            engineFactory = { listener -> AndroidTtsEngine(context, config, listener) },
            tokenizerFactory = tokenizerFactory
        )

        operator fun invoke(
            publication: Publication,
            engineFactory: TtsEngineFactory,
            tokenizerFactory: TtsTokenizerFactory = defaultTokenizerFactory
        ): TtsController? {
            if (!canSpeak(publication)) return null

            return TtsController(publication, engineFactory, tokenizerFactory)
        }

        fun canSpeak(publication: Publication): Boolean =
            publication.isContentIterable
    }

    sealed class State {
        object Idle : State()
        class Playing(val utterance: TtsEngine.Utterance, val range: Locator? = null) : State()
        class Failure(val error: Exception) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val engine: TtsEngine by lazy { engineFactory(EngineListener()) }
    private val scope = MainScope()

    init {
        require(canSpeak(publication)) { "The publication cannot be spoken with TtsController, as its content is not iterable" }
    }

    override suspend fun close() {
        engine.close()
        scope.cancel()
    }

    var config: TtsEngine.Configuration
        get() = engine.config
        set(value) { engine.config = value }

    suspend fun playPause(start: Locator? = null) {
        when (state.value) {
            is State.Failure -> return
            State.Idle -> play(start)
            is State.Playing -> pause()
        }
    }

    suspend fun play(start: Locator? = null) {
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
            next()
        }
    }

    suspend fun pause() {
        _state.value = State.Idle
        engine.stop()
    }

    suspend fun previous() {
        playNextUtterance(Direction.Backward)
    }

    suspend fun next() {
        playNextUtterance(Direction.Forward)
    }

    private enum class Direction {
        Forward, Backward;
    }

    @OptIn(DelicateCoroutinesApi::class)
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
        try {
            val utterance = nextUtterance(direction)
            if (utterance != null) {
                play(utterance)
            } else {
                _state.value = State.Idle
            }
        } catch (e: Exception) {
            _state.value = State.Failure(e)
        }
    }

    private suspend fun play(utterance: TtsEngine.Utterance) {
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
        tokenizerFactory(config.defaultLocale)
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

        override fun onError(error: Exception) {
            scope.launch {
                _state.value = State.Idle
            }
        }
    }
}