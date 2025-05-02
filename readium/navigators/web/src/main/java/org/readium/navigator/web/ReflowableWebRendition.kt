/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.navigator.common.HyperlinkListener
import org.readium.navigator.common.HyperlinkLocation
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.gestures.toFling2DBehavior
import org.readium.navigator.web.location.ReflowableWebLocation
import org.readium.navigator.web.pager.RenditionPager
import org.readium.navigator.web.pager.pagingFlingBehavior
import org.readium.navigator.web.reflowable.ReflowablePagingLayoutInfo
import org.readium.navigator.web.reflowable.ReflowableResource
import org.readium.navigator.web.reflowable.ReflowableWebPublication
import org.readium.navigator.web.util.AbsolutePaddingValues
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
public fun ReflowableWebRendition(
    state: ReflowableWebRenditionState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout,
    inputListener: InputListener = defaultInputListener(state.controller),
    hyperlinkListener: HyperlinkListener = defaultHyperlinkListener(state.controller),
) {
    val layoutDirection =
        state.layoutDelegate.overflow.value.readingProgression.toLayoutDirection()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize(),
            propagateMinConstraints = true
        ) {
            val viewportSize = rememberUpdatedState(DpSize(maxWidth, maxHeight))

            val coroutineScope = rememberCoroutineScope()

            val resourcePadding =
                if (state.layoutDelegate.overflow.value.scroll) {
                    AbsolutePaddingValues()
                } else {
                    when (LocalConfiguration.current.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE ->
                            AbsolutePaddingValues(vertical = 20.dp)
                        else ->
                            AbsolutePaddingValues(vertical = 40.dp)
                    }
                }

            val flingBehavior = if (state.layoutDelegate.overflow.value.scroll) {
                ScrollableDefaults.flingBehavior()
            } else {
                pagingFlingBehavior(
                    ReflowablePagingLayoutInfo(
                        pagerState = state.pagerState,
                        pageStates = state.resourceStates,
                        density = LocalDensity.current,
                        direction = layoutDirection
                    )
                )
            }.toFling2DBehavior(state.layoutDelegate.orientation)

            val backgroundColor = Color(
                state.layoutDelegate.settings.value.backgroundColor?.int
                    ?: state.layoutDelegate.settings.value.theme.backgroundColor
            )

            val inputListenerState = rememberUpdatedState(inputListener)

            val hyperlinkListenerState = rememberUpdatedState(hyperlinkListener)

            val currentPageState = remember(state) { derivedStateOf { state.pagerState.currentPage } }

            fun currentLocation() =
                ReflowableWebLocation(
                    href = state.publication.readingOrder.items[currentPageState.value].href,
                    progression = state.resourceStates[currentPageState.value].progression
                )

            if (state.controller == null) {
                // Initialize controller. In the future, that should require access to a ready WebView.
                state.initController(location = currentLocation())
            }

            LaunchedEffect(state) {
                snapshotFlow {
                    state.pagerState.currentPage
                }.onEach {
                    state.updateLocation(currentLocation())
                }.launchIn(this)
            }

            RenditionPager(
                modifier = Modifier
                    // Apply background on padding
                    .background(backgroundColor)
                    // Detect taps on padding
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onTapOnPadding(it, viewportSize.value, inputListenerState.value) }
                        )
                    }
                    .windowInsetsPadding(windowInsets),
                state = state.pagerState,
                scrollState = state.scrollState,
                flingBehavior = flingBehavior,
                beyondViewportPageCount = 3,
                orientation = state.layoutDelegate.orientation,
            ) { index ->
                ReflowableResource(
                    resourceState = state.resourceStates[index],
                    publicationBaseUrl = WebViewServer.publicationBaseHref,
                    webViewClient = state.webViewClient,
                    backgroundColor = backgroundColor,
                    padding = resourcePadding,
                    layoutDirection = layoutDirection,
                    scroll = state.layoutDelegate.settings.value.scroll,
                    orientation = state.layoutDelegate.orientation,
                    readiumCssInjector = state.readiumCssInjector.value,
                    onTap = { tapEvent ->
                        inputListenerState.value.onTap(tapEvent, TapContext(viewportSize.value))
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
                    onProgressionChange = {
                        if (index == currentPageState.value) {
                            val itemHref = state.publication.readingOrder[index].href
                            val newLocation = ReflowableWebLocation(itemHref, it)
                            state.updateLocation(newLocation)
                        }
                    },
                    onDocumentResized = {
                        state.scrollState.onDocumentResized(index)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun (Density).onTapOnPadding(
    offset: Offset,
    viewportSize: DpSize,
    listener: InputListener,
) {
    if (offset.x >= 0 && offset.y >= 0) {
        val offset = DpOffset(x = offset.x.toDp(), y = offset.y.toDp())
        val event = TapEvent(offset)
        listener.onTap(event, TapContext(viewportSize))
    }
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun HyperlinkProcessor.onLinkActivated(
    url: Url,
    outerHtml: String,
    readingOrder: ReflowableWebPublication.ReadingOrder,
    listener: HyperlinkListener,
) {
    val location = HyperlinkLocation(url.removeFragment(), url.fragment)
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
