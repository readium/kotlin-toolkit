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
import android.util.Base64
import android.util.DisplayMetrics
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.R2WebView
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.extensions.htmlId
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.ReadingProgression
import java.io.IOException
import java.io.InputStream
import kotlin.math.roundToInt

class R2EpubPageFragment : Fragment() {

    private val resourceUrl: String?
        get() = requireArguments().getString("url")

    var webView: R2WebView? = null
        private set

    private var windowInsets: WindowInsetsCompat = WindowInsetsCompat.CONSUMED
    private lateinit var containerView: View
    private lateinit var preferences: SharedPreferences

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val navigatorFragment = parentFragmentManager.findFragmentByTag(getString(R.string.epub_navigator_tag)) as EpubNavigatorFragment

        containerView = inflater.inflate(R.layout.viewpager_fragment_epub, container, false)
        preferences = activity?.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)!!

        val webView = containerView.findViewById(R.id.webView) as R2WebView
        this.webView = webView

        webView.navigator = navigatorFragment
        webView.listener = navigatorFragment
        webView.preferences = preferences

        webView.setScrollMode(preferences.getBoolean(SCROLL_REF, false))
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
                val activity = activity ?: return
                val metrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(metrics)


                val topDecile = webView.contentHeight - 1.15 * metrics.heightPixels
                val bottomDecile = (webView.contentHeight - metrics.heightPixels).toDouble()

                when (scrollY.toDouble()) {
                    in topDecile..bottomDecile -> {
                        if (!endReached) {
                            endReached = true
                            webView.listener.onPageEnded(endReached)
                        }
                    }
                    else -> {
                        if (endReached) {
                            endReached = false
                            webView.listener.onPageEnded(endReached)
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
                val currentFragment: R2EpubPageFragment? =
                    (epubNavigator?.resourcePager?.adapter as? R2PagerAdapter)?.getCurrentFragment() as? R2EpubPageFragment

                if (currentFragment != null && this@R2EpubPageFragment.tag == currentFragment.tag) {
                    var locations = epubNavigator.pendingLocator?.locations
                    epubNavigator.pendingLocator = null

                    // TODO this seems to be needed, will need to test more
                    if (url != null && url.indexOf("#") > 0) {
                        val id = url.substringAfterLast("#")
                        locations = Locator.Locations(fragments = listOf(id))
                    }

                    val currentWebView = currentFragment.webView
                    if (currentWebView != null && locations != null) {

                        lifecycleScope.launchWhenStarted {
                            // FIXME: We need a better way to wait, because if the value is too low it fails
                            delay(200)

                            val htmlId = locations.htmlId
                            var progression = locations.progression

                            when {
                                htmlId != null -> currentWebView.scrollToId(htmlId)

                                progression != null -> {
                                    // We need to reverse the progression with RTL because the Web View
                                    // always scrolls from left to right, no matter the reading direction.
                                    progression =
                                        if (webView.scrollMode || navigatorFragment.readingProgression == ReadingProgression.LTR) progression
                                        else 1 - progression

                                    if (webView.scrollMode) {
                                        currentWebView.scrollToPosition(progression)

                                    } else {
                                        // Figure out the target web view "page" from the requested
                                        // progression.
                                        var item = (progression * currentWebView.numPages).roundToInt()
                                        if (navigatorFragment.readingProgression == ReadingProgression.RTL && item > 0) {
                                            item -= 1
                                        }
                                        currentWebView.setCurrentItem(item, false)
                                    }
                                }
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

        resourceUrl?.let { webView.loadUrl(it) }

        setupPadding()

        return containerView
    }

    private fun setupPadding() {
        updatePadding()

        // Update padding when the scroll mode changes
        viewLifecycleOwner.lifecycleScope.launch {
            webView?.scrollModeFlow?.collectLatest {
                updatePadding()
            }
        }

        // Update padding when the window insets change, for example when the navigation and status
        // bars are toggled.
        ViewCompat.setOnApplyWindowInsetsListener(containerView) { _, insets ->
            windowInsets = insets
            updatePadding()
            insets
        }
    }

    private fun updatePadding() {
        val activity = activity ?: return
        if (!viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            return
        }

        val scrollMode = preferences.getBoolean(SCROLL_REF, false)
        if (scrollMode) {
            containerView.setPadding(0, 0, 0, 0)

        } else {
            val margin = resources.getDimension(R.dimen.r2_navigator_epub_vertical_padding).toInt()
            var top = margin
            var bottom = margin

            // Add additional padding to take into account the display cutout, if needed.
            if (
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P &&
                activity.window.attributes.layoutInDisplayCutoutMode != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            ) {
                val displayCutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                top += displayCutoutInsets.top
                bottom += displayCutoutInsets.bottom
            }

            containerView.setPadding(0, top, 0, bottom)
        }
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


