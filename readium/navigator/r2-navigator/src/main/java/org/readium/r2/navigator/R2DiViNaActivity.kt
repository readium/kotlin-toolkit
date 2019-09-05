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
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.navigator.pager.R2BasicWebView
import org.readium.r2.shared.Publication
import kotlin.coroutines.CoroutineContext


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

        toggleActionBar()

        // Set up divinaWebView to enable JavaScript and access to local URLs
        divinaWebView.getSettings().setJavaScriptEnabled(true)
        divinaWebView.getSettings().setAllowFileAccess(true)
        divinaWebView.getSettings().setAllowFileAccessFromFileURLs(true)
        divinaWebView.webViewClient = object : WebViewClientCompat() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Define the JS toggleMenu function that will call Android's toggleActionBar
                divinaWebView.evaluateJavascript("window.androidObj = function AndroidClass(){};", null)
                divinaWebView.evaluateJavascript("window.androidObj.toggleMenu = function() { Android.toggleMenu() };", null)

                // Now launch the DiViNa player for the folderPath = publicationPath
                divinaWebView.evaluateJavascript("if (player) { player.openDiViNaFromPath('${publicationPath}', window.androidObj.toggleMenu); };", null)
            }
        }
        divinaWebView.loadUrl("file:///android_asset/divinaPlayer.html")
        divinaWebView.addJavascriptInterface(JavaScriptInterface(), "Android")

    }

    // Define a JavaScriptInterface to call native Android code from within the divinaWebView
    private inner class JavaScriptInterface {
        @JavascriptInterface
        fun toggleMenu() {
            toggleActionBar()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        divinaWebView.evaluateJavascript("if (player) { player.destroy(); };", null)
    }
}

