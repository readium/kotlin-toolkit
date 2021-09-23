/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.toLocator
import org.readium.r2.testapp.BuildConfig.DEBUG
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * ScreenReader
 *
 * Basic screen reader engine that uses Android's TextToSpeech
 */

class ScreenReaderEngine(val context: Context, val publication: Publication) {

    interface Listener {

        fun onPlayTextChanged(text: String)

        fun onPlayStateChanged(playing: Boolean)

        fun onEndReached()
    }

    private var listeners: MutableList<Listener> = mutableListOf()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    // To avoid lifecycle issues, all `notify` functions will be called on the UI thread and
    // dispatch events to every listener attached at this time.
    private fun notifyPlayTextChanged(text: String) {
        for (listener in listeners) {
            listener.onPlayTextChanged(text)
        }
    }

    private fun notifyPlayStateChanged(playing: Boolean) {
        for (listener in listeners) {
            listener.onPlayStateChanged(playing)
        }
    }

    private fun notifyEndReached() {
        for (listener in listeners) {
            listener.onEndReached()
        }
    }

    private enum class PlaySentence(val value: Int) {
        SAME(0),
        NEXT(1),
        PREV(-1)
    }

    var isPaused: Boolean = false

    private var initialized = false

    private var pendingStartReadingResource = false

    private val items = publication.readingOrder

    private var resourceIndex = 0
        set(value) {
            when {
                value >= items.size -> {
                    field = items.size - 1
                    currentUtterance = 0
                }
                value < 0 -> {
                    field = 0
                    currentUtterance = 0
                }
                else -> {
                    field = value
                }
            }
        }

    private var utterances = mutableListOf<String>()

    var currentUtterance: Int = 0
        get() = if (field != -1) field else 0
        set(value) {
            field = when {
                value == -1 -> 0
                value == 0 -> 0
                value > utterances.size - 1 -> utterances.size - 1
                value < 0 -> 0
                else -> value
            }
            if (DEBUG) Timber.d("Current utterance index: $currentUtterance")
        }

    private var textToSpeech: TextToSpeech =
        TextToSpeech(context,
            TextToSpeech.OnInitListener { status ->
                initialized = (status != TextToSpeech.ERROR)
                onPrepared()
            })

    private fun onPrepared() {
        if (DEBUG) Timber.d("textToSpeech initialization status: $initialized")
        if (!initialized) {
            Toast.makeText(
                context.applicationContext, "There was an error with the TTS initialization",
                Toast.LENGTH_LONG
            ).show()
        }

        if (pendingStartReadingResource) {
            pendingStartReadingResource = false
            startReadingResource()
        }
    }

    val currentLocator: Locator
        get() = publication.readingOrder[resourceIndex].toLocator()

    /**
     * - Update the resource index.
     * - Mark [textToSpeech] as reading.
     * - Stop [textToSpeech] if it is reading.
     * - Start [textToSpeech] setup.
     *
     * @param resource: Int - The index of the resource we want read.
     */
    fun goTo(resource: Int) {
        resourceIndex = resource
        isPaused = false

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        startReadingResource()
    }

    fun previousResource() {
        resourceIndex--
        isPaused = false

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        startReadingResource()
    }

    fun nextResource() {
        resourceIndex++
        isPaused = false

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        startReadingResource()
    }

