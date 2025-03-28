/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.readium.navigator.common.HyperlinkListener
import org.readium.navigator.common.HyperlinkLocation
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.NullHyperlinkListener
import org.readium.navigator.common.NullInputListener
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.location.ReflowableWebLocation
import org.readium.navigator.web.pager.RenditionPager
import org.readium.navigator.web.reflowable.ReflowableResource
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

@SuppressLint("UnusedBoxWithConstraintsScope")
@ExperimentalReadiumApi
@Composable
public fun ReflowableWebRendition(
    state: ReflowableWebRenditionState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout,
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
        val viewportSize = rememberUpdatedState(DpSize(maxWidth, maxHeight))

        val readingProgression =
            state.layoutDelegate.settings.value.readingProgression

        val reverseLayout =
            LocalLayoutDirection.current.toReadingProgression() != readingProgression

        val itemIndex = state.pagerState.currentPage
        val itemHref = state.readingOrder.items[itemIndex].href
        state.updateLocation(ReflowableWebLocation(itemHref, state.currentProgression))

        val coroutineScope = rememberCoroutineScope()

        val paginatedVerticalPadding =
            when (LocalConfiguration.current.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> AbsolutePaddingValues(vertical = 20.dp)
                else -> AbsolutePaddingValues(vertical = 40.dp)
            }

        val insetsPaddingValues = windowInsets.asAbsolutePaddingValues()

        val padding =
            if (state.layoutDelegate.settings.value.scroll) {
                insetsPaddingValues
            } else {
                insetsPaddingValues + paginatedVerticalPadding
            }

        val backgroundColor = (
            state.layoutDelegate.settings.value.backgroundColor?.int
                ?: state.layoutDelegate.settings.value.theme.backgroundColor
            )

        val readyToScrollNext = remember(state.pagerState.currentPage) {
            mutableStateOf(itemIndex == state.pagerState.pageCount - 1)
        }

        val readyToScrollPrev = remember(state.pagerState.currentPage) {
            mutableStateOf(itemIndex == 0)
        }

        RenditionPager(
            modifier = modifier,
            state = state.pagerState,
            beyondViewportPageCount = 3,
            reverseLayout = reverseLayout,
            orientation = when {
                state.layoutDelegate.settings.value.verticalText -> Orientation.Horizontal
                state.layoutDelegate.settings.value.scroll -> Orientation.Vertical
                else -> Orientation.Horizontal
            }
        ) { index ->
            ReflowableResource(
                pagerState = state.pagerState,
                index = index,
                href = state.readingOrder.items[index].href,
                publicationBaseUrl = WebViewServer.publicationBaseHref,
                webViewClient = state.webViewClient,
                viewportSize = viewportSize.value,
                backgroundColor = Color(backgroundColor),
                padding = padding,
                reverseLayout = reverseLayout,
                scroll = state.layoutDelegate.settings.value.scroll,
                verticalText = state.layoutDelegate.settings.value.verticalText,
                rsProperties = state.readiumCss.value.rsProperties,
                userProperties = state.readiumCss.value.userProperties,
                layout = state.readiumCss.value.layout,
                initialProgression = when {
                    index < itemIndex -> 1.0
                    index > itemIndex -> 0.0
                    else -> state.currentProgression
                },
                stickToInitialProgression = index != itemIndex,
                enableScroll = readyToScrollNext.value && readyToScrollPrev.value,
                onTap = { tapEvent ->
                    inputListener.onTap(tapEvent, TapContext(viewportSize.value))
                },
                onLinkActivated = { url, outerHtml ->
                    coroutineScope.launch {
                        onLinkActivated(url, outerHtml, state, hyperlinkListener)
                    }
                },
                onScrollChanged = {
                    if (index == itemIndex) {
                        val itemIndex = state.pagerState.currentPage
                        val itemHref = state.readingOrder.items[itemIndex].href
                        state.updateLocation(ReflowableWebLocation(itemHref, state.currentProgression))
                    }
                },
                onReadyToScroll = {
                    when (index) {
                        itemIndex - 1 -> readyToScrollPrev.value = true
                        itemIndex + 1 -> readyToScrollNext.value = true
                        else -> {}
                    }
                }
            )
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
    state: ReflowableWebRenditionState,
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
