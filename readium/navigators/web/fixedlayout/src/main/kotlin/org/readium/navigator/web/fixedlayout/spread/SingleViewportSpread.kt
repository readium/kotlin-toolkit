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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.DecorationListener
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.fixedlayout.layout.SingleViewportSpread
import org.readium.navigator.web.fixedlayout.util.DisplayArea
import org.readium.navigator.web.fixedlayout.webapi.FixedSingleApi
import org.readium.navigator.web.fixedlayout.webapi.FixedSingleInitializationApi
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.webapi.ApiStateApi
import org.readium.navigator.web.internals.webapi.DelegatingApiStateListener
import org.readium.navigator.web.internals.webapi.FixedSingleDecorationApi
import org.readium.navigator.web.internals.webapi.FixedSingleSelectionApi
import org.readium.navigator.web.internals.webapi.FixedSingleSelectionListener
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
    onDecorationActivated: (DecorationListener.OnActivatedEvent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState = rememberWebViewStateWithHTMLData<RelaxedWebView>(
            data = state.htmlData,
            baseUrl = state.publicationBaseUrl.toString()
        )

        var scriptsLoaded by remember(webViewState.webView) {
            mutableStateOf(false)
        }

        LaunchedEffect(scriptsLoaded, webViewState.webView) {
            webViewState.webView
                ?.takeIf { scriptsLoaded }
                ?.let { webView ->
                    FixedSingleInitializationApi(webView)
                        .loadSpread(state.spread)
                }
        }

        var selectionListener by remember(webViewState.webView) {
            mutableStateOf<FixedSingleSelectionListener?>(null)
        }

        LaunchedEffect(webViewState.webView) {
            selectionListener = webViewState.webView?.let { FixedSingleSelectionListener(it) }
        }

        var areaApi by remember(webViewState.webView) {
            mutableStateOf<FixedSingleApi?>(null)
        }

        var selectionApi by remember(webViewState.webView) {
            mutableStateOf<FixedSingleSelectionApi?>(null)
        }

        var decorationApi by remember(webViewState.webView) {
            mutableStateOf<FixedSingleDecorationApi?>(null)
        }

        LaunchedEffect(webViewState.webView) {
            webViewState.webView?.let { webView ->
                val listener = DelegatingApiStateListener(
                    onAreaApiAvailableDelegate = {
                        areaApi = FixedSingleApi(webView)
                    },
                    onSelectionApiAvailableDelegate = {
                        selectionApi = FixedSingleSelectionApi(webView, selectionListener!!) { it }
                        onSelectionApiChanged(selectionApi)
                    },
                    onDecorationApiAvailableDelegate = {
                        decorationApi = FixedSingleDecorationApi(webView, decorationTemplates)
                    }
                )
                ApiStateApi(webView, listener)
            }
        }

        LaunchedEffect(areaApi) {
            areaApi?.let { areaApi ->
                snapshotFlow {
                    state.fit.value
                }.onEach {
                    areaApi.setFit(it)
                }.launchIn(this)

                snapshotFlow {
                    state.displayArea.value
                }.onEach {
                    areaApi.setDisplayArea(it)
                }.launchIn(this)
            }
        }

        val decorations = remember(webViewState.webView) { mutableStateOf(decorations) }
            .apply { value = decorations }

        LaunchedEffect(decorationApi, decorations) {
            decorationApi?.let { decorationApi ->
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
            onScriptsLoaded = { scriptsLoaded = true },
            onDocumentLoadedAndSized = {
            },
            layoutDirection = layoutDirection,
            onDecorationActivated = { id, group, rect, offset ->
                val decoration = decorations.value[group]?.firstOrNull { it.id == id }
                    ?: return@SpreadWebView

                val event = DecorationListener.OnActivatedEvent(
                    decoration = decoration,
                    group = group,
                    rect = rect,
                    offset = offset
                )
                onDecorationActivated(event)
            },
            actionModeCallback = actionModeCallback
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