    private fun setTTSLanguage() {
        val language = textToSpeech.setLanguage(Locale(publication.metadata.languages.firstOrNull() ?: ""))

        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(
                context.applicationContext, "There was an error with the TTS language, switching "
                        + "to EN-US", Toast.LENGTH_LONG
            ).show()
            textToSpeech.language = Locale.US
        }
    }

    /**
     * Inner function that sets the utterances variable.
     *
     * @return: Boolean - Whether utterances was able to be filled or not.
     */
    private suspend fun setUtterances(): Boolean {
        //Load resource as sentences
        utterances = mutableListOf()
        splitResourceAndAddToUtterances(items[resourceIndex])
        return utterances.size != 0
    }

    /**
     * Call the core setup functions to set the language, the utterances and the callbacks.
     *
     * @return: Boolean - Whether executing the function was successful or not.
     */
    private suspend fun configure(): Boolean {
        setTTSLanguage()

        return withContext(Dispatchers.Default) { setUtterances() }
                && flushUtterancesQueue()
                && setTTSCallbacks()
    }

    /**
     * Set the TTS callbacks.
     *
     * @return: Boolean - Whether setting the callbacks was successful or not.
     */
    private fun setTTSCallbacks(): Boolean {
        val res = textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            /**
             * Called when an utterance "starts" as perceived by the caller. This will
             * be soon before audio is played back in the case of a [TextToSpeech.speak]
             * or before the first bytes of a file are written to the file system in the case
             * of [TextToSpeech.synthesizeToFile].
             *
             * @param utteranceId The utterance ID of the utterance.
             */
            override fun onStart(utteranceId: String?) {
                Handler(Looper.getMainLooper()).post {
                    currentUtterance = utteranceId!!.toInt()
                    notifyPlayTextChanged(utterances[currentUtterance])
                }
            }

            /**
             * Called when an utterance has successfully completed processing.
             * All audio will have been played back by this point for audible output, and all
             * output will have been written to disk for file synthesis requests.
             *
             * This request is guaranteed to be called after [.onStart].
             *
             * @param utteranceId The utterance ID of the utterance.
             */
            override fun onDone(utteranceId: String?) {
                Handler(Looper.getMainLooper()).post {
                    if (utteranceId.equals((utterances.size - 1).toString())) {
                        if (items.size - 1 == resourceIndex) {
                            stopReading()
                            Handler(Looper.getMainLooper()).post {
                                notifyPlayStateChanged(false)
                            }
                        } else {
                            nextResource()
                        }
                    }
                }
            }

            /**
             * Called when an error has occurred during processing. This can be called
             * at any point in the synthesis process. Note that there might be calls
             * to [.onStart] for specified utteranceId but there will never
             * be a call to both [.onDone] and [.onError] for
             * the same utterance.
             *
             * @param utteranceId The utterance ID of the utterance.
             */
            override fun onError(utteranceId: String?) {
                if (DEBUG) Timber
                    .e("Error saying: ${utterances[utteranceId!!.toInt()]}")
            }
        })

        if (res == TextToSpeech.ERROR) {
            if (DEBUG) Timber.e("TTS failed to set callbacks")
            return false
        }

        return true
    }

    /**
     * Stop reading and destroy the [textToSpeech].
     */
    fun shutdown() {
        initialized = false
        stopReading()
        textToSpeech.shutdown()
    }

    /**
     * Set [isPaused] to false and add the [utterances] to the [textToSpeech] queue if [configure] worked
     * successfully
     */
    private fun startReadingResource() {
        if (!initialized) {
            pendingStartReadingResource = true
            return
        }

        isPaused = false
        notifyPlayStateChanged(true)

        if (runBlocking {  configure() }) {
            if (currentUtterance >= utterances.size) {
                if (DEBUG) Timber
                    .e("Invalid currentUtterance value: $currentUtterance . Expected less than $utterances.size")
                currentUtterance = 0
            }
            val index = currentUtterance
            for (i in index until utterances.size) {
                addToUtterancesQueue(utterances[i], i)
            }
        } else if ((items.size - 1) > resourceIndex) {
            nextResource()
        } else {
            Handler(Looper.getMainLooper()).post {
                notifyPlayStateChanged(false)
                notifyEndReached()
            }
        }
    }

    /**
     * Stop text to speech and set [isPaused] to true so that subsequent playing of TTS will not automatically
     * start playing.
     */
    fun pauseReading() {
        isPaused = true
        textToSpeech.stop()
        notifyPlayStateChanged(false)
    }

    /**
     * Stop text to speech and set [isPaused] to false so that subsequent playing of TTS will automatically
     * start playing.
     */
    fun stopReading() {
        isPaused = false
        textToSpeech.stop()
        notifyPlayStateChanged(false)
    }

    /**
     * Allow to resume playing from the start of the current track while still being in a completely black box.
     *
     * @return Boolean - Whether resuming playing from the start of the current track was successful.
     */
    fun resumeReading() {
        playSentence(PlaySentence.SAME)
        notifyPlayStateChanged(true)
    }

    /**
     * Allow to go the next sentence while still being in a completely black box.
     *
     * @return Boolean - Whether moving to the next sentence was successful.
     */
    fun nextSentence(): Boolean {
        return playSentence(PlaySentence.NEXT)
    }

    /**
     * Allow to go the previous sentence while still being in a completely black box.
     *
     * @return Boolean - Whether moving to the previous sentence was successful.
     */
    fun previousSentence(): Boolean {
        return playSentence(PlaySentence.PREV)
    }

    /**
     * The entry point for the hosting activity to adjust speech speed. Input is considered valid and within arbitrary
     * set boundaries. The update is not instantaneous and [TextToSpeech] needs to be paused and resumed for it to work.
     *
     * Print an exception if [textToSpeech.setSpeechRate(speed)] fails.
     *
     * @param speed: Float - The speech speed we wish to use with Android's [TextToSpeech].
     */
    fun setSpeechSpeed(speed: Float, restart: Boolean): Boolean {
        try {
            if (textToSpeech.setSpeechRate(speed) == TextToSpeech.ERROR)
                throw Exception("Failed to update speech speed")

            if (restart) {
                pauseReading()
                resumeReading()
            }
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e.toString())
            return false
        }

        return true
    }

    /**
     * Reorder the text to speech queue (after flushing it) according to the current track and the argument value.
     *
     * @param playSentence: [PlaySentence] - The track to play (relative to the current track).
     *
     * @return Boolean - Whether the function was executed successfully.
     */
    private fun playSentence(playSentence: PlaySentence): Boolean {
        isPaused = false
        val index = currentUtterance + playSentence.value

        if (index >= utterances.size || index < 0)
            return false

        if (!flushUtterancesQueue())
            return false

        for (i in index until utterances.size) {
            if (!addToUtterancesQueue(utterances[i], i)) {
                return false
            }
        }
        return true
    }

    /**
     * Helper function that manages adding an utterance to the Text To Speech for us.
     *
     * @return: Boolean - Whether adding the utterance to the Text To Speech queue was successful.
     */
    private fun addToUtterancesQueue(utterance: String, index: Int): Boolean {
        if (textToSpeech.speak(utterance, TextToSpeech.QUEUE_ADD, null, index.toString()) == TextToSpeech.ERROR) {
            if (DEBUG) Timber
                .e("Error while adding utterance: $utterance to the TTS queue")
            return false
        }

        return true
    }

    /**
     * Helper function that manages flushing the Text To Speech for us.
     *
     * @return: Boolean - Whether flushing the Text To Speech queue was successful.
     */
    private fun flushUtterancesQueue(): Boolean {
        if (textToSpeech.speak("", TextToSpeech.QUEUE_FLUSH, null, null) == TextToSpeech.ERROR) {
            if (DEBUG) Timber.e("Error while flushing TTS queue.")
            return false
        }

        return true
    }

    /**
     * Split all the paragraphs of the resource into sentences. The sentences are then added to the [utterances] list.
     *
     * @param elements: Elements - The list of elements (paragraphs)
     */
    private fun splitParagraphAndAddToUtterances(elements: Elements) {
        val elementSize = elements.size
        var index = 0
        for (i in 0 until elementSize) {

            val element = elements.eq(i)

            if (element.`is`("p") || element.`is`("h1") || element.`is`("h2")
                || element.`is`("h3") || element.`is`("div") || element.`is`("span")
            ) {

                //val sentences = element.text().split(Regex("(?<=\\. |(,{1}))"))
                val sentences = element.text().split(Regex("(?<=\\.)"))

                for (sentence in sentences) {
                    var sentenceCleaned = sentence
                    if (sentenceCleaned.isNotEmpty()) {
                        if (sentenceCleaned.first() == ' ') sentenceCleaned = sentenceCleaned.removeRange(0, 1)
                        if (sentenceCleaned.last() == ' ') sentenceCleaned =
                            sentenceCleaned.removeRange(sentenceCleaned.length - 1, sentenceCleaned.length)
                        utterances.add(sentenceCleaned)
                        index++
                    }
                }
            }
        }
    }

    /**
     * Fetch a resource and get short sentences from it.
     *
     * @param link: String - A link to the html resource to fetch, containing the text to be voiced.
     *
     * @return: Boolean - Whether the function executed successfully.
     */
    private suspend fun splitResourceAndAddToUtterances(link: Link): Boolean {
        return try {
            val resource = publication.get(link).readAsString(charset = null).getOrThrow()
            val document = Jsoup.parse(resource)
            val elements = document.select("*")
            splitParagraphAndAddToUtterances(elements)
            true

        } catch (e: IOException) {
            if (DEBUG) Timber.e(e.toString())
            false
        }
    }
}
