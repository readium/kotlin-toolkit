/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import org.readium.navigator.web.gestures.toFling2DBehavior
import org.readium.navigator.web.location.ReflowableWebLocation
import org.readium.navigator.web.pager.RenditionPager
import org.readium.navigator.web.pager.ScrollDispatcherImpl
import org.readium.navigator.web.reflowable.ReflowablePagingLayoutInfo
import org.readium.navigator.web.reflowable.ReflowableResource
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url
import pagingFlingBehavior

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
        modifier = modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val viewportSize = rememberUpdatedState(DpSize(maxWidth, maxHeight))

        val readingProgression =
            state.layoutDelegate.settings.value.readingProgression

        val reverseLayout =
            LocalLayoutDirection.current.toReadingProgression() != readingProgression

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

        val currentItemIndexState = remember { derivedStateOf { state.pagerState.currentPage } }

        val inputListenerState = rememberUpdatedState(inputListener)

        // First location update to trigger controller creation.
        // In the future, that should require access to the WebView.
        state.updateLocation(
            ReflowableWebLocation(
                href = state.publication.readingOrder.items[currentItemIndexState.value].href,
                progression = state.resourceStates[currentItemIndexState.value].progression.ratio
            )
        )

        val orientation = when {
            state.layoutDelegate.settings.value.verticalText -> Orientation.Horizontal
            state.layoutDelegate.settings.value.scroll -> Orientation.Vertical
            else -> Orientation.Horizontal
        }

        val density = LocalDensity.current

        val flingBehavior = if (state.layoutDelegate.settings.value.scroll) {
            ScrollableDefaults.flingBehavior()
        } else {
            val pagingLayoutInfo = remember(state) {
                ReflowablePagingLayoutInfo(
                    pagerState = state.pagerState,
                    resourceStates = state.resourceStates,
                    orientation = orientation,
                    density = density
                )
            }
            pagingFlingBehavior(pagingLayoutInfo)
        }.toFling2DBehavior(orientation)

        val scrollDispatcher = remember(state) {
            ScrollDispatcherImpl(
                pagerState = state.pagerState,
                resourceStates = state.resourceStates,
                flingBehavior = flingBehavior,
                pagerOrientation = orientation,
            )
        }

        scrollDispatcher.flingBehavior = flingBehavior
        scrollDispatcher.pagerOrientation = orientation

        RenditionPager(
            modifier = Modifier,
            state = state.pagerState,
            scrollDispatcher = scrollDispatcher,
            beyondViewportPageCount = 3,
            reverseLayout = reverseLayout,
            orientation = orientation
        ) { index ->
            val readyToScroll = ((index - 2)..(index + 2)).toList()
                .mapNotNull { state.resourceStates.getOrNull(it) }
                .all { it.scrollController.value != null }

            ReflowableResource(
                resourceState = state.resourceStates[index],
                pagerState = state.pagerState,
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
                enableScroll = readyToScroll,
                onTap = { tapEvent ->
                    inputListenerState.value.onTap(tapEvent, TapContext(viewportSize.value))
                },
                onLinkActivated = { url, outerHtml ->
                    coroutineScope.launch {
                        onLinkActivated(url, outerHtml, state, hyperlinkListener)
                    }
                },
                onScrollChanged = {
                    if (index == currentItemIndexState.value) {
                        val itemHref = state.publication.readingOrder.items[index].href
                        val newLocation = ReflowableWebLocation(itemHref, it)
                        state.updateLocation(newLocation)
                    }
                },
                onDocumentResized = {
                    scrollDispatcher.onDocumentResized(index)
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
    val isReadingOrder = state.publication.readingOrder.indexOfHref(url.removeFragment()) != null
    val context = state.computeHyperlinkContext(url, outerHtml)
    when {
        isReadingOrder -> listener.onReadingOrderLinkActivated(location, context)
        else -> when (url) {
            is RelativeUrl -> listener.onNonLinearLinkActivated(location, context)
            is AbsoluteUrl -> listener.onExternalLinkActivated(url, context)
        }
    }
}
