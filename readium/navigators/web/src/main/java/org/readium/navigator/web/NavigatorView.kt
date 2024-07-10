package org.readium.navigator.web

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.readium.navigator.web.util.WebView
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.util.rememberWebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@ExperimentalReadiumApi
@Composable
public fun NavigatorView(
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
                    WebView(
                        modifier = Modifier.fillParentMaxSize(),
                        state = webViewState,
                        client = state.webViewClient,
                        onCreated = { webview ->
                            Timber.d("WebView onCreate $url")
                            webview.settings.javaScriptEnabled = true
                            webview.settings.setSupportZoom(true)
                            webview.settings.builtInZoomControls = true
                            webview.settings.displayZoomControls = false
                            webview.settings.loadWithOverviewMode = true
                            webview.settings.useWideViewPort = true
                            webview.isVerticalScrollBarEnabled = false
                            webview.isHorizontalScrollBarEnabled = false
                        },
                        onDispose = {
                            Timber.d("WebView onDispose $url")
                        }
                    )
                }
            }
        }
    }
}
