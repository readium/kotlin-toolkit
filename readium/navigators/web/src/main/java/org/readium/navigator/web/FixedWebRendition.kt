/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import org.readium.navigator.common.HyperlinkListener
import org.readium.navigator.common.HyperlinkLocation
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.NullHyperlinkListener
import org.readium.navigator.common.NullInputListener
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.layout.DoubleViewportSpread
import org.readium.navigator.web.layout.SingleViewportSpread
import org.readium.navigator.web.location.FixedWebLocation
import org.readium.navigator.web.pager.NavigatorPager
import org.readium.navigator.web.spread.DoubleSpreadState
import org.readium.navigator.web.spread.DoubleViewportSpread
import org.readium.navigator.web.spread.SingleSpreadState
import org.readium.navigator.web.spread.SingleViewportSpread
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

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
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val viewportSize = DpSize(maxWidth, maxHeight)

        val safeDrawingPadding = windowInsets.asAbsolutePaddingValues()
        val displayArea = rememberUpdatedState(DisplayArea(viewportSize, safeDrawingPadding))

        val readingProgression =
            state.layoutDelegate.settings.value.readingProgression

        val reverseLayout =
            LocalLayoutDirection.current.toReadingProgression() != readingProgression

        // This is barely needed as location could be computed on the state side without any
        // data from the layout pass. I keep it so for demonstration purposes of the way the
        // reflowable navigator could fit the architecture as well.
        val spreadIndex = state.pagerState.currentPage
        val itemIndex = state.layoutDelegate.layout.value.pageIndexForSpread(spreadIndex)
        val itemHref = state.readingOrder.items[itemIndex].href
        state.updateLocation(FixedWebLocation(itemHref))

        val coroutineScope = rememberCoroutineScope()

        NavigatorPager(
            modifier = modifier,
            state = state.pagerState,
            beyondViewportPageCount = 2,
            key = { index -> state.layoutDelegate.layout.value.pageIndexForSpread(index) },
            reverseLayout = reverseLayout
        ) { index ->
            when (val spread = state.layoutDelegate.layout.value.spreads[index]) {
                is SingleViewportSpread -> {
                    val spreadState = remember {
                        SingleSpreadState(
                            htmlData = state.preloadedData.fixedSingleContent,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.layoutDelegate.fit,
                            displayArea = displayArea
                        )
                    }

                    SingleViewportSpread(
                        onTap = { inputListener.onTap(it, TapContext(viewportSize)) },
                        onLinkActivated = { url, outerHtml ->
                            coroutineScope.launch {
                                onLinkActivated(url, outerHtml, state, hyperlinkListener)
                            }
                        },
                        state = spreadState,
                        backgroundColor = backgroundColor
                    )
                }
                is DoubleViewportSpread -> {
                    val spreadState = remember {
                        DoubleSpreadState(
                            htmlData = state.preloadedData.fixedDoubleContent,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.layoutDelegate.fit,
                            displayArea = displayArea
                        )
                    }

                    DoubleViewportSpread(
                        onTap = { inputListener.onTap(it, TapContext(viewportSize)) },
                        onLinkActivated = { url, outerHtml ->
                            coroutineScope.launch {
                                onLinkActivated(url, outerHtml, state, hyperlinkListener)
                            }
                        },
                        state = spreadState,
                        backgroundColor = backgroundColor
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

private fun LayoutDirection.toReadingProgression(): ReadingProgression =
    when (this) {
        LayoutDirection.Ltr -> ReadingProgression.LTR
        LayoutDirection.Rtl -> ReadingProgression.RTL
    }

@OptIn(ExperimentalReadiumApi::class)
private suspend fun onLinkActivated(
    url: Url,
    outerHtml: String,
    state: FixedWebRenditionState,
    listener: HyperlinkListener,
) {
    val location = HyperlinkLocation(url.removeFragment())
    val isReadingOrder = state.readingOrder.indexOfHref(url.removeFragment()) != null
    val context = state.computeHyperlinkContext(url, outerHtml)
    when {
        isReadingOrder -> listener.onReadingOrderLinkActivated(location, context)
        else -> when (url) {
            is RelativeUrl -> listener.onNonLinearLinkActivated(location, context)
            is AbsoluteUrl -> listener.onExternalLinkActivated(url, context)
        }
    }
}
