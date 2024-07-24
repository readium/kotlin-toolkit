package org.readium.navigator.web

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import org.readium.navigator.web.util.LoggingNestedScrollConnection
import org.readium.navigator.web.util.LoggingTargetedFlingBehavior
import org.readium.navigator.web.util.PagerNestedConnection
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.rememberWebViewState
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
    val pagerState = rememberPagerState {
        state.spreads.value.size
    }

    HorizontalPager(
        modifier = modifier,
        userScrollEnabled = true,
        state = pagerState,
        beyondViewportPageCount = 2,
        flingBehavior = LoggingTargetedFlingBehavior(
            PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(0)
            )
        ),
        pageNestedScrollConnection = LoggingNestedScrollConnection(
            PagerNestedConnection(pagerState, Orientation.Horizontal)
        )
    ) {
        when (val spread = state.spreads.value[it]) {
            is LayoutResolver.Spread.Double ->
                throw NotImplementedError()
            is LayoutResolver.Spread.Single -> {
                val url = WebViewServer.publicationBaseHref.resolve(spread.url).toString()
                val webViewState = rememberWebViewState(url)
                WebView(
                    modifier = Modifier
                        .fillMaxSize(),
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
                        /*factory = { context ->
                            LoggingWebView(context).apply {
                                this.nestedScrollDispatcher = nestedScrollDispatcher
                            }
                        }*/
                )
            }
        }
    }
}

private val NoOpScrollConnection = object : NestedScrollConnection {}
