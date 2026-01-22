/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.fixedlayout

import android.annotation.SuppressLint
import android.view.ActionMode
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.navigator.common.DecorationListener
import org.readium.navigator.common.HyperlinkListener
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.Position
import org.readium.navigator.common.Progression
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.defaultDecorationListener
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.fixedlayout.layout.DoubleViewportSpread
import org.readium.navigator.web.fixedlayout.layout.Layout
import org.readium.navigator.web.fixedlayout.layout.SingleViewportSpread
import org.readium.navigator.web.fixedlayout.spread.DoubleSpreadState
import org.readium.navigator.web.fixedlayout.spread.DoubleViewportSpread
import org.readium.navigator.web.fixedlayout.spread.FixedPagingLayoutInfo
import org.readium.navigator.web.fixedlayout.spread.SingleSpreadState
import org.readium.navigator.web.fixedlayout.spread.SingleViewportSpread
import org.readium.navigator.web.fixedlayout.spread.SpreadNestedScrollConnection
import org.readium.navigator.web.fixedlayout.spread.SpreadScrollState
import org.readium.navigator.web.internals.gestures.Scrollable2DDefaults
import org.readium.navigator.web.internals.gestures.toFling2DBehavior
import org.readium.navigator.web.internals.pager.RenditionPager
import org.readium.navigator.web.internals.pager.RenditionScrollState
import org.readium.navigator.web.internals.pager.pagingFlingBehavior
import org.readium.navigator.web.internals.server.WebViewServer
import org.readium.navigator.web.internals.util.DisplayArea
import org.readium.navigator.web.internals.util.HyperlinkProcessor
import org.readium.navigator.web.internals.util.asAbsolutePaddingValues
import org.readium.navigator.web.internals.util.toLayoutDirection
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

