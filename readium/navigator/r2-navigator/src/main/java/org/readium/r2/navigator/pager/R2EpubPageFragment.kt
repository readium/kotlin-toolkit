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
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.webkit.WebViewClientCompat
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.R2WebView
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.extensions.htmlId
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.publication.Locator
import java.io.IOException
import java.io.InputStream

class R2EpubPageFragment : Fragment() {

    private val resourceUrl: String?
        get() = requireArguments().getString("url")

    lateinit var webView: R2WebView
    internal lateinit var listener: R2BasicWebView.Listener

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val navigatorFragment = parentFragmentManager.findFragmentByTag(getString(R.string.epub_navigator_tag)) as EpubNavigatorFragment

        val v = inflater.inflate(R.layout.viewpager_fragment_epub, container, false)
        val preferences = activity?.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)!!

        val scrollMode = preferences.getBoolean(SCROLL_REF, false)
        when (scrollMode) {
            true -> {
                v.setPadding(0, 0, 0, 0)
            }
            false -> {
                v.setPadding(0, 60, 0, 40)
            }
        }

        webView = v!!.findViewById(R.id.webView) as R2WebView

        webView.navigator = navigatorFragment
        webView.listener = navigatorFragment
        webView.preferences = preferences

        webView.setScrollMode(scrollMode)
        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.overrideUrlLoading = true
        webView.resourceUrl = resourceUrl
        webView.setPadding(0, 0, 0, 0)
        webView.addJavascriptInterface(webView, "Android")

        var endReached = false
        webView.setOnOverScrolledCallback(object : R2BasicWebView.OnOverScrolledCallback {
            override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
                val metrics = DisplayMetrics()
                requireActivity().windowManager.defaultDisplay.getMetrics(metrics)


                val topDecile = webView.contentHeight - 1.15 * metrics.heightPixels
                val bottomDecile = (webView.contentHeight - metrics.heightPixels).toDouble()

                when (scrollY.toDouble()) {
                    in topDecile..bottomDecile -> {
                        if (!endReached) {
                            endReached = true
                            webView.listener.onPageEnded(endReached)
                            when (scrollMode) {
                                true -> {
                                }
                            }
                        }
                    }
                    else -> {
                        if (endReached) {
                            endReached = false
                            webView.listener.onPageEnded(endReached)
                            when (scrollMode) {
                                true -> {
                                }
                            }
                        }
                    }
                }
            }
        })

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.hasGesture()) return false
                return if (webView.overrideUrlLoading) {
                    view.loadUrl(request.url.toString())
                    false
                } else {
                    webView.overrideUrlLoading = true
                    true
                }
            }

            override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
                // Do something with the event here
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val epubNavigator = (webView.navigator as? EpubNavigatorFragment)
                val currentFragment: R2EpubPageFragment = (epubNavigator?.resourcePager?.adapter as R2PagerAdapter).getCurrentFragment() as R2EpubPageFragment

                if (this@R2EpubPageFragment.tag == currentFragment.tag) {
                    var locations = epubNavigator.pendingLocator?.locations
                    epubNavigator.pendingLocator = null

                    // TODO this seems to be needed, will need to test more
                    if (url!!.indexOf("#") > 0) {
                        val id = url.substring(url.indexOf('#'))
                        webView.loadUrl("javascript:scrollAnchor($id);")
                        locations = Locator.Locations(fragments = listOf(id))
                    }

                    if (locations != null && locations.fragments.isEmpty()) {
                        locations.progression?.let { progression ->
                            currentFragment.webView.progression = progression

                            if (webView.scrollMode) {
                                currentFragment.webView.scrollToPosition(progression)
                            } else {
                                // FIXME: We need a better way to wait, because if the value is too low it fails
                                (object : CountDownTimer(200, 1) {
                                    override fun onTick(millisUntilFinished: Long) {}
                                    override fun onFinish() {
                                        currentFragment.webView.calculateCurrentItem()
                                        currentFragment.webView.setCurrentItem(currentFragment.webView.mCurItem, false)
                                    }
                                }).start()
                            }
                        }
                    }

                }
                webView.listener.onPageLoaded()
            }

            // prevent favicon.ico to be loaded, this was causing NullPointerException in NanoHttp
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!request.isForMainFrame && request.url.path?.endsWith("/favicon.ico") == true) {
                    try {
                        return WebResourceResponse("image/png", null, null)
                    } catch (e: Exception) {
                    }
                }
                return null
            }

            private fun injectScriptFile(view: WebView?, scriptFile: String) {
                val input: InputStream
                try {
                    input = resources.assets.open(scriptFile)
                    val buffer = ByteArray(input.available())
                    input.read(buffer)
                    input.close()

                    // String-ify the script byte-array using BASE64 encoding !!!
                    val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
                    view?.loadUrl("javascript:(function() {" +
                            "var parent = document.getElementsByTagName('head').item(0);" +
                            "var script = document.createElement('script');" +
                            "script.type = 'text/javascript';" +
                            // Tell the browser to BASE64-decode the string into your script !!!
                            "script.innerHTML = window.atob('" + encoded + "');" +
                            "parent.appendChild(script)" +
                            "})()")
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e1: IllegalStateException) {
                    // not attached to a context
                }
            }
        }

        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = false
        webView.setOnLongClickListener {
            false
        }

        val id = (webView.navigator as? EpubNavigatorFragment)?.pendingLocator?.locations?.htmlId
        if (id != null) {
            webView.loadUrl("$resourceUrl#$id")
        } else {
            resourceUrl?.let { webView.loadUrl(it) }
        }

        return v
    }

    companion object {

        fun newInstance(url: String): R2EpubPageFragment =
            R2EpubPageFragment().apply {
                arguments = Bundle().apply {
                    putString("url", url)
                }
            }

    }
}


