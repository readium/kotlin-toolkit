/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.navigator.common.HyperlinkListener
import org.readium.navigator.common.HyperlinkLocation
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.NullHyperlinkListener
import org.readium.navigator.common.NullInputListener
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.fixed.DoubleSpreadState
import org.readium.navigator.web.fixed.DoubleViewportSpread
import org.readium.navigator.web.fixed.FixedPagingLayoutInfo
import org.readium.navigator.web.fixed.FixedWebPublication
import org.readium.navigator.web.fixed.SingleSpreadState
import org.readium.navigator.web.fixed.SingleViewportSpread
import org.readium.navigator.web.fixed.SpreadNestedScrollConnection
import org.readium.navigator.web.fixed.SpreadScrollState
import org.readium.navigator.web.gestures.Scrollable2DDefaults
import org.readium.navigator.web.gestures.toFling2DBehavior
import org.readium.navigator.web.layout.DoubleViewportSpread
import org.readium.navigator.web.layout.SingleViewportSpread
import org.readium.navigator.web.location.FixedWebLocation
import org.readium.navigator.web.pager.RenditionPager
import org.readium.navigator.web.pager.RenditionScrollState
import org.readium.navigator.web.pager.pagingFlingBehavior
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.HyperlinkProcessor
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.util.toLayoutDirection
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

@SuppressLint("UnusedBoxWithConstraintsScope")
@ExperimentalReadiumApi
@Composable
public fun FixedWebRendition(
    state: FixedWebRenditionState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    inputListener: InputListener = state.controller
        ?.let { defaultInputListener(controller = it) }
        ?: NullInputListener(),
    hyperlinkListener: HyperlinkListener =
        state.controller
            ?.let { defaultHyperlinkListener(controller = it) }
            ?: NullHyperlinkListener(),
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val viewportSize = rememberUpdatedState(DpSize(maxWidth, maxHeight))

        val safeDrawingPadding = windowInsets.asAbsolutePaddingValues()

        val displayArea = rememberUpdatedState(DisplayArea(viewportSize.value, safeDrawingPadding))

        val layoutDirection = state.layoutDelegate.settings.value
            .readingProgression
            .toLayoutDirection()

        if (state.controller == null) {
            val itemHref = state.getCurrentHref()
            state.initController(location = FixedWebLocation(itemHref))
        }

        LaunchedEffect(state) {
            snapshotFlow {
                state.pagerState.currentPage
            }.onEach {
                val itemHref = state.getCurrentHref()
                state.navigationDelegate.updateLocation(location = FixedWebLocation(itemHref))
            }.launchIn(this)
        }

        val coroutineScope = rememberCoroutineScope()

        val inputListenerState = rememberUpdatedState(inputListener)

        val hyperlinkListenerState = rememberUpdatedState(hyperlinkListener)

        val density = LocalDensity.current

        val scrollStates = remember(state, state.layoutDelegate.layout.value) {
            state.layoutDelegate.layout.value.spreads
                .map { SpreadScrollState() }
        }

        val flingBehavior = run {
            val pagingLayoutInfo = remember(state, scrollStates) {
                FixedPagingLayoutInfo(
                    pagerState = state.pagerState,
                    pageStates = scrollStates,
                    orientation = Orientation.Horizontal,
                    density = density
                )
            }
            pagingFlingBehavior(pagingLayoutInfo)
        }.toFling2DBehavior(Orientation.Horizontal)

        val scrollDispatcher = remember(state, scrollStates) {
            RenditionScrollState(
                pagerState = state.pagerState,
                pageStates = scrollStates,
                pagerOrientation = Orientation.Horizontal,
            )
        }

        LaunchedEffect(state.layoutDelegate.layout.value, state.controller) {
            state.controller?.let {
                val currentHref = it.location.value.href
                val spreadIndex = checkNotNull(state.layoutDelegate.layout.value.spreadIndexForHref(currentHref))
                state.pagerState.requestScrollToPage(spreadIndex)
            }
        }

        val readyToScroll = ((state.pagerState.currentPage - 2)..(state.pagerState.currentPage + 2)).toList()
            .mapNotNull { scrollStates.getOrNull(it) }
            .all { it.scrollController.value != null }

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
            layoutDirection = layoutDirection,
            enableScroll = readyToScroll,
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

            when (val spread = state.layoutDelegate.layout.value.spreads[index]) {
                is SingleViewportSpread -> {
                    val spreadState =
                        SingleSpreadState(
                            index = index,
                            htmlData = state.preloadedData.fixedSingleContent,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.layoutDelegate.fit,
                            displayArea = displayArea
                        )

                    SingleViewportSpread(
                        pagerState = state.pagerState,
                        progression = initialProgression,
                        onTap = { inputListenerState.value.onTap(it, TapContext(viewportSize.value)) },
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
                        state = spreadState,
                        scrollState = scrollStates[index],
                        backgroundColor = backgroundColor,
                        reverseScrollDirection = layoutDirection == LayoutDirection.Ltr
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
                        pagerState = state.pagerState,
                        progression = initialProgression,
                        onTap = { inputListenerState.value.onTap(it, TapContext(viewportSize.value)) },
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
                        state = spreadState,
                        scrollState = scrollStates[index],
                        backgroundColor = backgroundColor,
                        reverseScrollDirection = layoutDirection == LayoutDirection.Ltr
                    )
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
    val location = HyperlinkLocation(url.removeFragment())
    val isReadingOrder = readingOrder.indexOfHref(url.removeFragment()) != null
    val context = computeLinkContext(url, outerHtml)
    when {
        isReadingOrder -> listener.onReadingOrderLinkActivated(location, context)
        else -> when (url) {
            is RelativeUrl -> listener.onNonLinearLinkActivated(location, context)
            is AbsoluteUrl -> listener.onExternalLinkActivated(url, context)
        }
    }
}
