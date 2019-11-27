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
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.activity_epub.*
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.readium.r2.navigator.BASE_URL
import org.readium.r2.shared.Publication
import org.readium.r2.testapp.R
import timber.log.Timber
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Locale


/**
 * R2ScreenReader
 *
 * Basic screen reader overlay that uses Android's TextToSpeech
 */

class R2ScreenReader(var context: Context, var publication: Publication, var port: Int, var epubName:String, indexResource: Int) {

    private var initialized = false

    private var resourceIndex = indexResource

    /**
     * Set the value of [resourceIndex] and dismiss screen reader if the new value is over the maximal value.
     *
     * @return: Boolean - Whether the function executed successfully.
     */
    private fun setResourceIndex(value: Int): Boolean {
        if (value >= items.size) {
            resourceIndex = items.size
            dismissScreenReader()
            return false
        }
        else if (value < 0) {
            resourceIndex = 0
        }
        else {
            resourceIndex = value
            currentUtterance = 0
        }
        Timber.d( "resourceIndex: $resourceIndex")
        return true
    }

    private var utterances = mutableListOf<String>()

    private var utterancesCurrentIndex: Int = 0

    var currentUtterance
        get() = utterancesCurrentIndex
        set(value) {
            if (value == 0)
                utterancesCurrentIndex = 0
            else if (value > utterances.size - 1)
                utterancesCurrentIndex = utterances.size - 1
            else if (value < 0)
                utterancesCurrentIndex = 0
            else
                utterancesCurrentIndex = value
        }

    private var items = publication.readingOrder

    private enum class PLAY_SENTENCE(val value: Int) {
        SAME(0),
        NEXT(1),
        PREV(-1)
    }

    private var textToSpeech: TextToSpeech

    private val activityReference: WeakReference<EpubActivity>

    var isPaused: Boolean

    val isSpeaking: Boolean
        get() = textToSpeech.isSpeaking

    val currentResource
        get() = resourceIndex


    init {
        isPaused = false

        //Initialize reference
        activityReference = WeakReference(context as EpubActivity)

        //Set utterance
        currentUtterance = activityReference.get()?.getCurrentUtterance() ?: 0

        //Initialize TTS
        textToSpeech = TextToSpeech(context,
                TextToSpeech.OnInitListener { status ->
                    initialized = (status != TextToSpeech.ERROR)
                })
    }


    fun seekTo(progression: Any) {
        // TODO
    }

    /**
     * - Set a temporary var to isPaused (isPaused's value may be altered by calls).
     * - Start initialization if [utterances] is empty.
     * - Stop [textToSpeech] if it is reading.
     * - Start [textToSpeech] setup.
     */
    fun onResume() {
        val paused = isPaused

        if (utterances.size == 0)
            startReading()
        if (paused)
            pauseReading()
    }

    /**
     * - Update the resource index.
     * - Mark [textToSpeech] as reading.
     * - Stop [textToSpeech] if it is reading.
     * - Start [textToSpeech] setup.
     *
     * @param index: Int - The index of the resource we want read.
     * @return: Boolean - Whether the function executed successfully.
     */
    fun goTo(index: Int): Boolean {
        if (index >= items.size)
            return false
        if (resourceIndex != index) {
            if (setResourceIndex(index)) {
                isPaused = false
                currentUtterance = 0
            }

            if (textToSpeech.isSpeaking) {
                textToSpeech.stop()
            }
        }
        return startReading()
    }

    /**
     * - Update the resource index.
     * - Check that it has a valid value. Update it if the value is too big or small.
     * - Mark [textToSpeech] as reading.
     * - Stop [textToSpeech] if it is reading.
     * - Start [textToSpeech] setup.
     *
     * @return: Boolean - Whether the function executed successfully.
     */
    private fun addToResourceIndex(value: Int): Boolean {
        if (setResourceIndex(resourceIndex + value)) {
            isPaused = false
            currentUtterance = 0

            if (textToSpeech.isSpeaking) {
                textToSpeech.stop()
            }

            return startReading()
        }

        return false
    }

    /**
     * - Remove 1 from [resourceIndex] by calling [R2ScreenReader.addToResourceIndex] with -1 as a parameter.
     *
     * @return: Boolean - Whether the function executed successfully.
     */
    fun previousResource(): Boolean {
        return addToResourceIndex(-1)
    }

    /**
     * - Add 1 to [resourceIndex] by calling [R2ScreenReader.addToResourceIndex] with 1 as a parameter.
     *
     * @return: Boolean - Whether the function executed successfully.
     */
    fun nextResource(): Boolean {
        return addToResourceIndex(1)
    }

    /**
     * - If [initialized] is false, print a Toast and return false.
     * - Set the language.
     * - Split the current resource and adds sentences to [utterances].
     * - If no utterances were found, restart the whole setup for the next resource.
     * - Flush [textToSpeech] queue and add listeners on its events.
     * - return true if everything went well or false if it failed.
     *
     * @return Boolean - Whether configure was successful or not.
     */
    private fun configure(): Boolean {
        if (initialized) {
            val language = textToSpeech.setLanguage(Locale(publication.metadata.languages.firstOrNull()))

            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context.applicationContext, "There was an error with the TTS language, switching "
                    + "to EN-US", Toast.LENGTH_LONG).show()
                textToSpeech.language = Locale.US
            }

