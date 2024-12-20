/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import org.readium.navigator.web.pager.NavigatorPager
import org.readium.navigator.web.reflowable.ReflowableResource
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.DisplayArea
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
    val paginatedVerticalPadding =
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 20.dp
            else -> 40.dp
        }

    val verticalPadding =
        if (state.layoutDelegate.settings.value.scroll) {
            0.dp
        } else {
            paginatedVerticalPadding
        }

    val backgroundColor = (
        state.layoutDelegate.settings.value.backgroundColor?.int
            ?: state.layoutDelegate.settings.value.theme.backgroundColor
        )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(Color(backgroundColor))
            .padding(windowInsets.asPaddingValues())
            .padding(vertical = verticalPadding),
        propagateMinConstraints = true
    ) {
        val viewportSize = DpSize(maxWidth, maxHeight)

        val safeDrawingPadding = windowInsets.asAbsolutePaddingValues()
        val displayArea = rememberUpdatedState(DisplayArea(viewportSize, safeDrawingPadding))

        val readingProgression =
            state.layoutDelegate.settings.value.readingProgression

        val reverseLayout =
            LocalLayoutDirection.current.toReadingProgression() != readingProgression

        val itemIndex = state.pagerState.currentPage
        val itemHref = state.readingOrder.items[itemIndex].href
        state.updateLocation(ReflowableWebLocation(itemHref))

        val coroutineScope = rememberCoroutineScope()

        NavigatorPager(
            modifier = modifier,
            state = state.pagerState,
            beyondViewportPageCount = 2,
            reverseLayout = reverseLayout,
            orientation =
            if (state.layoutDelegate.settings.value.scroll) {
                Orientation.Vertical
            } else {
                Orientation.Horizontal
            }
        ) { index ->
            ReflowableResource(
                href = state.readingOrder.items[index].href,
                publicationBaseUrl = WebViewServer.publicationBaseHref,
                webViewClient = state.webViewClient,
                displayArea = displayArea.value,
                reverseLayout = reverseLayout,
                scroll = state.layoutDelegate.settings.value.scroll,
                rsProperties = state.readiumCss.value.rsProperties,
                userProperties = state.readiumCss.value.userProperties,
                onTap = { inputListener.onTap(it, TapContext(viewportSize)) },
                onLinkActivated = { url, outerHtml ->
                    coroutineScope.launch {
                        onLinkActivated(url, outerHtml, state, hyperlinkListener)
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