/**
 * Composes a fixed Web publication.
 *
 * @param state the state object describing the publication to render
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@ExperimentalReadiumApi
@Composable
public fun FixedWebRendition(
    state: FixedWebRenditionState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    windowInsets: WindowInsets = WindowInsets.displayCutout,
    inputListener: InputListener = defaultInputListener(state.controller),
    hyperlinkListener: HyperlinkListener = defaultHyperlinkListener(state.controller),
    decorationListener: DecorationListener<FixedWebDecorationLocation> = defaultDecorationListener(state.controller),
    textSelectionActionModeCallback: ActionMode.Callback? = null,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val density = LocalDensity.current

        val coroutineScope = rememberCoroutineScope()

        val pagerStateNow = state.pagerState.value

        val layoutNow = state.layoutDelegate.layout.value

        val selectionDelegateNow = state.selectionDelegate

        val layoutDirectionNow =
            state.layoutDelegate.overflow.value.readingProgression.toLayoutDirection()

        val displayArea = rememberUpdatedState(
            DisplayArea(
                viewportSize = DpSize(maxWidth, maxHeight),
                safeDrawingPadding = windowInsets.asAbsolutePaddingValues()
            )
        )

        val inputListenerState = rememberUpdatedState(inputListener)

        val hyperlinkListenerState = rememberUpdatedState(hyperlinkListener)

        val decorationListenerState = rememberUpdatedState(decorationListener)

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionNow) {
            if (state.controller == null) {
                val currentLocation = currentLocation(layoutNow, pagerStateNow, state.publication)
                state.initController(location = currentLocation)
            }

            val scrollStates: List<SpreadScrollState> = remember(layoutNow) {
                layoutNow.spreads.map { SpreadScrollState() }
            }

            val flingBehavior = run {
                val pagingLayoutInfo = remember(density, pagerStateNow, scrollStates, layoutDirectionNow) {
                    FixedPagingLayoutInfo(
                        pagerState = pagerStateNow,
                        pageStates = scrollStates,
                        orientation = Orientation.Horizontal,
                        direction = layoutDirectionNow,
                        density = density
                    )
                }
                pagingFlingBehavior(pagingLayoutInfo)
            }.toFling2DBehavior(Orientation.Horizontal)

            val scrollDispatcher = remember(state, pagerStateNow, scrollStates) {
                RenditionScrollState(
                    pagerState = pagerStateNow,
                    pageStates = scrollStates,
                    overflow = state.layoutDelegate.overflow
                )
            }

            val spreadFlingBehavior = Scrollable2DDefaults.flingBehavior()

            val spreadNestedScrollConnection = remember(pagerStateNow, scrollStates) {
                SpreadNestedScrollConnection(
                    pagerState = pagerStateNow,
                    resourceStates = scrollStates,
                    flingBehavior = spreadFlingBehavior
                )
            }

            LaunchedEffect(pagerStateNow, layoutNow) {
                snapshotFlow {
                    pagerStateNow.currentPage
                }.onEach {
                    val currentLocation = currentLocation(layoutNow, pagerStateNow, state.publication)
                    state.navigationDelegate.updateLocation(currentLocation)
                }.launchIn(this)
            }

            RenditionPager(
                modifier = Modifier.nestedScroll(spreadNestedScrollConnection),
                state = pagerStateNow,
                scrollState = scrollDispatcher,
                flingBehavior = flingBehavior,
                orientation = Orientation.Horizontal,
                beyondViewportPageCount = 2,
                enableScroll = true
            ) { index ->
                val initialProgression = when {
                    index < pagerStateNow.currentPage -> 1.0
                    else -> 0.0
                }

                val spread = layoutNow.spreads[index]

                val decorations = state.decorationDelegate.decorations
                    .mapValues { groupDecorations ->
                        groupDecorations.value.filter { spread.contains(it.location.href) }
                    }.toImmutableMap()

                when (spread) {
                    is SingleViewportSpread -> {
                        val spreadState =
                            SingleSpreadState(
                                index = index,
                                htmlData = state.preloadedData.fixedSingleContent,
                                publicationBaseUrl = WebViewServer.publicationBaseHref,
                                webViewClient = state.webViewClient,
                                spread = spread,
                                fit = state.layoutDelegate.fit,
                                displayArea = displayArea,
                            )

                        SingleViewportSpread(
                            pagerState = pagerStateNow,
                            progression = initialProgression,
                            layoutDirection = layoutDirectionNow,
                            onTap = {
                                inputListenerState.value.onTap(
                                    it,
                                    TapContext(displayArea.value.viewportSize)
                                )
                            },
                            onLinkActivated = { url, outerHtml ->
                                coroutineScope.launch {
                                    state.hyperlinkProcessor.onLinkActivated(
                                        url = url,
                                        outerHtml = outerHtml,
                                        readingOrder = state.publication.readingOrder,
                                        listener = hyperlinkListenerState.value
                                    )
                                }
                            },
                            actionModeCallback = textSelectionActionModeCallback,
                            onSelectionApiChanged = { selectionDelegateNow.selectionApis[index] = it },
                            state = spreadState,
                            scrollState = scrollStates[index],
                            backgroundColor = backgroundColor,
                            decorationTemplates = state.decorationDelegate.decorationTemplates,
                            decorations = decorations,
                            onDecorationActivated = { event ->
                                decorationListenerState.value.onDecorationActivated(event)
                            },
                        )
                    }

                    is DoubleViewportSpread -> {
                        val spreadState =
                            DoubleSpreadState(
                                index = index,
                                htmlData = state.preloadedData.fixedDoubleContent,
                                publicationBaseUrl = WebViewServer.publicationBaseHref,
                                webViewClient = state.webViewClient,
                                spread = spread,
                                fit = state.layoutDelegate.fit,
                                displayArea = displayArea,
                            )

                        DoubleViewportSpread(
                            pagerState = pagerStateNow,
                            progression = initialProgression,
                            layoutDirection = layoutDirectionNow,
                            onTap = {
                                inputListenerState.value.onTap(
                                    it,
                                    TapContext(displayArea.value.viewportSize)
                                )
                            },
                            onLinkActivated = { url, outerHtml ->
                                coroutineScope.launch {
                                    state.hyperlinkProcessor.onLinkActivated(
                                        url = url,
                                        outerHtml = outerHtml,
                                        readingOrder = state.publication.readingOrder,
                                        listener = hyperlinkListenerState.value
                                    )
                                }
                            },
                            actionModeCallback = textSelectionActionModeCallback,
                            onSelectionApiChanged = { selectionDelegateNow.selectionApis[index] = it },
                            state = spreadState,
                            scrollState = scrollStates[index],
                            backgroundColor = backgroundColor,
                            decorationTemplates = state.decorationDelegate.decorationTemplates,
                            decorations = decorations,
                            onDecorationActivated = { event ->
                                decorationListenerState.value.onDecorationActivated(event)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun currentLocation(
    layout: Layout,
    pagerState: PagerState,
    publication: FixedWebPublication,
): FixedWebLocation {
    val currentSpreadIndex = pagerState.currentPage
    val itemIndex = layout.pageIndexForSpread(currentSpreadIndex)
    val href = publication.readingOrder[itemIndex].href
    val mediaType = publication.readingOrder[itemIndex].mediaType
    val position = Position(itemIndex + 1)!!
    val totalProgression = Progression(currentSpreadIndex / layout.spreads.size.toDouble())!!
    return FixedWebLocation(href, position, totalProgression, mediaType)
}

private suspend fun HyperlinkProcessor.onLinkActivated(
    url: Url,
    outerHtml: String,
    readingOrder: FixedWebPublication.ReadingOrder,
    listener: HyperlinkListener,
) {
    val isReadingOrder = readingOrder.indexOfHref(url.removeFragment()) != null
    val context = computeLinkContext(url, outerHtml)
    when {
        isReadingOrder -> listener.onReadingOrderLinkActivated(url, context)
        else -> when (url) {
            is RelativeUrl -> listener.onNonLinearLinkActivated(url, context)
            is AbsoluteUrl -> listener.onExternalLinkActivated(url, context)
        }
    }
}