            //Load resource as sentences
            utterances = mutableListOf()
            splitResourceAndAddToUtterances("$BASE_URL:$port/$epubName${items[resourceIndex].href}")

            while (resourceIndex < items.size && utterances.size == 0) {
                setResourceIndex(resourceIndex + 1)
                splitResourceAndAddToUtterances("$BASE_URL:$port/$epubName${items[resourceIndex].href}")
            }

            //emptying TTS' queue
            if (!actOnUtterancesQueue("", TextToSpeech.QUEUE_FLUSH, null))
                return false

            //checking progression
            val res = textToSpeech.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
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

                    val toHighlight = utterances[utterancesCurrentIndex]

                    activityReference.get()?.launch {
                        activityReference.get()?.findViewById<TextView>(R.id.tts_textView)?.text = toHighlight
                        activityReference.get()?.play_pause?.setImageResource(android.R.drawable.ic_media_pause)

                        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(activityReference.get()?.tts_textView!!, 1, 30, 1,
                            TypedValue.COMPLEX_UNIT_DIP)
                    }
                }

                /**
                 * Called when an utterance is stopped, whether voluntarily by the user, or not.
                 *
                 * @param utteranceId The utterance ID of the utterance.
                 * @param interrupted Whether or not the speaking has been interrupted.
                 */
                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (interrupted) {
                        activityReference.get()?.launch {
                            activityReference.get()?.play_pause?.setImageResource(android.R.drawable.ic_media_play)
                        }

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
                    activityReference.get()?.launch {
                        activityReference.get()?.play_pause?.setImageResource(android.R.drawable.ic_media_play)

                        if (utteranceId.equals((utterances.size - 1).toString())) {
                            activityReference.get()?.goForward(false, completion = {})
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
                    Timber.e("Error saying: " + utterances[utteranceId!!.toInt()])
                }
            })

            if (res == TextToSpeech.ERROR) {
                Timber.e("TTS failed to set callbacks")
                return false
            }
        } else {
            Toast.makeText(context.applicationContext, "There was an error with the TTS initialization",
                Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    /**
     * Dismiss the screen reader
     */
    private fun dismissScreenReader() {
        pauseReading()
        activityReference.get()?.dismissScreenReader()
    }

    /**
     * Stop reading and uninitialize the [textToSpeech].
     */
    fun shutdown() {
        val activity = activityReference.get()
        activity?.saveCurrentUtterance(currentUtterance.toLong())
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
        if (configure()) {
            if (utterancesCurrentIndex >= utterances.size) {
                Timber.e("Invalid utterancesCurrentIndex value: $utterancesCurrentIndex . Expected less than $utterances.size")
                currentUtterance = 0
            }
            val index = utterancesCurrentIndex
            for (i in index until utterances.size) {
                if (!actOnUtterancesQueue(utterances[i], TextToSpeech.QUEUE_ADD, i))
                    return false
            }

            return true
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
        activityReference.get()?.saveCurrentUtterance(currentUtterance.toLong())
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
     * Print an exception if [textToSpeech.setSpeechRate] fails.
     *
     * @param speed: Float - The speech speed we wish to use with Android's [TextToSpeech].
     */
    fun setSpeechSpeed(speed: Float): Boolean {
        try {
            if (textToSpeech.setSpeechRate(speed) == TextToSpeech.ERROR)
                Exception("Failed to update speech speed")

            pauseReading()
            resumeReading()
        } catch (e: Exception) {
            Timber.e(e.printStackTrace().toString())
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
        val index = utterancesCurrentIndex + playSentence.value

        if (index >= utterances.size || index < 0 )
            return false

        if (!actOnUtterancesQueue("", TextToSpeech.QUEUE_FLUSH, null))
            return false

        for (i in index until utterances.size) {
            if (!actOnUtterancesQueue(utterances[i], TextToSpeech.QUEUE_ADD, i)) {
                return false
            }
        }
        return true
    }

    /**
     * Perform an action on the TTS service and manage the result (and error if an error happened).
     * The function will shutdown the TTS if it fails.
     *
     * @param str: String - The string TTS should speak. Empty string if flushing.
     * @param flag: Int - The flag that indicates which action TTS should make [TextToSpeech.QUEUE_ADD]
     *   or [TextToSpeech.QUEUE_FLUSH].
     * @param index: Int - The index of the string in the utterances queue.
     *
     * @return Boolean - Whether the function was executed successfully.
     */
    private fun actOnUtterancesQueue(str: String, flag: Int, index: Int?): Boolean {
        try {
            if (textToSpeech.speak(str, flag, null, index?.toString()) == TextToSpeech.ERROR) {
                throw Exception("Couldn't perform action on TTS queue")
            }
        } catch (e: Exception) {
            Timber.e("Critical TTS error $e")

            Toast.makeText(context.applicationContext, "Internal TTS error",
                Toast.LENGTH_LONG).show()
            shutdown()
            return false
        }
        return true
    }

    /**
     * Split all the resource's paragraphs into sentences. The sentences are then added to the [utterances] list.
     *
     * @param elements: Elements - The list of elements (paragraphs)
     */
    private fun splitParagraphAndAddToUtterances(elements: Elements) {
        val elementSize = elements.size
        var index = 0
        for (i in 0 until elementSize) {

            val element = elements.eq(i)

            if (element.`is`("p") || element.`is`("h1") || element.`is`("h2")
                || element.`is`("h3")) {

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
                Timber.e(e.toString())
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