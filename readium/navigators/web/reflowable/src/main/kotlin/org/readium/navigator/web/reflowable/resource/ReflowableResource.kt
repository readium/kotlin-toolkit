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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.DecorationChange
import org.readium.navigator.common.DecorationListener
import org.readium.navigator.common.Progression
import org.readium.navigator.common.TapEvent
import org.readium.navigator.common.changesByHref
import org.readium.navigator.web.common.WebDecorationTemplate
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.util.AbsolutePaddingValues
import org.readium.navigator.web.internals.util.absolutePadding
import org.readium.navigator.web.internals.util.getValue
import org.readium.navigator.web.internals.util.rememberUpdatedRef
import org.readium.navigator.web.internals.util.shift
import org.readium.navigator.web.internals.webapi.Decoration
import org.readium.navigator.web.internals.webapi.DelegatingDocumentApiListener
import org.readium.navigator.web.internals.webapi.DelegatingGesturesListener
import org.readium.navigator.web.internals.webapi.DelegatingReflowableApiStateListener
import org.readium.navigator.web.internals.webapi.DocumentStateApi
import org.readium.navigator.web.internals.webapi.GesturesApi
import org.readium.navigator.web.internals.webapi.ReadiumCssApi
import org.readium.navigator.web.internals.webapi.ReflowableApiStateApi
import org.readium.navigator.web.internals.webapi.ReflowableDecorationApi
import org.readium.navigator.web.internals.webapi.ReflowableSelectionApi
import org.readium.navigator.web.internals.webview.RelaxedWebView
import org.readium.navigator.web.internals.webview.WebView
import org.readium.navigator.web.internals.webview.WebViewScrollController
import org.readium.navigator.web.internals.webview.rememberWebViewState
import org.readium.navigator.web.reflowable.ReflowableWebDecoration
import org.readium.navigator.web.reflowable.ReflowableWebDecorationCssSelectorLocation
import org.readium.navigator.web.reflowable.ReflowableWebDecorationLocation
import org.readium.navigator.web.reflowable.ReflowableWebDecorationTextQuoteLocation
import org.readium.navigator.web.reflowable.css.ReadiumCssInjector
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
    decorationTemplates: WebDecorationTemplates,
    decorations: ImmutableMap<String, List<ReflowableWebDecoration>>,
    actionModeCallback: ActionMode.Callback?,
    onSelectionApiChanged: (ReflowableSelectionApi?) -> Unit,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
    onDecorationActivated: (DecorationListener.OnActivatedEvent<ReflowableWebDecorationLocation>) -> Unit,
    onProgressionChange: (Progression) -> Unit,
    onDocumentResized: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState = rememberWebViewState<RelaxedWebView>(
            url = publicationBaseUrl.resolve(resourceState.href).toString()
        )

        var documentStateApi by remember(webViewState.webView) {
            mutableStateOf<DocumentStateApi?>(null)
        }

        var gesturesApi by remember(webViewState.webView) {
            mutableStateOf<GesturesApi?>(null)
        }

        LaunchedEffect(webViewState.webView) {
            webViewState.webView?.let { webView ->
                gesturesApi = GesturesApi(webView)
                documentStateApi = DocumentStateApi(webView)
            }
        }

        var cssApi by remember(webViewState.webView) {
            mutableStateOf<ReadiumCssApi?>(null)
        }

        var decorationApi by remember(webViewState.webView) {
            mutableStateOf<ReflowableDecorationApi?>(null)
        }

        var selectionApi by remember(webViewState.webView) {
            mutableStateOf<ReflowableSelectionApi?>(null)
        }

        val decorations = remember(webViewState.webView) { mutableStateOf(decorations) }
            .apply { value = decorations }

        val showPlaceholder =
            remember(webViewState.webView) { mutableStateOf(true) }

        val paddingShift = DpOffset(padding.left, padding.top)

        val onSelectionApiChangedRef by rememberUpdatedRef(onSelectionApiChanged)

        LaunchedEffect(webViewState.webView, padding) {
            webViewState.webView?.let { webView ->
                val listener = DelegatingReflowableApiStateListener(
                    onCssApiAvailableDelegate = {
                        cssApi = ReadiumCssApi(webView)
                    },
                    onSelectionApiAvailableDelegate = {
                        selectionApi = ReflowableSelectionApi(webView) { it.shift(paddingShift) }
                        onSelectionApiChangedRef(selectionApi)
                    },
                    onDecorationApiAvailableDelegate = {
                        decorationApi = ReflowableDecorationApi(webView, decorationTemplates)
                    }
                )
                ReflowableApiStateApi(webView, listener)
            }
        }

        LaunchedEffect(
            documentStateApi,
            webViewState.webView,
            resourceState,
            showPlaceholder,
            orientation,
            layoutDirection
        ) {
            webViewState.webView?.let { webView ->
                documentStateApi?.let { documentStateApi ->
                    documentStateApi.listener = DelegatingDocumentApiListener(
                        onDocumentLoadedAndSizedDelegate = {
                            Timber.d("resource ${resourceState.index} onDocumentLoadedAndResized")
                            webView.requestLayout()
                            webView.setNextLayoutListener {
                                val scrollController = WebViewScrollController(webView)
                                scrollController.moveToProgression(
                                    progression = resourceState.progression.value,
                                    snap = !scroll,
                                    orientation = orientation,
                                    direction = layoutDirection
                                )
                                resourceState.scrollController.value = scrollController
                                Timber.d("resource ${resourceState.index} ready to scroll")
                                webView.setOnScrollChangeListener { view, scrollX, scrollY, oldScrollX, oldScrollY ->
                                    scrollController.progression(
                                        orientation,
                                        layoutDirection
                                    )?.let { onProgressionChange(Progression(it)!!) }
                                }
                                showPlaceholder.value = false
                            }
                        },
                        onDocumentResizedDelegate = {
                            Timber.d("resource ${resourceState.index} onDocumentResized")
                            onDocumentResized.invoke()
                        }
                    )
                }
            }
        }

        LaunchedEffect(gesturesApi, onTap, onLinkActivated, padding) {
            gesturesApi?.let { gesturesApi ->
                gesturesApi.listener = DelegatingGesturesListener(
                    onTapDelegate = { offset ->
                        val shiftedOffset = offset + paddingShift
                        onTap(TapEvent(shiftedOffset))
                    },
                    onLinkActivatedDelegate = { href, outerHtml ->
                        onLinkActivated(publicationBaseUrl.relativize(href), outerHtml)
                    },
                    onDecorationActivatedDelegate = { id, group, rect, offset ->
                        val decoration = decorations.value[group]?.firstOrNull { it.id.value == id }
                            ?: return@DelegatingGesturesListener

                        val event = DecorationListener.OnActivatedEvent<ReflowableWebDecorationLocation>(
                            decoration = decoration,
                            group = group,
                            rect = rect.shift(paddingShift),
                            offset = offset + paddingShift
                        )
                        onDecorationActivated(event)
                    }
                )
            }
        }

        LaunchedEffect(decorationApi, decorations) {
            decorationApi?.let { decorationApi ->
                var lastDecorations = emptyMap<String, List<ReflowableWebDecoration>>()
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
                                            decorationApi.addDecoration(change.decoration.toWebApiDecoration(template), group)
                                        }
                                        is DecorationChange.Moved -> {}
                                        is DecorationChange.Removed -> {
                                            decorationApi.removeDecoration(change.id, group)
                                        }
                                        is DecorationChange.Updated -> {
                                            decorationApi.removeDecoration(change.decoration.id, group)
                                            val template = decorationTemplates[change.decoration.style::class]
                                                ?: continue
                                            decorationApi.addDecoration(change.decoration.toWebApiDecoration(template), group)
                                        }
                                    }
                                }
                            }
                        }

                        lastDecorations = it
                    }.launchIn(this)
            }
        }

        LaunchedEffect(cssApi, readiumCssInjector) {
            val cssProperties = readiumCssInjector.userProperties.toCssProperties() +
                readiumCssInjector.rsProperties.toCssProperties()
            cssApi?.setProperties(cssProperties)
            // FIXME: resource is laid out again, so we should apply progression again
        }

        // Hide content before initial position is settled
        if (showPlaceholder.value) {
            Box(
                modifier = Modifier
                    .background(backgroundColor)
                    .zIndex(1f)
                    .fillMaxSize(),
                content = {}
            )
        }

        LaunchedEffect(webViewState.webView, actionModeCallback) {
            webViewState.webView?.setCustomSelectionActionModeCallback(actionModeCallback)
        }

        val orientationRef by rememberUpdatedRef(orientation)

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
                            return orientationRef == Orientation.Horizontal &&
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

private fun ReflowableWebDecoration.toWebApiDecoration(
    template: WebDecorationTemplate,
): Decoration {
    val element = template.element(style)
    val cssSelector = when (location) {
        is ReflowableWebDecorationCssSelectorLocation ->
            (location as ReflowableWebDecorationCssSelectorLocation).cssSelector
        is ReflowableWebDecorationTextQuoteLocation ->
            (location as ReflowableWebDecorationTextQuoteLocation).cssSelector
    }
    val textQuote = (location as? ReflowableWebDecorationTextQuoteLocation)?.textQuote

    return Decoration(
        id = id,
        style = style,
        element = element,
        cssSelector = cssSelector,
        textQuote = textQuote
    )
}
