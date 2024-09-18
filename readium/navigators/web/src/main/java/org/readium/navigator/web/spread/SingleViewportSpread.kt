package org.readium.navigator.web.spread

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.web.layout.SingleViewportSpread
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.PrepaginatedSingleApi
import org.readium.navigator.web.webview.LoadingState
import org.readium.navigator.web.webview.rememberWebViewStateWithHTMLData
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class)
@Composable
internal fun SingleSpread(
    state: SingleSpreadState
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState = rememberWebViewStateWithHTMLData(
            data = state.htmlData,
            baseUrl = state.publicationBaseUrl.toString()
        )

        val webApi = remember(webViewState.webView, webViewState.loadingState) {
            webViewState.webView
                ?.takeIf { webViewState.loadingState is LoadingState.Finished }
                ?.let {
                    PrepaginatedSingleApi(it)
                }
        }

        webApi?.let { api ->
            LaunchedEffect(api) {
                snapshotFlow {
                    state.fit.value
                }.onEach {
                    api.setFit(state.fit.value)
                }.launchIn(this)

                snapshotFlow {
                    state.viewport.value
                }.onEach {
                    val (width, height) = state.viewport.value
                    api.setViewport(width, height)
                }.launchIn(this)

                api.loadSpread(state.spread)
            }
        }

        SpreadWebView(
            state = webViewState,
            client = state.webViewClient
        )
    }
}

@OptIn(ExperimentalReadiumApi::class)
internal class SingleSpreadState(
    val htmlData: String,
    val publicationBaseUrl: AbsoluteUrl,
    val webViewClient: WebViewClient,
    val spread: SingleViewportSpread,
    val fit: State<Fit>,
    val viewport: State<Size>
) {
    val url: AbsoluteUrl =
        publicationBaseUrl.resolve(spread.page.href)
}
