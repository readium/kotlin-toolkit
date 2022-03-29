/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.*
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.*
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.*
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import java.util.*

sealed class TextToSpeechException private constructor(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class IteratorError(message: String, cause: Exception) : TextToSpeechException(message, cause)
    class LanguageNotSupported(val locale: Locale) : TextToSpeechException("The language ${locale.toLanguageTag()} is not supported by the TTS engine")
    class LanguageMissingData(val locale: Locale) : TextToSpeechException("The language ${locale.toLanguageTag()} requires additional files by the TTS engine")
}

class TextToSpeechController(
    val context: Context,
    val publication: Publication,
    val listener: Listener,
    config: Configuration = Configuration(),
) : SuspendingCloseable {
    data class Configuration(
        val defaultLocale: Locale? = null,
        val rate: Double = 1.0
    )

    interface Listener {
        fun onSpeakUtterrance(text: String, locale: Locale, locator: Locator)
        fun onSpeakUtteranceRange(locator: Locator)
        fun onError(exception: TextToSpeechException)
    }

    private val scope: CoroutineScope = MainScope()

    private var textIterator: ContentIterator? = null

    private val ttsListener = TtsListener()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, ttsListener).apply {
        setOnUtteranceProgressListener(ttsListener)
    }

    var config: Configuration = config
        set(value) {
            field = value
            tts.setSpeechRate(value.rate.toFloat())
        }

    var voice: Voice
        get() = tts.voice
        set(value) { tts.voice = value }

    val voices: Map<Locale, List<Voice>> get() =
        tts.voices.groupBy(Voice::getLocale)

    fun play(start: Locator? = null) = scope.launch {
        initializeIterator(start)
        playNextUtterance()
    }

    override suspend fun close() {
        textIterator?.close()
        scope.cancel()
        tts.shutdown()
    }

    private suspend fun initializeIterator(start: Locator? = null) {
        textIterator = PublicationContentIterator(publication, start = start, resourceContentIteratorFactories = listOf(HtmlResourceContentIterator.createFactory()))
    }

    private var count = 0
    private var currentSpan: Content.Text.Span? = null
    private var spans = mutableListOf<Content.Text.Span>()

    private suspend fun playNextUtterance() {
        while (!initialized) {
            delay(100)
        }
        val span = spans.removeFirstOrNull()
        currentSpan = span
        if (span != null) {
            if (!span.text.any { it.isLetterOrDigit() }) {
                playNextUtterance()
                return
            }

            val locale = span.language?.let { Locale.forLanguageTag(it.replace("_", "-")) }
                ?: config.defaultLocale
                ?: publication.metadata.locale
                ?: Locale.getDefault()

            val localeResult = tts.setLanguage(locale)
            if (localeResult >= LANG_AVAILABLE) {
                tts.speak(span.text, QUEUE_FLUSH, null, count++.toString())
                listener.onSpeakUtterrance(span.text, locale, span.locator)
            } else {
                if (localeResult == LANG_MISSING_DATA) {
                    listener.onError(TextToSpeechException.LanguageMissingData(locale))
                } else {
                    listener.onError(TextToSpeechException.LanguageNotSupported(locale))
                }
                playNextUtterance()
            }

        } else {
            val iter = textIterator ?: return

            val text = iter.next().getOrElse {
                listener.onError(
                    TextToSpeechException.IteratorError(
                        "Failed to create the text iterator",
                        it
                    )
                )
                return
            } ?: return

            if (text is Content.Text) {
                spans = text.spans
                    .flatMap { tokenize(it).getOrThrow() }
                    .toMutableList()
            }
            playNextUtterance()
        }

    }

    private suspend fun tokenize(span: Content.Text.Span): Try<List<Content.Text.Span>, Exception> =
        unitTextContentTokenizer(unit = TextUnit.Sentence, locale = span.locale).tokenize(span.text)
            .map {
                it.map { text ->
                    Content.Text.Span(
                        locator = span.locator.copy(text = text),
                        text = text.highlight ?: "",
                        language = span.language
                    )
                }
            }

    fun pause() {
        tts.stop()
    }

    fun skipForward() = scope.launch {
        playNextUtterance()
    }

    fun skipBackward() {

    }
    private var initialized = false

    private inner class TtsListener : TextToSpeech.OnInitListener, UtteranceProgressListener() {
        override fun onInit(status: Int) {
            initialized = true
        }

        override fun onStart(utteranceId: String?) {
        }

        override fun onDone(utteranceId: String?) {
            scope.launch { playNextUtterance() }
        }

        override fun onError(utteranceId: String?) {
            scope.launch { playNextUtterance() }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            var locator = currentSpan?.locator ?: return
            locator = locator.copy(text = locator.text.substring(start, end))
            listener.onSpeakUtteranceRange(locator)
        }
    }
}
