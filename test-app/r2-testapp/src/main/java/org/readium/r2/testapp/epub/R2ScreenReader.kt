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
import org.jsoup.Jsoup
import org.readium.r2.navigator.BASE_URL
import org.readium.r2.shared.Publication
import org.readium.r2.testapp.R
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


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

    fun stop() {
        stopReading()
    }

    fun pause() {
        pauseReading()
    }

    fun release() {
        shutdown()
    }

    fun start() {
        startReading()
    }

    fun resume() {
        resumeReading()
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


    fun configure() {
        if (initialized) {
            val language = textToSpeech.setLanguage(Locale(publication.metadata.languages.firstOrNull()))

            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context.applicationContext, "There was an error with the TTS language, switching to EN-US", Toast.LENGTH_LONG).show()
                textToSpeech.language = Locale.US
            }

            //Load resource as sentences
            utterances = mutableListOf()

            getUtterances("$BASE_URL:$port/$epubName${items[resourceIndex].href}")

            if (utterances.size == 0 ){
                nextResource()
                startReading()
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

                    activityReference.get()?.findViewById<TextView>(R.id.tts_textView)?.text = toHighlight


                    activityReference.get()?.play_pause?.setImageResource(android.R.drawable.ic_media_pause)


                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(activityReference.get()?.tts_textView!!, 1, 30, 1,
                            TypedValue.COMPLEX_UNIT_DIP)
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
                        activityReference.get()?.play_pause?.setImageResource(android.R.drawable.ic_media_play)
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
                    activityReference.get()?.play_pause?.setImageResource(android.R.drawable.ic_media_play)

                    if (utteranceId.equals((utterances.size-1).toString())) {
                        activityReference.get()?.goForward(false, completion = {})
                        nextResource()
                        startReading()
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
            Toast.makeText(context.applicationContext, "There was an error with the TTS initialization", Toast.LENGTH_LONG).show()
        }
    }

    private fun shutdown() {
        initialized = false
        stopReading()
        textToSpeech.shutdown()
    }


    private fun startReading() {
        isPaused = false
        configure()
        val index = 0
        for (i in index until utterances.size) {
            textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_ADD, null, i.toString())
        }
    }

    private fun pauseReading() {
        isPaused = true
        textToSpeech.stop()
    }

    private fun resumeReading() {
        isPaused = false
        val index = utterancesCurrentIndex
        for (i in index until utterances.size) {
            if (i == index) {
                textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_FLUSH, null, i.toString())
            } else {
                textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_ADD, null, i.toString())
            }
        }
    }

    private fun stopReading() {
        isPaused = false
        textToSpeech.stop()
    }

    fun nextSentence(): Boolean {
        isPaused = false
        val index = utterancesCurrentIndex + 1
        if (utterancesCurrentIndex < 0) {
            return false
        }
        for (i in index until utterances.size) {
            if (i == index) {
                textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_FLUSH, null, i.toString())
            } else {
                textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_ADD, null, i.toString())
            }
        }
        return true
    }

    fun previousSentence(): Boolean {
        isPaused = false
        val index = utterancesCurrentIndex - 1
        if (utterancesCurrentIndex > utterances.size) {
            return false
        }
        for (i in index until utterances.size) {
            if (i == index) {
                textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_FLUSH, null, i.toString())
            } else {
                textToSpeech.speak(utterances[i], TextToSpeech.QUEUE_ADD, null, i.toString())
            }
        }
        return true
    }

    private fun flushUtterancesQueue() {
        textToSpeech.speak("", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun getUtterances(resourceUrl: String?) {
        val thread = Thread(Runnable {
            try {
                val document = Jsoup.connect(resourceUrl).get()
                val elements = document.select("*")
                val elementSize = elements.size

                var index2=0

                for (i in 0 until elementSize) {
                    val element = elements.eq(i)

                    if (element.`is`("p") || element.`is`("h1") || element.`is`("h2") || element.`is`("h3")) {
                        /*
                         * Splitting the big paragraph into smaller paragraphs
                         * (sentences by sentences)
                         * These sentences will be passed onto TTS
                         */
//                        val sentences = element.text().split(Regex("(?<=\\. |(,{1}))"))
                        val sentences = element.text().split(Regex("(?<=\\.)"))

                        for (sentence in sentences) {
                            var sentenceCleaned = sentence
                            if (sentenceCleaned.isNotEmpty()) {
                                if (sentenceCleaned.first() == ' ') sentenceCleaned = sentenceCleaned.removeRange(0, 1)
                                if (sentenceCleaned.last() == ' ') sentenceCleaned = sentenceCleaned.removeRange(sentenceCleaned.length - 1, sentenceCleaned.length)
                                utterances.add(sentenceCleaned)
                                index2++
                            }
                        }
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

        })

        thread.start()
        thread.join()
    }
}