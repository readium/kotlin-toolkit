/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.tts

import android.app.Activity
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.iterator.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse
import timber.log.Timber

typealias TextToSpeechTry<SuccessT> = Try<SuccessT, TextToSpeechException>

sealed class TextToSpeechException private constructor(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class IteratorError(message: String, override val cause: TextIteratorException) : TextToSpeechException(message, cause)
}

@OptIn(ExperimentalDecorator::class)
class TextToSpeechController(
    val activity: Activity,
    val publication: Publication,
    val navigator: Navigator,
    val listener: Listener,
) {
    interface Listener {
        fun onError(exception: TextToSpeechException)
    }

    private val scope: CoroutineScope = MainScope()

    private var textIterator: TextIterator? = null

    private val ttsListener = TtsListener()
    private val tts: TextToSpeech = TextToSpeech(activity, ttsListener).apply {
        setOnUtteranceProgressListener(ttsListener)
    }

    fun play(start: Locator? = null) = scope.launch {
        initializeIterator(start)
        playNextUtterance()
    }

    private suspend fun initializeIterator(start: Locator? = null) {
        // FIXME: locale
        publication.textIterator(unit = TextUnit.Sentence, start = start)
            .onSuccess { textIterator = it }
            .onFailure { listener.onError(TextToSpeechException.IteratorError("Failed to create the text iterator", it)) }
    }

    private var count = 0
    private var currentText: Text? = null

    private suspend fun playNextUtterance() {
        while (!initialized) {
            delay(100)
        }
        val iter = textIterator ?: return

        val text = iter.next().getOrElse {
            listener.onError(TextToSpeechException.IteratorError("Failed to create the text iterator", it))
            return
        }
        if (text == null) {
            textIterator = null
            return
        }

        currentText = text
        navigator.go(text.locator)
        (navigator as? DecorableNavigator)?.applyDecorations(listOf(
            Decoration(id = "tts", locator = text.locator, style = Decoration.Style.Highlight(tint = Color.RED))
        ), group = "tts")

        tts.speak(text.text, TextToSpeech.QUEUE_FLUSH, null, count++.toString())
    }

    fun pause() {

    }

    fun skipForward() {
    }

    fun skipBackward() {

    }
    private var initialized = false

    private inner class TtsListener : TextToSpeech.OnInitListener, UtteranceProgressListener() {
        override fun onInit(status: Int) {
            initialized = true
//            val res = tts.isLanguageAvailable(Locale.forLanguageTag("fr-BLA"))
////            if (res == TextToSpeech.LANG_MISSING_DATA) {
//                val installIntent = Intent()
//                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
//                activity.startActivity(installIntent)
////            } else {
////                Timber.e("FRENCH INSTALLED")
////            }
        }

        override fun onStart(utteranceId: String?) {
        }

        override fun onDone(utteranceId: String?) {
            scope.launch { playNextUtterance() }
        }

        override fun onError(utteranceId: String?) {
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            var locator = currentText?.locator ?: return
            locator = locator.copy(text = locator.text.substring(start, end))
            navigator.go(locator)

//            scope.launch {
//                (navigator as? DecorableNavigator)?.applyDecorations(
//                    listOf(
//                        Decoration(
//                            id = "tts",
//                            locator = locator,
//                            style = Decoration.Style.Underline(tint = Color.RED)
//                        )
//                    ), group = "tts2"
//                )
//            }
        }
    }
}
