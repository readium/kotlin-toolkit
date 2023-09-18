package org.readium.navigator.web

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.navigator.web.databinding.ReadiumNavigatorWebBinding
import org.readium.r2.navigator.SimplePresentation
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation

/**
 * Navigator for Web publications.
 *
 * To use this [Fragment], create a factory with `WebNavigatorFragment.createFactory()`.
 */
@OptIn(ExperimentalReadiumApi::class)
public class WebNavigatorFragment(
    override val publication: Publication,
    private val initialLocator: Locator?
) : Fragment(), VisualNavigator {

    private lateinit var currentActivity: FragmentActivity
    private lateinit var webViewServer: WebViewServer

    private var _binding: ReadiumNavigatorWebBinding? = null
    private val binding get() = _binding!!
    private val webView get() = binding.webView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentActivity = requireActivity()

        webViewServer = WebViewServer(
            currentActivity.applicationContext as Application,
            publication,
            servedAssets = listOf("readium/navigator/web/.*")
        )
        _binding = ReadiumNavigatorWebBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.setPadding(0, 0, 0, 0)
        // webView.addJavascriptInterface(webView, "Android")

        if (publication.metadata.presentation.layout == EpubLayout.FIXED) {
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.setSupportZoom(true)
            webView.settings.builtInZoomControls = true
            webView.settings.displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClientCompat() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                false

            override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
                // Do something with the event here
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // To make sure the page is properly laid out before jumping to the target locator,
                // we execute a dummy JavaScript and wait for the callback result.
                webView.evaluateJavascript("true") {}
                webView.evaluateJavascript(
                    "readium.load()\n" // '${WebViewServer.publicationBaseHref}');\n"
                ) {}
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                webViewServer.shouldInterceptRequest(request)
        }

        webView.loadUrl("https://readium/assets/readium/navigator/web/index.html")

        viewLifecycleOwner.lifecycleScope.launch {
            withStarted {
                // Restore the last locator before a configuration change (e.g. screen rotation), or the
                // initial locator when given.
                val locator = savedInstanceState?.let {
                    BundleCompat.getParcelable(
                        it,
                        "locator",
                        Locator::class.java
                    )
                }
                    ?: initialLocator
                if (locator != null) {
                    go(locator)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("locator", currentLocator.value)
        super.onSaveInstanceState(outState)
    }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        webView.evaluateJavascript(
            "window.readium.navigator.go(`${locator.toJSON()}`}, false, (ok: boolean) => {})"
        ) { }
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    // VisualNavigator

    override val publicationView: View
        get() = requireView()

    override val presentation: StateFlow<VisualNavigator.Presentation>
        get() = MutableStateFlow(
            SimplePresentation(
                org.readium.r2.navigator.preferences.ReadingProgression.LTR,
                scroll = false,
                axis = Axis.HORIZONTAL
            )
        )

    private lateinit var inputListener: InputListener

    override fun addInputListener(listener: InputListener) {
        inputListener = listener
    }

    override fun removeInputListener(listener: InputListener) {
        inputListener = listener
    }

    @Deprecated(
        "Use `presentation.value.readingProgression` instead",
        replaceWith = ReplaceWith("presentation.value.readingProgression"),
        level = DeprecationLevel.ERROR
    )
    override val readingProgression: ReadingProgression
        get() = throw NotImplementedError()

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        return false
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        return false
    }

    override val currentLocator: StateFlow<Locator> get() = _currentLocator

    private val _currentLocator = MutableStateFlow(
        initialLocator
            ?: requireNotNull(publication.locatorFromLink(publication.readingOrder.first()))
    )

    override suspend fun firstVisibleElementLocator(): Locator? {
        return null
    }

    public companion object {

        /**
         * Returns a URL to the application asset at [path], served in the web views.
         */
        public fun assetUrl(path: String): String =
            WebViewServer.assetUrl(path)
    }
}
