/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.navigator.fxl.R2FXLLayout
import org.readium.r2.navigator.fxl.R2FXLOnDoubleTapListener


class R2FXLPageFragment : Fragment() {

    private val firstResourceUrl: String?
        get() = arguments!!.getString("firstUrl")

    private val secondResourceUrl: String?
        get() = arguments!!.getString("secondUrl")

    private val bookTitle: String?
        get() = arguments!!.getString("title")


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val preferences = activity?.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)!!

        val view: View = inflater.inflate(R.layout.fxlview_double, container, false)

        val r2FXLLayout = view.findViewById<View>(R.id.r2FXLLayout) as R2FXLLayout
        r2FXLLayout.isAllowParentInterceptOnScaled = true
        r2FXLLayout.addOnDoubleTapListener(R2FXLOnDoubleTapListener(true))

        r2FXLLayout.addOnTapListener(object : R2FXLLayout.OnTapListener {
            override fun onTap(view: R2FXLLayout, info: R2FXLLayout.TapInfo): Boolean {
                (activity as R2EpubActivity).toggleActionBar()
                return true
            }
        })

        val left = view.findViewById<View>(R.id.firstWebView) as R2WebView
        val right = view.findViewById<View>(R.id.secondWebView) as R2WebView

        setupWebView(left, preferences, firstResourceUrl, r2FXLLayout)
        setupWebView(right, preferences, secondResourceUrl, r2FXLLayout)

        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: R2WebView, preferences: SharedPreferences, resourceUrl: String?, r2FXLLayout: R2FXLLayout) {
        webView.activity = activity as R2EpubActivity

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        webView.setInitialScale(1)

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

                    if (webView.activity.reloadPagerPositions) {
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
                                    webView.activity.reloadPagerPositions = false
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
                                    webView.activity.reloadPagerPositions = false
                                }
                            }
                        }
                    } else {
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
//        webView.setGestureDetector(GestureDetector(context, CustomGestureDetector(webView)))
        webView.loadUrl(resourceUrl)
    }

//    class CustomGestureDetector(val webView: R2WebView) : GestureDetector.SimpleOnGestureListener() {
//
//        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
//            if (e1 == null || e2 == null) return false
//            if (e1.pointerCount > 1 || e2.pointerCount > 1)
//                return false
//            else {
//                try { // right to left swipe .. go to next page
//                    if (e1.x - e2.x > 100) {
//                        webView.scrollRight()
//                        return true
//                    } //left to right swipe .. go to prev page
//                    else if (e2.x - e1.x > 100) {
//                        webView.scrollLeft()
//                        return true
//                    }
//                } catch (e: Exception) { // nothing
//                }
//
//                return false
//            }
//        }
//    }

    companion object {

        fun newInstance(url: String, url2: String, title: String): R2FXLPageFragment {

            val args = Bundle()
            args.putString("firstUrl", url)
            args.putString("secondUrl", url2)
            args.putString("title", title)

            val fragment = R2FXLPageFragment()
            fragment.arguments = args
            return fragment
        }
    }

}


