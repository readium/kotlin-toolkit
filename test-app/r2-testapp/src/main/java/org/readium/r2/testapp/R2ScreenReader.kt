/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.widget.TextViewCompat
import android.util.TypedValue
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_r2_epub.*
import org.jsoup.Jsoup
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.Publication
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


class R2ScreenReader(private val context: Context, private val publication: Publication) {

    private var initialized = false
    private var paused = false

    private var utterances = mutableListOf<String>()
    private var utterancesCurrentIndex: Int = 0
    private var currentResourceHref: String? = null

    /*
     * May prove useful
    private var utterancesProgression: Int = 0
    private var resourceLength: Int = -1
    private var progression: Double = 0.0
    */

    private var textToSpeech: TextToSpeech? = null

    private val activityReference: WeakReference<R2EpubActivity>
    private var webView: WebView? = null


    init {
        //Initialize reference
        activityReference = WeakReference(context as R2EpubActivity)

        //Initialize TTS
        textToSpeech = TextToSpeech(context,
                TextToSpeech.OnInitListener { status ->
                    initialized = (status != TextToSpeech.ERROR)
                })


        //Create webview reference
        val adapter = activityReference.get()?.resourcePager?.adapter as R2PagerAdapter
        val fragment = (adapter.mFragments.get((adapter).getItemId(activityReference.get()?.resourcePager!!.currentItem))) as? R2EpubPageFragment
        webView = fragment?.webView
    }


    fun isInitialized(): Boolean {
        return initialized
    }


    fun configureTTS(resourceHref: String) {
        if (isInitialized()) {
            val language = textToSpeech?.setLanguage(Locale(publication.metadata.languages.firstOrNull()))

            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context.applicationContext, "There was an error with the TTS language, switching to EN-US", Toast.LENGTH_LONG).show()
                textToSpeech?.language = Locale.US
            }

            //Load resource as sentences
            utterances = mutableListOf()
            currentResourceHref = resourceHref
            getUtterances(currentResourceHref)

            //emptying TTS' queue
            flushUtterancesQueue()

            //checking progression
            textToSpeech?.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
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

                    (webView as WebView).post {
                        (webView as WebView).evaluateJavascript("findUtterance(\"$toHighlight\");", null)
                    }
                    activityReference.get()?.findViewById<TextView>(R.id.tts_textView)?.text = toHighlight

                    activityReference.get()?.play?.setImageResource(android.R.drawable.ic_media_pause)

                    activityReference.get()?.ttsOn = true

                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(activityReference.get()?.tts_textView!!, 1, 30, 1,
                            TypedValue.COMPLEX_UNIT_DIP);
                }

                /**
                 * Called when an utterance is stopped, whether voluntarily by the user, or not.
                 *
                 * @param utteranceId The utterance ID of the utterance.
                 * @param interrupted Whether or not the speaking has been interrupted.
                 */

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (interrupted) {
                        (webView as WebView).post {
                            (webView as WebView).evaluateJavascript("setHighlight();", null)
                        }
                        activityReference.get()?.play?.setImageResource(android.R.drawable.ic_media_play)
                        activityReference.get()?.ttsOn = false
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
                    (webView as WebView).post {
                        (webView as WebView).evaluateJavascript("setHighlight();", null)
                    }
                    activityReference.get()?.play?.setImageResource(android.R.drawable.ic_media_play)
                    activityReference.get()?.ttsOn = false
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

    fun shutdown() {
        initialized = false

        stopReading()

        textToSpeech?.shutdown()
    }


    fun startReading() {
        var index = 0
        for (sentences in utterances) {
            textToSpeech?.speak(sentences, TextToSpeech.QUEUE_ADD, null, index.toString())
            index++
        }
    }

    fun pauseReading() {
        paused = true
        textToSpeech?.stop()
    }

    fun resumeReading() {
        var index = utterancesCurrentIndex
        var first = true

        for (i in index until utterances.size) {

            if (first) {
                textToSpeech?.speak(utterances[i], TextToSpeech.QUEUE_FLUSH, null, index.toString())
                first = false
            } else {
                textToSpeech?.speak(utterances[i], TextToSpeech.QUEUE_ADD, null, index.toString())
            }
            index++
        }
    }

    fun stopReading() {
        paused = false
        textToSpeech?.stop()
//        utterances.clear()
    }

    fun next(): Boolean {
        var index = utterancesCurrentIndex + 1
        var first = true

        for (i in index until utterances.size) {

            val toSpeak = utterances.get(i)
            if (first) {
                textToSpeech?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, index.toString())
                first = false
            } else {
                textToSpeech?.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, index.toString())
            }
            index++
            return true
        }
        return false
    }

    fun prev(): Boolean {
        var index = utterancesCurrentIndex - 1
        if (index < 0) {
            index = 0
            return false
        }
        var first = true
        for (i in index until utterances.size) {
            val toSpeak = utterances.get(i)
            if (first) {
                textToSpeech?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, index.toString())
                first = false
            } else {
                textToSpeech?.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, index.toString())
            }
            index++
            return true
        }
        return false
    }
    private fun flushUtterancesQueue() {
        textToSpeech?.speak("", TextToSpeech.QUEUE_FLUSH, null, null)
    }


    fun getUtterances(resourceUrl: String?) {
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
                        val sentences = element.text().split(Regex("(?<=\\. |(,{1}))"))

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



    fun isTTSSpeaking(): Boolean {
        return textToSpeech?.isSpeaking!!
    }

    fun isPaused(): Boolean {
        return paused
    }

}