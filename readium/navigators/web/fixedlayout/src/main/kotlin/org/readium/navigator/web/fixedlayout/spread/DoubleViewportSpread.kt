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
import org.readium.navigator.common.DecorationChange
import org.readium.navigator.common.DecorationListener
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.changesByHref
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.navigator.web.fixedlayout.FixedWebDecoration
import org.readium.navigator.web.fixedlayout.FixedWebDecorationLocation
import org.readium.navigator.web.fixedlayout.layout.DoubleViewportSpread
import org.readium.navigator.web.fixedlayout.toWebApiDecoration
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.util.DisplayArea
import org.readium.navigator.web.internals.webapi.DelegatingFixedApiStateListener
import org.readium.navigator.web.internals.webapi.FixedApiStateApi
import org.readium.navigator.web.internals.webapi.FixedDoubleAreaApi
import org.readium.navigator.web.internals.webapi.FixedDoubleDecorationApi
import org.readium.navigator.web.internals.webapi.FixedDoubleInitializationApi
import org.readium.navigator.web.internals.webapi.FixedDoubleSelectionApi
import org.readium.navigator.web.internals.webapi.FixedDoubleSelectionListener
import org.readium.navigator.web.internals.webapi.Iframe
import org.readium.navigator.web.internals.webview.RelaxedWebView
import org.readium.navigator.web.internals.webview.rememberWebViewStateWithHTMLData
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

@Composable
internal fun DoubleViewportSpread(
    pagerState: PagerState,
    scrollState: SpreadScrollState,
    progression: Double,
    layoutDirection: LayoutDirection,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
    onSelectionApiChanged: (FixedDoubleSelectionApi?) -> Unit,
    actionModeCallback: ActionMode.Callback?,
    state: DoubleSpreadState,
    backgroundColor: Color,
    decorationTemplates: WebDecorationTemplates,
    decorations: Map<String, List<FixedWebDecoration>>,
    onDecorationActivated: (DecorationListener.OnActivatedEvent<FixedWebDecorationLocation>) -> Unit,
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
                    FixedDoubleInitializationApi(webView)
                        .loadSpread(state.spread.leftPage?.href, state.spread.rightPage?.href)
                }
        }

        var selectionListener by remember(webViewState.webView) {
            mutableStateOf<FixedDoubleSelectionListener?>(null)
        }

        LaunchedEffect(webViewState.webView) {
            selectionListener = webViewState.webView?.let { FixedDoubleSelectionListener(it) }
        }

        var areaApi by remember(webViewState.webView) {
            mutableStateOf<FixedDoubleAreaApi?>(null)
        }

        var selectionApi by remember(webViewState.webView) {
            mutableStateOf<FixedDoubleSelectionApi?>(null)
        }

        var decorationApi by remember(webViewState.webView) {
            mutableStateOf<FixedDoubleDecorationApi?>(null)
        }

        LaunchedEffect(webViewState.webView) {
            webViewState.webView?.let { webView ->
                val listener = DelegatingFixedApiStateListener(
                    onInitializationApiAvailableDelegate = {
                        scriptsLoaded = true
                    },
                    onAreaApiAvailableDelegate = {
                        areaApi = FixedDoubleAreaApi(webView)
                    },
                    onSelectionApiAvailableDelegate = {
                        selectionApi = FixedDoubleSelectionApi(webView, selectionListener!!) { it }
                        onSelectionApiChanged(selectionApi)
                    },
                    onDecorationApiAvailableDelegate = {
                        decorationApi = FixedDoubleDecorationApi(webView, decorationTemplates)
                    }
                )
                FixedApiStateApi(webView, listener)
            }
        }

        LaunchedEffect(areaApi) {
            areaApi?.let { areaApi ->
                snapshotFlow {
                    state.fit.value
                }.onEach {
                    areaApi.setFit(state.fit.value)
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
                var lastDecorations = emptyMap<String, List<FixedWebDecoration>>()
                snapshotFlow { decorations.value }
                    .onEach {
                        for ((group, decos) in it.entries) {
                            val lastInGroup = lastDecorations[group].orEmpty()
                            for ((href, changes) in lastInGroup.changesByHref(decos)) {
                                val iframe = when (href) {
                                    state.spread.leftPage?.href -> Iframe.Left
                                    state.spread.rightPage?.href -> Iframe.Right
                                    else -> continue
                                }
                                for (change in changes) {
                                    when (change) {
                                        is DecorationChange.Added -> {
                                            val template = decorationTemplates[change.decoration.style::class]
                                                ?: continue
                                            val webApiDecoration = change.decoration.toWebApiDecoration(template)
                                            decorationApi.addDecoration(webApiDecoration, iframe, group)
                                        }
                                        is DecorationChange.Moved -> {}
                                        is DecorationChange.Removed -> {
                                            decorationApi.removeDecoration(change.id, group)
                                        }
                                        is DecorationChange.Updated -> {
                                            decorationApi.removeDecoration(change.decoration.id, group)
                                            val template = decorationTemplates[change.decoration.style::class]
                                                ?: continue
                                            val webApiDecoration = change.decoration.toWebApiDecoration(template)
                                            decorationApi.addDecoration(webApiDecoration, iframe, group)
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
            progression = progression,
            layoutDirection = layoutDirection,
            client = state.webViewClient,
            onTap = onTap,
            onLinkActivated = { url, outerHtml ->
                onLinkActivated(
                    state.publicationBaseUrl.relativize(url),
                    outerHtml
                )
            },
            backgroundColor = backgroundColor,
            onDecorationActivated = { id, group, rect, offset ->
                val decoration = decorations.value[group]?.firstOrNull { it.id.value == id }
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

internal class DoubleSpreadState(
    val index: Int,
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
