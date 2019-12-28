/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.epub

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.readium.r2.navigator.BASE_URL
import org.readium.r2.navigator.IR2TTS
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.shared.Publication
import timber.log.Timber
import java.io.IOException
import java.util.*


/**
 * R2ScreenReader
 *
 * Basic screen reader overlay that uses Android's TextToSpeech
 */

class R2ScreenReader(var context: Context, var ttsCallbacks: IR2TTS, var navigator: VisualNavigator, var publication: Publication, private var port: Int, private var epubName: String, initialResourceIndex: Int) {

    private var initialized = false

    private var resourceIndex = initialResourceIndex
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
            Timber.tag(this::class.java.simpleName).d("Current utterance index: $currentUtterance")
        }

    private var items = publication.readingOrder

    private enum class PLAY_SENTENCE(val value: Int) {
        SAME(0),
        NEXT(1),
        PREV(-1)
    }

    private var textToSpeech: TextToSpeech

    var isPaused: Boolean

    val isSpeaking: Boolean
        get() = textToSpeech.isSpeaking

    val currentResource
        get() = resourceIndex


    init {
        isPaused = false

        //Initialize TTS
        textToSpeech = TextToSpeech(context,
                TextToSpeech.OnInitListener { status ->
                    initialized = (status != TextToSpeech.ERROR)
                    Timber.tag(this::class.java.simpleName).d("textToSpeech initialization status: $initialized")
                })
    }


    /**
     * - Set a temporary var to isPaused (isPaused's value may be altered by calls).
     * - Start initialization if [utterances] is empty.
     * - Stop [textToSpeech] if it is reading.
     */
    fun onResume() {
        val paused = isPaused

        if (utterances.size == 0) {
            startReading()
        }
        if (paused) {
            pauseReading()
        }
    }

    /**
     * - Update the resource index.
     * - Mark [textToSpeech] as reading.
     * - Stop [textToSpeech] if it is reading.
     * - Start [textToSpeech] setup.
     *
     * @param resource: Int - The index of the resource we want read.
     * @return: Boolean - Whether the function executed successfully.
     */
    fun goTo(resource: Int): Boolean {
        if (resource >= items.size)
            return false

        resourceIndex = resource
        isPaused = false

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        return startReading()
    }

    /**
     * @return: Boolean - Whether the function executed successfully.
     */
    fun previousResource(): Boolean {
        resourceIndex--
        isPaused = false

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }

        return startReading()
    }

    /**
     * @return: Boolean - Whether the function executed successfully.
     */
    fun nextResource(): Boolean {
        resourceIndex++
        isPaused = false

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }

        return startReading()
    }

    /**
     * Inner function that sets the Text To Speech language.
     */
    private fun setTTSLanguage() {
        val language = textToSpeech.setLanguage(Locale(publication.metadata.languages.firstOrNull()))

        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context.applicationContext, "There was an error with the TTS language, switching "
                    + "to EN-US", Toast.LENGTH_LONG).show()
            textToSpeech.language = Locale.US
        }
    }

    /**
     * Inner function that sets the utterances variable.
     *
     * @return: Boolean - Whether utterances was able to be filled or not.
     */
    private fun setUtterances(): Boolean {
        //Load resource as sentences
        utterances = mutableListOf()
        splitResourceAndAddToUtterances("$BASE_URL:$port/$epubName${items[resourceIndex].href}")

//        while (++resourceIndex < items.size && utterances.size == 0) {
//            splitResourceAndAddToUtterances("$BASE_URL:$port/$epubName${items[resourceIndex].href}")
//        }
//
//        if (resourceIndex == items.size)
//            --resourceIndex

        return utterances.size != 0
    }

    /**
     * Call the core setup functions to set the language, the utterances and the callbacks.
     *
     * @return: Boolean - Whether executing the function was successful or not.
     */
    private fun configure(): Boolean {
        setTTSLanguage()

        return setUtterances()
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
                currentUtterance = utteranceId!!.toInt()

                ttsCallbacks.playTextChanged(utterances[currentUtterance])
                ttsCallbacks.playStateChanged(true)
            }

            /**
             * Called when an utterance is stopped, whether voluntarily by the user, or not.
             *
             * @param utteranceId The utterance ID of the utterance.
             * @param interrupted Whether or not the speaking has been interrupted.
             */
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (interrupted) {
                    ttsCallbacks.playStateChanged(false)
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
                ttsCallbacks.playStateChanged(false)

                if (utteranceId.equals((utterances.size - 1).toString())) {
                    if (items.size - 1 == resourceIndex) {
                        dismissScreenReader()
                        stopReading()
                    } else {
                        navigator.goForward(false, completion = {})
                        nextResource()
                        startReading()
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
                Timber.tag(this::class.java.simpleName).e("Error saying: ${utterances[utteranceId!!.toInt()]}")
            }
        })

        if (res == TextToSpeech.ERROR) {
            Timber.tag(this::class.java.simpleName).e("TTS failed to set callbacks")
            return false
        }

        return true
    }

    /**
     * Dismiss the screen reader
     */
    private fun dismissScreenReader() {
        pauseReading()
        ttsCallbacks.dismissScreenReader()
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
     * successfully (returning true)
     *
     * @return: Boolean - Whether the function executed successfully.
     */
    private fun startReading(): Boolean {
        isPaused = false
        if (initialized && configure()) {
            if (currentUtterance >= utterances.size) {
                Timber.tag(this::class.java.simpleName).e("Invalid currentUtterance value: $currentUtterance . Expected less than $utterances.size")
                currentUtterance = 0
            }
            val index = currentUtterance
            for (i in index until utterances.size) {
                if (!addToUtterancesQueue(utterances[i], i))
                    return false
            }

            return true
        } else if (initialized && (items.size - 1) > resourceIndex) {
            navigator.goForward(false, completion = {})
            return nextResource()
        }


        if (!initialized) {
            Toast.makeText(
                    context.applicationContext, "There was an error with the TTS initialization",
                    Toast.LENGTH_LONG
            ).show()
        }

        return false
    }

    /**
     * Stop text to speech and set [isPaused] to true so that subsequent playing of TTS will not automatically
     * start playing.
     */
    fun pauseReading() {
        isPaused = true
        textToSpeech.stop()
    }

    /**
     * Stop text to speech and set [isPaused] to false so that subsequent playing of TTS will automatically
     * start playing.
     */
    fun stopReading() {
        isPaused = false
        textToSpeech.stop()
    }

    /**
     * Allow to resume playing from the start of the current track while still being in a completely black box.
     *
     * @return Boolean - Whether resuming playing from the start of the current track was successful.
     */
    fun resumeReading() {
        playSentence(PLAY_SENTENCE.SAME)
    }

    /**
     * Allow to go the next sentence while still being in a completely black box.
     *
     * @return Boolean - Whether moving to the next sentence was successful.
     */
    fun nextSentence(): Boolean {
        return playSentence(PLAY_SENTENCE.NEXT)
    }

    /**
     * Allow to go the previous sentence while still being in a completely black box.
     *
     * @return Boolean - Whether moving to the previous sentence was successful.
     */
    fun previousSentence(): Boolean {
        return playSentence(PLAY_SENTENCE.PREV)
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
            Timber.tag(this::class.java.simpleName).e(e.toString())
            return false
        }

        return true
    }

    /**
     * Reorder the text to speech queue (after flushing it) according to the current track and the argument value.
     *
     * @param playSentence: [PLAY_SENTENCE] - The track to play (relative to the current track).
     *
     * @return Boolean - Whether the function was executed successfully.
     */
    private fun playSentence(playSentence: PLAY_SENTENCE): Boolean {
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
            Timber.tag(this::class.java.simpleName).e("Error while adding utterance: $utterance to the TTS queue")
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
            Timber.tag(this::class.java.simpleName).e("Error while flushing TTS queue.")
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
                    || element.`is`("h3") || element.`is`("div") || element.`is`("span")) {

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
     * @param resourceUrl: String - The html resource to fetch from the internal server, containing the text to be
     *   voiced.
     *
     * @return: Boolean - Whether the function executed successfully.
     */
    private fun splitResourceAndAddToUtterances(resourceUrl: String?): Boolean {
        var success = false
        val thread = Thread(Runnable {
            try {
                val document = Jsoup.connect(resourceUrl).get()
                val elements = document.select("*")

                splitParagraphAndAddToUtterances(elements)

            } catch (e: IOException) {
                Timber.tag(this::class.java.simpleName).e(e.toString())
                success = false
                return@Runnable
            }
            success = true
        })

        thread.start()
        thread.join()
        return success
    }
}