package org.readium.navigator.web

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import org.readium.navigator.web.util.LoggingNestedScrollConnection
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
        modifier = modifier/*.nestedScroll(object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                Timber.d("onPreScroll $available")
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                Timber.d("onPostScroll $consumed $available")
               return Offset(pagerState.dispatchRawDelta(available.x), 0f)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return available


        })*/,
        userScrollEnabled = true,
        state = pagerState,
        beyondViewportPageCount = 2,
        pageNestedScrollConnection =
        LoggingNestedScrollConnection(PagerNestedConnection(pagerState, Orientation.Horizontal))
    ) {
        when (val it = state.spreads.value[it]) {
            is LayoutResolver.Spread.Double ->
                throw NotImplementedError()
            is LayoutResolver.Spread.Single -> {
                val url = WebViewServer.publicationBaseHref.resolve(it.url).toString()
                val webViewState = rememberWebViewState(url)
                val nestedScrollDispatcher = NestedScrollDispatcher()
                WebView(
                    modifier = Modifier.fillMaxSize(),
                           /* .nestedScroll(object : NestedScrollConnection {

                                override suspend fun onPostFling(
                                    consumed: Velocity,
                                    available: Velocity
                                ): Velocity {
                                    return available
                                }
                            }),*/
                    // .nestedScroll(NoOpScrollConnection, nestedScrollDispatcher),
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
