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
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.webkit.WebViewClientCompat
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.databinding.FragmentFxllayoutDoubleBinding
import org.readium.r2.navigator.databinding.FragmentFxllayoutSingleBinding
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.fxl.R2FXLLayout
import org.readium.r2.navigator.epub.fxl.R2FXLOnDoubleTapListener

class R2FXLPageFragment : Fragment() {

    private val firstResourceUrl: String?
        get() = requireArguments().getString("firstUrl")

    private val secondResourceUrl: String?
        get() = requireArguments().getString("secondUrl")

    private var webViews = mutableListOf<WebView>()

    private var _doubleBinding: FragmentFxllayoutDoubleBinding? = null
    private val doubleBinding get() = _doubleBinding!!

    private var _singleBinding: FragmentFxllayoutSingleBinding? = null
    private val singleBinding get() = _singleBinding!!

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        secondResourceUrl?.let {
            _doubleBinding = FragmentFxllayoutDoubleBinding.inflate(inflater, container, false)
            val view: View = doubleBinding.root
            view.setPadding(0, 0, 0, 0)

            val r2FXLLayout = doubleBinding.r2FXLLayout
            r2FXLLayout.isAllowParentInterceptOnScaled = true

            val left = doubleBinding.firstWebView
            val right = doubleBinding.secondWebView

            setupWebView(left, firstResourceUrl)
            setupWebView(right, secondResourceUrl)

            r2FXLLayout.addOnDoubleTapListener(R2FXLOnDoubleTapListener(true))
            r2FXLLayout.addOnTapListener(object : R2FXLLayout.OnTapListener {
                override fun onTap(view: R2FXLLayout, info: R2FXLLayout.TapInfo): Boolean {
                    return left.listener.onTap(PointF(info.x, info.y))
                }
            })

            return view
        }?:run {
            _singleBinding = FragmentFxllayoutSingleBinding.inflate(inflater, container, false)
            val view: View = singleBinding.root
            view.setPadding(0, 0, 0, 0)

            val r2FXLLayout = singleBinding.r2FXLLayout
            r2FXLLayout.isAllowParentInterceptOnScaled = true

            val webview = singleBinding.webViewSingle

            setupWebView(webview, firstResourceUrl)

            r2FXLLayout.addOnDoubleTapListener(R2FXLOnDoubleTapListener(true))
            r2FXLLayout.addOnTapListener(object : R2FXLLayout.OnTapListener {
                override fun onTap(view: R2FXLLayout, info: R2FXLLayout.TapInfo): Boolean {
                    return webview.listener.onTap(PointF(info.x, info.y))
                }
            })

            return view
        }
    }

    override fun onDetach() {
        super.onDetach()

        // Prevent the web views from leaking when their parent is detached.
        // See https://stackoverflow.com/a/19391512/1474476
        for (wv in webViews) {
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _singleBinding = null
        _doubleBinding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: R2BasicWebView, resourceUrl: String?) {
        webViews.add(webView)
        webView.navigator = parentFragment as Navigator
        webView.listener = parentFragment as R2BasicWebView.Listener

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false

        webView.setInitialScale(1)

        webView.setPadding(0, 0, 0, 0)
        webView.addJavascriptInterface(webView, "Android")


        webView.webViewClient = object : WebViewClientCompat() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                (webView as? R2BasicWebView)?.shouldOverrideUrlLoading(request) ?: false

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

        }
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = false
        webView.setOnLongClickListener {
            true
        }

        resourceUrl?.let { webView.loadUrl(it) }
    }

    companion object {

        fun newInstance(url: String, url2: String? = null): R2FXLPageFragment =
            R2FXLPageFragment().apply {
                arguments = Bundle().apply {
                    putString("firstUrl", url)
                    putString("secondUrl", url2)
                }
            }

    }

}


