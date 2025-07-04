/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.fixedlayout.spread

import android.view.ActionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.fixedlayout.layout.SingleViewportSpread
import org.readium.navigator.web.fixedlayout.util.DisplayArea
import org.readium.navigator.web.fixedlayout.webapi.FixedSingleApi
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.webapi.FixedSingleDecorationApi
import org.readium.navigator.web.internals.webapi.FixedSingleSelectionApi
import org.readium.navigator.web.internals.webview.RelaxedWebView
import org.readium.navigator.web.internals.webview.rememberWebViewStateWithHTMLData
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorationChange
import org.readium.r2.navigator.changesByHref
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

@Composable
internal fun SingleViewportSpread(
    pagerState: PagerState,
    scrollState: SpreadScrollState,
    progression: Double,
    layoutDirection: LayoutDirection,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
    onSelectionApiChanged: (FixedSingleSelectionApi?) -> Unit,
    actionModeCallback: ActionMode.Callback?,
    state: SingleSpreadState,
    backgroundColor: Color,
    decorationTemplates: HtmlDecorationTemplates,
    decorations: Map<String, List<Decoration>>,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState = rememberWebViewStateWithHTMLData<RelaxedWebView>(
            data = state.htmlData,
            baseUrl = state.publicationBaseUrl.toString()
        )

        val scriptsLoaded = remember(webViewState.webView) {
            mutableStateOf(false)
        }

        val layoutApi = remember(webViewState.webView, scriptsLoaded.value) {
            webViewState.webView
                .takeIf { scriptsLoaded.value }
                ?.let { FixedSingleApi(it) }
        }

        LaunchedEffect(webViewState.webView) {
            val selectionApi = webViewState.webView
                ?.let { FixedSingleSelectionApi(it) { it } }
            onSelectionApiChanged(selectionApi)
        }

        LaunchedEffect(layoutApi) {
            if (layoutApi != null) {
                snapshotFlow {
                    state.fit.value
                }.onEach {
                    layoutApi.setFit(it)
                }.launchIn(this)

                snapshotFlow {
                    state.displayArea.value
                }.onEach {
                    layoutApi.setDisplayArea(it)
                }.launchIn(this)

                layoutApi.setFit(state.fit.value)
                layoutApi.setDisplayArea(state.displayArea.value)
                layoutApi.loadSpread(state.spread)
            }
        }

        LaunchedEffect(webViewState.webView, actionModeCallback) {
            webViewState.webView?.setCustomSelectionActionModeCallback(actionModeCallback)
        }

        val decorationApi = remember(webViewState.webView) { mutableStateOf<FixedSingleDecorationApi?>(null) }

        val decorations = remember(webViewState.webView) { mutableStateOf(decorations) }
            .apply { value = decorations }

        LaunchedEffect(decorationApi.value, decorations) {
            decorationApi.value?.let { decorationApi ->
                var lastDecorations = emptyMap<String, List<Decoration>>()
                snapshotFlow { decorations.value }
                    .onEach {
                        for ((group, decos) in it.entries) {
                            val lastInGroup = lastDecorations[group].orEmpty()
                            for ((_, changes) in lastInGroup.changesByHref(decos)) {
                                for (change in changes) {
                                    when (change) {
                                        is DecorationChange.Added -> {
                                            val template = decorationTemplates[change.decoration.style::class]
                                                ?: continue
                                            decorationApi.addDecoration(change.decoration, template, group)
                                        }
                                        is DecorationChange.Moved -> {}
                                        is DecorationChange.Removed -> {
                                            decorationApi.removeDecoration(change.id, group)
                                        }
                                        is DecorationChange.Updated -> {
                                            decorationApi.removeDecoration(change.decoration.id, group)
                                            val template = decorationTemplates[change.decoration.style::class]
                                                ?: continue
                                            decorationApi.addDecoration(change.decoration, template, group)
                                        }
                                    }
                                }
                            }
                        }

                        lastDecorations = it
                    }.launchIn(this)
            }
        }

        SpreadWebView(
            spreadIndex = state.index,
            pagerState = pagerState,
            state = webViewState,
            spreadScrollState = scrollState,
            client = state.webViewClient,
            progression = progression,
            onTap = onTap,
            onLinkActivated = { url, outerHtml ->
                onLinkActivated(
                    state.publicationBaseUrl.relativize(url),
                    outerHtml
                )
            },
            backgroundColor = backgroundColor,
            onScriptsLoaded = { scriptsLoaded.value = true },
            onDocumentLoadedAndSized = {
                decorationApi.value = FixedSingleDecorationApi(it)
                    .apply { registerTemplates(decorationTemplates) }
            },
            layoutDirection = layoutDirection
        )
    }
}

internal class SingleSpreadState(
    val index: Int,
    val htmlData: String,
    val publicationBaseUrl: AbsoluteUrl,
    val webViewClient: WebViewClient,
    val spread: SingleViewportSpread,
    val fit: State<Fit>,
    val displayArea: State<DisplayArea>,
) {
    val url: AbsoluteUrl =
        publicationBaseUrl.resolve(spread.page.href)
}
