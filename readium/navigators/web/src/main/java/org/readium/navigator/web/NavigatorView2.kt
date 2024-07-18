package org.readium.navigator.web

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.webview.rememberWebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@ExperimentalReadiumApi
@Composable
public fun NavigatorView2(
    modifier: Modifier,
    state: NavigatorState
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LazyRow(
        modifier = modifier,
        userScrollEnabled = true,
        state = listState,
        flingBehavior = flingBehavior
    ) {
        items(state.spreads.value) {
            when (it) {
                is LayoutResolver.Spread.Double ->
                    throw NotImplementedError()
                is LayoutResolver.Spread.Single -> {
                    val url = WebViewServer.publicationBaseHref.resolve(it.url).toString()
                    val webViewState = rememberWebViewState(url)

                    AndroidView(
                        factory = { context ->
                            android.webkit.WebView(context).apply {
                                Timber.d("WebView onCreate $url")
                                settings.javaScriptEnabled = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false
                                webViewClient = object : WebViewClient() {

                                    override fun shouldInterceptRequest(
                                        view: WebView,
                                        request: WebResourceRequest
                                    ): WebResourceResponse? {
                                        return state.webViewServer.shouldInterceptRequest(request)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize(),
                        update = { webView -> webView.loadUrl(url) },
                        onReset = { webView ->
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.clearHistory()
                        }
                    )
                }
            }
        }
    }
}
