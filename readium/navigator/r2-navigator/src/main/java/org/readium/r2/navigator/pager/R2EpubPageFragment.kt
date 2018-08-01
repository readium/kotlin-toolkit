/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import org.readium.r2.navigator.APPEARANCE_REF
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.navigator.SCROLL_REF


class R2EpubPageFragment : Fragment() {

    private val resourceUrl: String?
        get() = arguments!!.getString("url")

    private val bookTitle: String?
        get() = arguments!!.getString("title")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_page_epub, container, false)
        val preferences = activity?.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)!!

        // Set text color depending of appearance preference
        (v.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor(
                if (preferences.getInt(APPEARANCE_REF, 0) ?: 0 > 1) "#ffffff" else "#000000"
        ))

        val scrollMode = preferences.getBoolean(SCROLL_REF, false)
        when (scrollMode) {
            true -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.GONE
                v.setPadding(0, 4, 0, 4)
            }
            false -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.VISIBLE
                v.setPadding(0, 30, 0, 30)
            }
        }

        (v.findViewById(R.id.book_title) as TextView).text = bookTitle

        val webView: R2WebView = v!!.findViewById(R.id.webView) as R2WebView

        webView.activity = activity as R2EpubActivity

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = true
        webView.setPadding(0, 0, 0, 0)
        webView.addJavascriptInterface(webView, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                try {
                    val childCount = webView.activity.resourcePager.childCount

                    if (webView.activity.relaodPagerPositions) {
                        if (childCount == 2) {
                            when {
                                webView.activity.pagerPosition == 0 -> {
                                    val progression = preferences.getString("${webView.activity.publicationIdentifier}-documentProgression", 0.0.toString()).toDouble()
                                    webView.scrollToPosition(progression)
                                    webView.activity.pagerPosition++
                                }
                                else -> {
                                    webView.scrollToPosition(0.0)
                                    webView.activity.pagerPosition = 0
                                    webView.activity.relaodPagerPositions = false
                                }
                            }
                        } else {
                            when {
                                webView.activity.pagerPosition == 0 -> {
                                    val progression = preferences.getString("${webView.activity.publicationIdentifier}-documentProgression", 0.0.toString()).toDouble()
                                    webView.scrollToPosition(progression)
                                    webView.activity.pagerPosition++
                                }
                                webView.activity.pagerPosition == 1 -> {
                                    webView.scrollToPosition(1.0)
                                    webView.activity.pagerPosition++
                                }
                                else -> {
                                    webView.scrollToPosition(0.0)
                                    webView.activity.pagerPosition = 0
                                    webView.activity.relaodPagerPositions = false
                                }
                            }
                        }
                    }
                    else {
                        webView.activity.pagerPosition = 0
                        val progression = preferences.getString("${webView.activity.publicationIdentifier}-documentProgression", 0.0.toString()).toDouble()
                        webView.scrollToPosition(progression)
                    }
                } catch (e: Exception) {
                    // TODO double check this error, a crash happens when scrolling to fast between resources.....
                    // kotlin.TypeCastException: null cannot be cast to non-null type org.readium.r2.navigator.R2EpubActivity
                }

            }

            // prevent favicon.ico to be loaded, this was causing NullPointerException in NanoHttp
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!request.isForMainFrame && request.url.path.endsWith("/favicon.ico")) {
                    try {
                        return WebResourceResponse("image/png", null, null)
                    } catch (e: Exception) {
                    }
                }
                return null
            }

        }
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = false
        webView.setOnLongClickListener {
            true
        }
        webView.setGestureDetector(GestureDetector(context, CustomGestureDetector(webView)))
        webView.loadUrl(resourceUrl)

        return v
    }

    class CustomGestureDetector(val webView: R2WebView) : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null || e2 == null) return false
            if (e1.pointerCount > 1 || e2.pointerCount > 1)
                return false
            else {
                try { // right to left swipe .. go to next page
                    if (e1.x - e2.x > 100) {
                        webView.scrollRight()
                        return true
                    } //left to right swipe .. go to prev page
                    else if (e2.x - e1.x > 100) {
                        webView.scrollLeft()
                        return true
                    }
                } catch (e: Exception) { // nothing
                }

                return false
            }
        }
    }

    companion object {

        fun newInstance(url: String, title: String): R2EpubPageFragment {

            val args = Bundle()
            args.putString("url", url)
            args.putString("title", title)
            val fragment = R2EpubPageFragment()
            fragment.arguments = args
            return fragment
        }
    }

}


