/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.spread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.layout.DoubleViewportSpread
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.FixedDoubleApi
import org.readium.navigator.web.webview.rememberWebViewStateWithHTMLData
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

@Composable
internal fun DoubleViewportSpread(
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
    state: DoubleSpreadState,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState = rememberWebViewStateWithHTMLData(
            data = state.htmlData,
            baseUrl = state.publicationBaseUrl.toString()
        )

        val scriptsLoaded = remember(webViewState.webView) {
            mutableStateOf(false)
        }

        val layoutApi = remember(webViewState.webView, scriptsLoaded.value) {
            webViewState.webView
                .takeIf { scriptsLoaded.value }
                ?.let { FixedDoubleApi(it) }
        }

        layoutApi?.let { api ->
            LaunchedEffect(api) {
                snapshotFlow {
                    state.fit.value
                }.onEach {
                    api.setFit(state.fit.value)
                }.launchIn(this)

                snapshotFlow {
                    state.displayArea.value
                }.onEach {
                    api.setDisplayArea(it)
                }.launchIn(this)

                api.loadSpread(state.spread)
            }
        }

        SpreadWebView(
            state = webViewState,
            client = state.webViewClient,
            onTap = onTap,
            onLinkActivated = { url, outerHtml ->
                onLinkActivated(
                    state.publicationBaseUrl.relativize(url),
                    outerHtml
                )
            },
            backgroundColor = backgroundColor,
            onScriptsLoaded = { scriptsLoaded.value = true }
        )
    }
}

internal class DoubleSpreadState(
    val htmlData: String,
    val publicationBaseUrl: AbsoluteUrl,
    val webViewClient: WebViewClient,
    val spread: DoubleViewportSpread,
    val fit: State<Fit>,
    val displayArea: State<DisplayArea>,
) {
    val left: AbsoluteUrl? =
        spread.leftPage?.let { publicationBaseUrl.resolve(it.href) }

    val right: AbsoluteUrl? =
        spread.rightPage?.let { publicationBaseUrl.resolve(it.href) }
}
