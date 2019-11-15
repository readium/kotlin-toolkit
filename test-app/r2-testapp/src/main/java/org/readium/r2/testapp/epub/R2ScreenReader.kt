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
 * A basic screen reader based on Android's TextToSpeech
 *
 *
 */

class R2ScreenReader(var context: Context, var publication: Publication, var port: Int, var epubName:String) {

    private var initialized = false

    private var utterances = mutableListOf<String>()
    private var utterancesCurrentIndex: Int = 0
    private var items = publication.readingOrder

    enum class PLAY_SENTENCE(val value: Int) {
        SAME(0),
        NEXT(1),
        PREV(-1)
    }

    /*
     * May prove useful
    private var utterancesProgression: Int = 0
    private var resourceLength: Int = -1
    private var progression: Double = 0.0
    */

    private var textToSpeech: TextToSpeech

    private val activityReference: WeakReference<EpubActivity>
//    private var webView: WebView? = null

    var isPaused:Boolean

    val isSpeaking: Boolean
        get() = textToSpeech.isSpeaking

    private var resourceIndex:Int

    init {

        isPaused = false
        resourceIndex = 0

        //Initialize reference
        activityReference = WeakReference(context as EpubActivity)

        //Initialize TTS
        textToSpeech = TextToSpeech(context,
                TextToSpeech.OnInitListener { status ->
                    initialized = (status != TextToSpeech.ERROR)
                })

        //Create webview reference
//        val adapter = activityReference.get()?.resourcePager?.adapter as R2PagerAdapter
//        val fragment = (adapter.mFragments.get((adapter).getItemId(activityReference.get()?.resourcePager!!.currentItem))) as? R2EpubPageFragment
//        webView = fragment?.webView
    }


    fun seekTo(progression: Any) {
        // TODO
    }

    fun onResume() {
        val paused = isPaused
        if (utterances.size == 0)
            startReading()
        if (paused)
            pauseReading()
    }

    fun goTo(index: Int) {
        this.resourceIndex = index
        isPaused = false
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }

    fun previousResource() {
        resourceIndex -= 1
        isPaused = false
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }

    fun nextResource() {
        resourceIndex += 1
        isPaused = false
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }

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

            if (utterances.size == 0 ){
                nextResource()
                startReading()
                return false
            }

            //emptying TTS' queue
            flushUtterancesQueue()

            //checking progression
            textToSpeech.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
                /**
                 * Called when an utterance "starts" as perceived by the caller. This will
                 * be soon before audio is played back in the case of a [TextToSpeech.speak]
                 * or before the first bytes of a file are written to the file system in the case
                 * of [TextToSpeech.synthesizeToFile].
                 *
                 * @param utteranceId The utterance ID of the utterance.
                 */
                override fun onStart(utteranceId: String?) {
                    utterancesCurrentIndex = utteranceId!!.toInt()

                    val toHighlight = utterances[utterancesCurrentIndex]

//                    (webView as WebView).post {
//                        (webView as WebView).evaluateJavascript("findUtterance(\"$toHighlight\");", null)
//                    }

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
//                        (webView as WebView).post {
//                            (webView as WebView).evaluateJavascript("setHighlight();", null)
//                        }
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
//                    (webView as WebView).post {
//                        (webView as WebView).evaluateJavascript("setHighlight();", null)
//                    }
                    activityReference.get()?.launch {
                        activityReference.get()?.play_pause?.setImageResource(android.R.drawable.ic_media_play)

                        if (utteranceId.equals((utterances.size-1).toString())) {
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
                }
            })

        } else {
            Toast.makeText(context.applicationContext, "There was an error with the TTS initialization",
                Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun shutdown() {
        initialized = false
        stopReading()
        textToSpeech.shutdown()
    }

    fun startReading() {
        isPaused = false
        if (configure()) {
            val index = 0
            for (i in index until utterances.size) {
                try {
                    if (textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_ADD, null, i.toString()) == TextToSpeech.ERROR)
                        throw Exception("Couldn't add the string to the TTS queue")
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    fun pauseReading() {
        isPaused = true
        textToSpeech.stop()
    }

    fun stopReading() {
        isPaused = false
        textToSpeech.stop()
    }

    fun resumeReading() {
        playSentence(PLAY_SENTENCE.SAME.value)
    }

    fun nextSentence(): Boolean {
        return playSentence(PLAY_SENTENCE.NEXT.value)
    }

    fun previousSentence(): Boolean {
        return playSentence(PLAY_SENTENCE.PREV.value)
    }

    /**
     * Reorder the text to speech queue (after flushing it) according to the current track and the argument value.
     *
     * @param playSentence: [Int] - The track to play (relative to the current track).
     */
    private fun playSentence(playSentence: Int): Boolean {
        isPaused = false
        val index = utterancesCurrentIndex + playSentence

        if (index >= utterances.size || index < 0 )
            return false

        textToSpeech.speak(utterances[index], TextToSpeech.QUEUE_FLUSH, null, index.toString())

        for (i in index + 1 until utterances.size)
            textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_ADD, null, i.toString())

        return true
    }

    /**
     * Clean the text to speech queue by adding an empty text and using the TextToSpeech.QUEUE_FLUSH flag value.
     */
    private fun flushUtterancesQueue() {
        try {
            if (textToSpeech.speak("", TextToSpeech.QUEUE_FLUSH, null, null) == TextToSpeech.ERROR)
                throw Exception("Unable to flush queue")
        } catch(e: Exception) {
            Timber.e(e)
        }
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
     * voiced.
     */
    private fun splitResourceAndAddToUtterances(resourceUrl: String?) {
        val thread = Thread(Runnable {
            try {
                val document = Jsoup.connect(resourceUrl).get()
                val elements = document.select("*")

                splitParagraphAndAddToUtterances(elements)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        })

        thread.start()
        thread.join()
    }
}