/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout

import android.annotation.SuppressLint
import android.view.ActionMode
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.defaultDecorationListener
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.fixedlayout.layout.DoubleViewportSpread
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
import org.readium.navigator.web.internals.util.AbsolutePaddingValues
import org.readium.navigator.web.internals.util.DisplayArea
import org.readium.navigator.web.internals.util.HyperlinkProcessor
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
    windowInsets: WindowInsets = WindowInsets.displayCutout,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    inputListener: InputListener = defaultInputListener(state.controller),
    hyperlinkListener: HyperlinkListener = defaultHyperlinkListener(controller = state.controller),
    decorationListener: DecorationListener<FixedWebDecorationLocation> = defaultDecorationListener(state.controller),
    textSelectionActionModeCallback: ActionMode.Callback? = null,
) {
    val layoutDirection =
        state.layoutDelegate.overflow.value.readingProgression.toLayoutDirection()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize(),
            propagateMinConstraints = true
        ) {
            val viewportSize = rememberUpdatedState(DpSize(maxWidth, maxHeight))

            val safeDrawingPadding = windowInsets.asAbsolutePaddingValues()

            val displayArea =
                rememberUpdatedState(DisplayArea(viewportSize.value, safeDrawingPadding))

            fun currentLocation(): FixedWebLocation {
                val spreadIndex = state.pagerState.currentPage
                val itemIndex = state.layoutDelegate.layout.value.pageIndexForSpread(spreadIndex)
                val href = state.publication.readingOrder[itemIndex].href
                val mediaType = state.publication.readingOrder[itemIndex].mediaType

                return FixedWebLocation(href, mediaType)
            }

            if (state.controller == null) {
                state.initController(location = currentLocation())
            }

            LaunchedEffect(state) {
                snapshotFlow {
                    state.pagerState.currentPage
                }.onEach {
                    state.navigationDelegate.updateLocation(currentLocation())
                }.launchIn(this)
            }

            val coroutineScope = rememberCoroutineScope()

            val inputListenerState = rememberUpdatedState(inputListener)

            val hyperlinkListenerState = rememberUpdatedState(hyperlinkListener)

            val decorationListenerState = rememberUpdatedState(decorationListener)

            val density = LocalDensity.current

            val scrollStates = remember(state, state.layoutDelegate.layout.value) {
                state.layoutDelegate.layout.value.spreads
                    .map { SpreadScrollState() }
            }

            val flingBehavior = run {
                val pagingLayoutInfo = remember(state, scrollStates, layoutDirection) {
                    FixedPagingLayoutInfo(
                        pagerState = state.pagerState,
                        pageStates = scrollStates,
                        orientation = Orientation.Horizontal,
                        direction = layoutDirection,
                        density = density
                    )
                }
                pagingFlingBehavior(pagingLayoutInfo)
            }.toFling2DBehavior(Orientation.Horizontal)

            val scrollDispatcher = remember(state, scrollStates) {
                RenditionScrollState(
                    pagerState = state.pagerState,
                    pageStates = scrollStates,
                    overflow = state.layoutDelegate.overflow
                )
            }

            LaunchedEffect(state.layoutDelegate.layout.value, state.controller) {
                state.controller?.let {
                    val currentHref = it.location.href
                    val spreadIndex = checkNotNull(
                        state.layoutDelegate.layout.value.spreadIndexForHref(currentHref)
                    )
                    state.pagerState.requestScrollToPage(spreadIndex)
                }
            }

            val spreadFlingBehavior = Scrollable2DDefaults.flingBehavior()

            val spreadNestedScrollConnection =
                remember(state.pagerState, scrollStates) {
                    SpreadNestedScrollConnection(
                        pagerState = state.pagerState,
                        resourceStates = scrollStates,
                        flingBehavior = spreadFlingBehavior
                    )
                }

            RenditionPager(
                modifier = Modifier.nestedScroll(spreadNestedScrollConnection),
                state = state.pagerState,
                scrollState = scrollDispatcher,
                flingBehavior = flingBehavior,
                orientation = Orientation.Horizontal,
                beyondViewportPageCount = 2,
                enableScroll = true,
                key = { index ->
                    val readingProgression = state.layoutDelegate.layout.value.readingProgression
                    val spread = state.layoutDelegate.layout.value.spreads[index]
                    val pages = spread.pages.map { it.index }
                    val fit = state.layoutDelegate.fit.value
                    "$readingProgression $spread $pages $fit"
                },
            ) { index ->
                val initialProgression = when {
                    index < state.pagerState.currentPage -> 1.0
                    else -> 0.0
                }

                val spread = state.layoutDelegate.layout.value.spreads[index]

                val decorations = state.decorationDelegate.decorations
                    .mapValues { it.value.filter { it.location.href in spread.pages.map { it.href } } }
                    .toImmutableMap()

                when (spread) {
                    is SingleViewportSpread -> {
                        val spreadState =
                            SingleSpreadState(
                                index = index,
                                htmlData = state.preloadedData.fixedSingleContent,
                                publicationBaseUrl = WebViewServer.Companion.publicationBaseHref,
                                webViewClient = state.webViewClient,
                                spread = spread,
                                fit = state.layoutDelegate.fit,
                                displayArea = displayArea,
                            )

                        SingleViewportSpread(
                            pagerState = state.pagerState,
                            progression = initialProgression,
                            layoutDirection = layoutDirection,
                            onTap = {
                                inputListenerState.value.onTap(
                                    it,
                                    TapContext(viewportSize.value)
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
                            onSelectionApiChanged = { state.selectionDelegate.selectionApis[index] = it },
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
                                publicationBaseUrl = WebViewServer.Companion.publicationBaseHref,
                                webViewClient = state.webViewClient,
                                spread = spread,
                                fit = state.layoutDelegate.fit,
                                displayArea = displayArea,
                            )

                        DoubleViewportSpread(
                            pagerState = state.pagerState,
                            progression = initialProgression,
                            layoutDirection = layoutDirection,
                            onTap = {
                                inputListenerState.value.onTap(
                                    it,
                                    TapContext(viewportSize.value)
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
                            onSelectionApiChanged = { state.selectionDelegate.selectionApis[index] = it },
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

@Composable
private fun WindowInsets.asAbsolutePaddingValues(): AbsolutePaddingValues {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val top = with(density) { getTop(density).toDp() }
    val right = with(density) { getRight(density, layoutDirection).toDp() }
    val bottom = with(density) { getBottom(density).toDp() }
    val left = with(density) { getLeft(density, layoutDirection).toDp() }
    return AbsolutePaddingValues(top = top, right = right, bottom = bottom, left = left)
}

@OptIn(ExperimentalReadiumApi::class)
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
