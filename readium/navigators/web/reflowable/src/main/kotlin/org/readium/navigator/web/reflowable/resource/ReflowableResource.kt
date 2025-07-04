/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable.resource

import android.annotation.SuppressLint
import android.view.ActionMode
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.DecorationListener
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.util.AbsolutePaddingValues
import org.readium.navigator.web.internals.util.absolutePadding
import org.readium.navigator.web.internals.webapi.DelegatingGesturesListener
import org.readium.navigator.web.internals.webapi.DocumentStateApi
import org.readium.navigator.web.internals.webapi.GesturesApi
import org.readium.navigator.web.internals.webapi.ReflowableDecorationApi
import org.readium.navigator.web.internals.webapi.ReflowableSelectionApi
import org.readium.navigator.web.internals.webview.RelaxedWebView
import org.readium.navigator.web.internals.webview.WebView
import org.readium.navigator.web.internals.webview.WebViewScrollController
import org.readium.navigator.web.internals.webview.rememberWebViewState
import org.readium.navigator.web.reflowable.css.ReadiumCssInjector
import org.readium.navigator.web.reflowable.webapi.CssApi
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorationChange
import org.readium.r2.navigator.changesByHref
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun ReflowableResource(
    resourceState: ReflowableResourceState,
    publicationBaseUrl: AbsoluteUrl,
    webViewClient: WebViewClient,
    backgroundColor: Color,
    padding: AbsolutePaddingValues,
    scroll: Boolean,
    orientation: Orientation,
    layoutDirection: LayoutDirection,
    readiumCssInjector: ReadiumCssInjector,
    decorationTemplates: HtmlDecorationTemplates,
    decorations: ImmutableMap<String, List<Decoration>>,
    actionModeCallback: ActionMode.Callback?,
    onSelectionApiChanged: (ReflowableSelectionApi?) -> Unit,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
    onDecorationActivated: (DecorationListener.OnActivatedEvent) -> Unit,
    onProgressionChange: (Double) -> Unit,
    onDocumentResized: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState =
            rememberWebViewState<RelaxedWebView>(
                url = publicationBaseUrl.resolve(resourceState.href).toString()
            )

        val orientationState =
            rememberUpdatedState(orientation)

        val directionState =
            rememberUpdatedState(layoutDirection)

        val scriptsLoaded =
            remember(webViewState.webView) { mutableStateOf(false) }

        val contentIsLaidOut =
            remember(webViewState.webView) { mutableStateOf(false) }

        val cssApi = remember(webViewState.webView) { mutableStateOf<CssApi?>(null) }

        val decorationApi = remember(webViewState.webView) { mutableStateOf<ReflowableDecorationApi?>(null) }

        val selectionApi = remember(webViewState.webView) { mutableStateOf<ReflowableSelectionApi?>(null) }

        val decorations = remember(webViewState.webView) { mutableStateOf(decorations) }
            .apply { value = decorations }

        LaunchedEffect(decorationApi.value, decorations) {
            decorationApi.value?.let { decorationApi ->
                var lastDecorations = emptyMap<String, List<Decoration>>()
                snapshotFlow { decorations.value }
                    .onEach {
                        for ((group, decos) in it.entries) {
                            val lastInGroup = lastDecorations[group].orEmpty()
                            for ((_, changes) in lastInGroup.changesByHref(decos)) {
                                for (change in changes) {
                                    when (change) {
                                        is DecorationChange.Added -> {
                                            val template = decorationTemplates[change.decoration.style::class]
                                                ?: continue
                                            decorationApi.addDecoration(change.decoration, template, group)
                                        }
                                        is DecorationChange.Moved -> {}
                                        is DecorationChange.Removed -> {
                                            decorationApi.removeDecoration(change.id, group)
                                        }
                                        is DecorationChange.Updated -> {
                                            decorationApi.removeDecoration(change.decoration.id, group)
                                            val template = decorationTemplates[change.decoration.style::class]
                                                ?: continue
                                            decorationApi.addDecoration(change.decoration, template, group)
                                        }
                                    }
                                }
                            }
                        }

                        lastDecorations = it
                    }.launchIn(this)
            }
        }

        LaunchedEffect(webViewState.webView, onTap, onLinkActivated) {
            webViewState.webView?.let { webView ->
                GesturesApi(
                    webView = webView,
                    listener = DelegatingGesturesListener(
                        onTapDelegate = { offset ->
                            val shiftedOffset = DpOffset(
                                x = offset.x + padding.left,
                                y = offset.y + padding.top
                            )
                            onTap(TapEvent(shiftedOffset))
                        },
                        onLinkActivatedDelegate = { href, outerHtml ->
                            onLinkActivated(publicationBaseUrl.relativize(href), outerHtml)
                        },
                        onDecorationActivatedDelegate = { id, group, rect, offset ->
                            val decoration = decorations.value[group]?.firstOrNull { it.id == id }
                                ?: return@DelegatingGesturesListener

                            val shiftedOffset = DpOffset(
                                x = offset.x + padding.left,
                                y = offset.y + padding.top
                            )
                            val shiftedRect = DpRect(
                                left = rect.left + padding.left,
                                right = rect.right + padding.left,
                                top = rect.top + padding.top,
                                bottom = rect.bottom + padding.top,
                            )
                            val event = DecorationListener.OnActivatedEvent(
                                decoration = decoration,
                                group = group,
                                rect = shiftedRect,
                                offset = shiftedOffset
                            )
                            onDecorationActivated(event)
                        }
                    )
                )
            }
        }

        LaunchedEffect(webViewState.webView, scriptsLoaded, cssApi, resourceState, contentIsLaidOut) {
            webViewState.webView?.let { webView ->
                DocumentStateApi(
                    webView = webView,
                    onScriptsLoadedDelegate = {
                        scriptsLoaded.value = true
                        cssApi.value = CssApi(webView)
                        decorationApi.value = ReflowableDecorationApi(webView)
                            .apply { registerTemplates(decorationTemplates) }
                        selectionApi.value = ReflowableSelectionApi(webView) { rect: DpRect ->
                            DpRect(
                                top = rect.top + padding.top,
                                right = rect.right + padding.left,
                                bottom = rect.bottom + padding.top,
                                left = rect.left + padding.left
                            )
                        }
                    },
                    onDocumentLoadedAndSizedDelegate = {
                        Timber.d("resource ${resourceState.index} onDocumentLoadedAndResized")
                        webView.requestLayout()
                        webView.setNextLayoutListener {
                            val scrollController = WebViewScrollController(webView)
                            scrollController.moveToProgression(
                                progression = resourceState.progression,
                                snap = !scroll,
                                orientation = orientationState.value,
                                direction = layoutDirection
                            )
                            resourceState.scrollController.value = scrollController
                            Timber.d("resource ${resourceState.index} ready to scroll")
                            webView.setOnScrollChangeListener { view, scrollX, scrollY, oldScrollX, oldScrollY ->
                                onProgressionChange(
                                    scrollController.progression(
                                        orientationState.value,
                                        directionState.value
                                    )
                                )
                            }
                            contentIsLaidOut.value = true
                        }
                    },
                    onDocumentResizedDelegate = {
                        Timber.d("resource ${resourceState.index} onDocumentResized")
                        onDocumentResized.invoke()
                    }
                )
            }
        }

        LaunchedEffect(cssApi.value, readiumCssInjector) {
            cssApi.value?.setProperties(readiumCssInjector.userProperties, readiumCssInjector.rsProperties)
            // FIXME: resource is laid out again, so we should apply progression again
        }

        LaunchedEffect(decorationApi.value) {
            decorationApi.value?.registerTemplates(decorationTemplates)
        }

        LaunchedEffect(webViewState.webView, actionModeCallback) {
            webViewState.webView?.setCustomSelectionActionModeCallback(actionModeCallback)
        }

        LaunchedEffect(selectionApi, onSelectionApiChanged) {
            snapshotFlow { selectionApi.value }
                .onEach { onSelectionApiChanged(it) }
                .launchIn(this)
        }

        // Hide content before initial position is settled
        if (!contentIsLaidOut.value) {
            Box(
                modifier = Modifier
                    .background(backgroundColor)
                    .zIndex(1f)
                    .fillMaxSize(),
                content = {}
            )
        }

        // Recreate WebView when Readium CSS layout changes because injected stuff depends on it
        key(readiumCssInjector.layout) {
            WebView(
                modifier = Modifier
                    .fillMaxSize()
                    .absolutePadding(padding),
                state = webViewState,
                factory = { RelaxedWebView(it) },
                client = webViewClient,
                onCreated = { webview ->
                    webview.settings.javaScriptEnabled = true
                    webview.settings.setSupportZoom(false)
                    webview.settings.builtInZoomControls = false
                    webview.settings.displayZoomControls = false
                    webview.settings.loadWithOverviewMode = true
                    webview.settings.useWideViewPort = true
                    webview.isVerticalScrollBarEnabled = false
                    webview.isHorizontalScrollBarEnabled = false
                    webview.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    // Prevents vertical scrolling towards blank space.
                    // See https://github.com/readium/readium-css/issues/158
                    webview.setOnTouchListener(object : View.OnTouchListener {
                        @SuppressLint("ClickableViewAccessibility")
                        override fun onTouch(view: View, event: MotionEvent): Boolean {
                            return orientationState.value == Orientation.Horizontal &&
                                event.action == MotionEvent.ACTION_MOVE
                        }
                    })
                },
                onDispose = {
                    resourceState.scrollController.value = null
                    Timber.d("resource ${resourceState.index} disposed")
                }
            )
        }
    }
}
