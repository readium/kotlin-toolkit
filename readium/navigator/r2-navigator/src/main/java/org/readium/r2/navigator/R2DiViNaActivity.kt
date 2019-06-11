/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.navigator.pager.R2BasicWebView
import org.readium.r2.shared.Publication
import kotlin.coroutines.CoroutineContext

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView

import org.zeroturnaround.zip.ZipUtil
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset


open class R2DiViNaActivity : AppCompatActivity(), CoroutineScope {
    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private lateinit var preferences: SharedPreferences
    lateinit var divinaWebView: R2BasicWebView

    private lateinit var publicationPath: String
    lateinit var publication: Publication
    private lateinit var zipName: String
    private lateinit var publicationIdentifier: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_divina)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        divinaWebView = findViewById(R.id.divinaWebView)

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        zipName = intent.getStringExtra("zipName")

        publicationIdentifier = publication.metadata.identifier
        title = publication.metadata.title

//        toggleActionBar()

        // Set up divinaWebView to enable JavaScript and access to local URLs
        divinaWebView.getSettings().setJavaScriptEnabled(true)
        divinaWebView.getSettings().setAllowFileAccess(true)
        divinaWebView.getSettings().setAllowFileAccessFromFileURLs(true)
        divinaWebView.webChromeClient = object : WebChromeClient() {
            // Send JS's console.log to Android's Log
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.apply {
                    Timber.i("DiViNaPlayer  ${message()} -- From line ${lineNumber()} of ${sourceId()}")
                }
                return true
            }
            // Wait until the HTML and its JS scripts are fully loaded before calling the divinaPlayer library
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100) {

                    // In the below, writing ${path} directly triggers an "Invalid flags supplied to RegExp constructor 'sdcard'" error...
                    divinaWebView.evaluateJavascript("player.openDiViNaFromPath('${publicationPath}');", null)
                }
            }
        }
        divinaWebView.loadUrl("file:///android_asset/index.html") // Change index.html name to avoid ambiguity

    }

    fun toggleActionBar(v: View? = null) {
        launch {
            if (supportActionBar!!.isShowing) {
                divinaWebView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            } else {
                divinaWebView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
    }
}

