/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.ActionMode
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import org.readium.navigator.web.internals.gestures.toFling2DBehavior
import org.readium.navigator.web.internals.pager.RenditionPager
import org.readium.navigator.web.internals.pager.pagingFlingBehavior
import org.readium.navigator.web.internals.server.WebViewServer
import org.readium.navigator.web.internals.util.AbsolutePaddingValues
import org.readium.navigator.web.internals.util.HyperlinkProcessor
import org.readium.navigator.web.internals.util.asAbsolutePaddingValues
import org.readium.navigator.web.internals.util.rememberUpdatedRef
import org.readium.navigator.web.internals.util.symmetric
import org.readium.navigator.web.internals.util.toLayoutDirection
import org.readium.navigator.web.reflowable.layout.LayoutConstants
import org.readium.navigator.web.reflowable.resource.ReflowablePagingLayoutInfo
import org.readium.navigator.web.reflowable.resource.ReflowableResource
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

/**
 * Composes a reflowable Web publication.
 *
 * @param state the state object describing the publication to render
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@ExperimentalReadiumApi
@Composable
public fun ReflowableWebRendition(
    state: ReflowableWebRenditionState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout,
    inputListener: InputListener = defaultInputListener(state.controller),
    hyperlinkListener: HyperlinkListener = defaultHyperlinkListener(state.controller),
    decorationListener: DecorationListener<ReflowableWebDecorationLocation> = defaultDecorationListener(state.controller),
    textSelectionActionModeCallback: ActionMode.Callback? = null,
) {
    val overflowNow = state.layoutDelegate.overflow.value

    val layoutDirection =
        overflowNow.readingProgression.toLayoutDirection()

    val layoutOrientation =
        overflowNow.orientation

    val settingsNow = state.layoutDelegate.settings

    val injectorNow = state.layoutDelegate.readiumCssInjector

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize(),
            propagateMinConstraints = true
        ) {
            val viewportSize = rememberUpdatedRef(DpSize(maxWidth, maxHeight))

            state.layoutDelegate.viewportSize = viewportSize.value

            state.layoutDelegate.safeDrawing = windowInsets.asAbsolutePaddingValues()

            state.layoutDelegate.fontScale = LocalDensity.current.fontScale

            val coroutineScope = rememberCoroutineScope()

            val resourcePadding = when (overflowNow.scroll) {
                true ->
                    AbsolutePaddingValues()
                false -> {
                    val margins = when (LocalConfiguration.current.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> LayoutConstants.pageVerticalMarginsLandscape
                        else -> LayoutConstants.pageVerticalMarginsPortrait
                    }
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .union(WindowInsets(top = margins, bottom = margins))
                        .symmetric()
                        .asAbsolutePaddingValues()
                }
            }

            val flingBehavior = if (overflowNow.scroll) {
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
            }.toFling2DBehavior(layoutOrientation)

            val backgroundColor = Color(settingsNow.backgroundColor.int)

            LaunchedEffect(state.pagerState) {
                snapshotFlow {
                    state.pagerState.currentPage
                }.onEach {
                    state.updateLocation()
                }.launchIn(this)
            }

            RenditionPager(
                state = state.pagerState,
                scrollState = state.scrollState,
                flingBehavior = flingBehavior,
                beyondViewportPageCount = 3,
                orientation = layoutOrientation,
            ) { index ->
                val href = state.publication.readingOrder.items[index].href

                val decorations = state.decorationDelegate.decorations
                    .mapValues { groupDecorations -> groupDecorations.value.filter { it.location.href == href } }
                    .toImmutableMap()

                ReflowableResource(
                    resourceState = state.resourceStates[index],
                    publicationBaseUrl = WebViewServer.publicationBaseHref,
                    webViewClient = state.webViewClient,
                    backgroundColor = backgroundColor,
                    padding = resourcePadding,
                    layoutDirection = layoutDirection,
                    scroll = overflowNow.scroll,
                    orientation = layoutOrientation,
                    readiumCssInjector = injectorNow,
                    decorationTemplates = state.decorationDelegate.decorationTemplates,
                    decorations = decorations,
                    actionModeCallback = textSelectionActionModeCallback,
                    onSelectionApiChanged = { state.selectionDelegate.selectionApis[index] = it },
                    onTap = { tapEvent ->
                        inputListener.onTap(tapEvent, TapContext(viewportSize.value))
                    },
                    onLinkActivated = { url, outerHtml ->
                        coroutineScope.launch {
                            state.hyperlinkProcessor.onLinkActivated(
                                url = url,
                                outerHtml = outerHtml,
                                readingOrder = state.publication.readingOrder,
                                listener = hyperlinkListener
                            )
                        }
                    },
                    onDecorationActivated = { event ->
                        decorationListener.onDecorationActivated(event)
                    },
                    onLocationChange = {
                        state.updateLocation()
                    },
                    onDocumentResized = {
                        state.scrollState.onDocumentResized(index)
                        state.updateLocation()
                    },
                )
            }
        }
    }
}

private suspend fun HyperlinkProcessor.onLinkActivated(
    url: Url,
    outerHtml: String,
    readingOrder: ReflowableWebPublication.ReadingOrder,
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
