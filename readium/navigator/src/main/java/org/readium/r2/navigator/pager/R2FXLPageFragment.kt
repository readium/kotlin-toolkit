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
import androidx.fragment.app.viewModels
import androidx.webkit.WebViewClientCompat
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.databinding.FragmentFxllayoutDoubleBinding
import org.readium.r2.navigator.databinding.FragmentFxllayoutSingleBinding
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorViewModel
import org.readium.r2.navigator.epub.fxl.R2FXLLayout
import org.readium.r2.navigator.epub.fxl.R2FXLOnDoubleTapListener

class R2FXLPageFragment : Fragment() {

    private val firstResourceUrl: String?
        get() = requireArguments().getString("firstUrl")

    private val secondResourceUrl: String?
        get() = requireArguments().getString("secondUrl")

    private var webViews = mutableListOf<R2BasicWebView>()

    private var _doubleBinding: FragmentFxllayoutDoubleBinding? = null
    private val doubleBinding get() = _doubleBinding!!

    private var _singleBinding: FragmentFxllayoutSingleBinding? = null
    private val singleBinding get() = _singleBinding!!

    private val navigator: EpubNavigatorFragment?
        get() = parentFragment as? EpubNavigatorFragment

    private val viewModel: EpubNavigatorViewModel by viewModels(ownerProducer = { requireParentFragment() })

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

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
                    return left.listener?.onTap(PointF(info.x, info.y)) ?: false
                }
            })

            return view
        } ?: run {
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
                    return webview.listener?.onTap(PointF(info.x, info.y)) ?: false
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
        for (webView in webViews) {
            webView.listener = null
        }
        _singleBinding = null
        _doubleBinding = null

        super.onDestroyView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: R2BasicWebView, resourceUrl: String?) {
        webViews.add(webView)
        navigator?.let {
            webView.listener = it.webViewListener
        }

        webView.useLegacySettings = viewModel.useLegacySettings
        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        // If we don't explicitly override the [textZoom], it will be set by Android's
        // accessibility font size system setting which breaks the layout of some fixed layouts.
        // See https://github.com/readium/kotlin-toolkit/issues/76
        webView.settings.textZoom = 100

        webView.setInitialScale(1)

        webView.setPadding(0, 0, 0, 0)
        webView.addJavascriptInterface(webView, "Android")

        webView.webViewClient = object : WebViewClientCompat() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                (webView as? R2BasicWebView)?.shouldOverrideUrlLoading(request) ?: false

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                (webView as? R2BasicWebView)?.shouldInterceptRequest(view, request)
        }
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = false
        webView.setOnLongClickListener {
            true
        }

        resourceUrl?.let { webView.loadUrl(it) }
    }

    companion object {

        fun newInstance(url: String?, url2: String? = null): R2FXLPageFragment =
            R2FXLPageFragment().apply {
                arguments = Bundle().apply {
                    putString("firstUrl", url)
                    putString("secondUrl", url2)
                }
            }
    }
}
