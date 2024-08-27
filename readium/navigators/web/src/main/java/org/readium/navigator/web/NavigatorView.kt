package org.readium.navigator.web

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import org.readium.navigator.web.javascript.PrepaginatedDoubleLayout
import org.readium.navigator.web.javascript.PrepaginatedSingleLayout
import org.readium.navigator.web.util.LoggingNestedScrollConnection
import org.readium.navigator.web.util.LoggingTargetedFlingBehavior
import org.readium.navigator.web.util.PagerNestedConnection
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.rememberWebViewStateWithHTMLData
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

    val flingBehavior = LoggingTargetedFlingBehavior(
        PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(0)
        )
    )

    HorizontalPager(
        modifier = modifier,
        userScrollEnabled = false,
        state = pagerState,
        beyondViewportPageCount = 2,
        flingBehavior = flingBehavior,
        pageNestedScrollConnection = LoggingNestedScrollConnection(
            PagerNestedConnection(pagerState, flingBehavior, Orientation.Horizontal)
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            propagateMinConstraints = true
        ) {
            val size = Size(maxWidth.value, maxHeight.value)
            Timber.d("size $size")

            val webViewState = when (val spread = state.spreads.value[it]) {
                is LayoutResolver.Spread.Double ->
                    rememberWebViewStateWithHTMLData(
                        data = state.fxlSpreadTwo,
                        baseUrl = WebViewServer.publicationBaseHref.toString(),
                        onLoaded = {
                            with(PrepaginatedDoubleLayout()) {
                                loadSpread(spread)
                                setViewport(size.width, size.height)
                            }
                        }
                    )

                is LayoutResolver.Spread.Single -> {
                    rememberWebViewStateWithHTMLData(
                        data = state.fxlSpreadOne,
                        baseUrl = WebViewServer.publicationBaseHref.toString(),
                        onLoaded = {
                            with(PrepaginatedSingleLayout()) {
                                loadSpread(spread)
                                setViewport(size.width, size.height)
                            }
                        }
                    )
                }
            }

            WebView(
                modifier = Modifier
                    .fillMaxSize(),
                state = webViewState,
                client = state.webViewClient,
                onCreated = { webview ->
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
                }
            )
        }
    }
}

private val NoOpScrollConnection = object : NestedScrollConnection {}
