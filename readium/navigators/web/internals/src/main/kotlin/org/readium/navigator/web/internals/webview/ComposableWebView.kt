/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup.LayoutParams
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A wrapper around the Android View WebView to provide a basic WebView composable.
 *
 * The WebView attempts to set the layoutParams based on the Compose modifier passed in. If it
 * is incorrectly sizing, use the layoutParams composable function instead.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param modifier A compose modifier
 * @param onCreated Called when the WebView is first created, this can be used to set additional
 * settings on the WebView. WebChromeClient and WebViewClient should not be set here as they will be
 * subsequently overwritten after this lambda is called.
 * @param onDispose Called when the WebView is destroyed. Provides a bundle which can be saved
 * if you need to save and restore state in this WebView.
 * @param client Provides access to WebViewClient via subclassing
 * @param chromeClient Provides access to WebChromeClient via subclassing
 */
@Composable
public fun WebView(
    state: WebViewState<WebView>,
    modifier: Modifier = Modifier,
    onCreated: (WebView) -> Unit = {},
    onDispose: (WebView) -> Unit = {},
    client: WebViewClient = remember { WebViewClient() },
    chromeClient: WebChromeClient = remember { WebChromeClient() },
) {
    WebView(
        state = state,
        factory = { WebView((it)) },
        modifier = modifier,
        onCreated = onCreated,
        onDispose = onDispose,
        client = client,
        chromeClient = chromeClient
    )
}

/**
 * A wrapper around the Android View WebView to provide a basic WebView composable.
 *
 * The WebView attempts to set the layoutParams based on the Compose modifier passed in. If it
 * is incorrectly sizing, use the layoutParams composable function instead.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param modifier A compose modifier
 * @param onCreated Called when the WebView is first created, this can be used to set additional
 * settings on the WebView. WebChromeClient and WebViewClient should not be set here as they will be
 * subsequently overwritten after this lambda is called.
 * @param onDispose Called when the WebView is destroyed. Provides a bundle which can be saved
 * if you need to save and restore state in this WebView.
 * @param client Provides access to WebViewClient via subclassing
 * @param chromeClient Provides access to WebChromeClient via subclassing
 * @param factory A WebView factory for using a custom subclass of WebView
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
public fun <T : WebView> WebView(
    state: WebViewState<T>,
    factory: ((Context) -> T),
    modifier: Modifier = Modifier,
    onCreated: (T) -> Unit = {},
    onDispose: (T) -> Unit = {},
    client: WebViewClient = remember { WebViewClient() },
    chromeClient: WebChromeClient = remember { WebChromeClient() },
) {
    BoxWithConstraints(
        modifier = modifier,
        propagateMinConstraints = true
    ) {
        // WebView changes it's layout strategy based on
        // it's layoutParams. We convert from Compose Modifier to
        // layout params here.
        val width =
            if (constraints.hasFixedWidth) {
                LayoutParams.MATCH_PARENT
            } else {
                LayoutParams.WRAP_CONTENT
            }
        val height =
            if (constraints.hasFixedHeight) {
                LayoutParams.MATCH_PARENT
            } else {
                LayoutParams.WRAP_CONTENT
            }

        val layoutParams = FrameLayout.LayoutParams(
            width,
            height
        )

        LazyRow(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                WebView(
                    state,
                    factory,
                    layoutParams,
                    Modifier.fillParentMaxSize(),
                    onCreated,
                    onDispose,
                    client,
                    chromeClient,
                )
            }
        }
    }
}

/**
 * A wrapper around the Android View WebView to provide a basic WebView composable.
 *
 * The WebView attempts to set the layoutParams based on the Compose modifier passed in. If it
 * is incorrectly sizing, use the layoutParams composable function instead.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param layoutParams A FrameLayout.LayoutParams object to custom size the underlying WebView.
 * @param modifier A compose modifier
 * @param onCreated Called when the WebView is first created, this can be used to set additional
 * settings on the WebView. WebChromeClient and WebViewClient should not be set here as they will be
 * subsequently overwritten after this lambda is called.
 * @param onDispose Called when the WebView is destroyed. Provides a bundle which can be saved
 * if you need to save and restore state in this WebView.
 * @param client Provides access to WebViewClient via subclassing
 * @param chromeClient Provides access to WebChromeClient via subclassing
 * @param factory A WebView factory for using a custom subclass of WebView
 */
@Composable
public fun <T : WebView> WebView(
    state: WebViewState<T>,
    factory: ((Context) -> T),
    layoutParams: FrameLayout.LayoutParams,
    modifier: Modifier = Modifier,
    onCreated: (T) -> Unit = {},
    onDispose: (T) -> Unit = {},
    client: WebViewClient = remember { WebViewClient() },
    chromeClient: WebChromeClient = remember { WebChromeClient() },
) {
    val webView = state.webView

    webView?.let { wv ->
        LaunchedEffect(wv, state) {
            snapshotFlow { state.content }.collect { content ->
                when (content) {
                    is WebContent.Url -> {
                        wv.loadUrl(content.url, content.additionalHttpHeaders)
                    }

                    is WebContent.Data -> {
                        wv.loadDataWithBaseURL(
                            content.baseUrl,
                            content.data,
                            content.mimeType,
                            content.encoding,
                            content.historyUrl
                        )
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { context ->
            factory(context).apply {
                this.layoutParams = layoutParams
                this.webChromeClient = chromeClient
                this.webViewClient = client
                state.webView = this
                onCreated(this)
            }
        },
        modifier = modifier,
        onRelease = {
            onDispose(it)
            state.webView = null
        }
    )
}

public sealed class WebContent {
    internal data class Url(
        val url: String,
        val additionalHttpHeaders: Map<String, String> = emptyMap(),
    ) : WebContent()

    internal data class Data(
        val data: String,
        val baseUrl: String? = null,
        val encoding: String = "utf-8",
        val mimeType: String? = null,
        val historyUrl: String? = null,
    ) : WebContent()
}

/**
 * A state holder to hold the state for the WebView. In most cases this will be remembered
 * using the rememberWebViewState(uri) function.
 */
@Stable
public class WebViewState<T : WebView>(webContent: WebContent) {
    /**
     *  The content being loaded by the WebView
     */
    public var content: WebContent by mutableStateOf(webContent)

    // An internal DisposableEffect or AndroidView onDestroy is called
    // after the state saver and so can't be used.
    public var webView: T? by mutableStateOf<T?>(null)
}

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param url The url to load in the WebView
 * @param additionalHttpHeaders Optional, additional HTTP headers that are passed to [WebView.loadUrl].
 *   Note that these headers are used for all subsequent requests of the WebView.
 */
@Composable
public fun <T : WebView> rememberWebViewState(
    url: String,
    additionalHttpHeaders: Map<String, String> = emptyMap(),
): WebViewState<T> =
    // Rather than using .apply {} here we will recreate the state, this prevents
    // a recomposition loop when the webview updates the url itself.
    remember {
        WebViewState<T>(
            WebContent.Url(
                url = url,
                additionalHttpHeaders = additionalHttpHeaders
            )
        )
    }.apply {
        this.content = WebContent.Url(
            url = url,
            additionalHttpHeaders = additionalHttpHeaders
        )
    }

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param data The uri to load in the WebView
 */
@Composable
public fun <T : WebView> rememberWebViewStateWithHTMLData(
    data: String,
    baseUrl: String? = null,
    encoding: String = "utf-8",
    mimeType: String? = null,
    historyUrl: String? = null,
): WebViewState<T> =
    remember {
        WebViewState<T>(WebContent.Data(data, baseUrl, encoding, mimeType, historyUrl))
    }.apply {
        this.content = WebContent.Data(
            data,
            baseUrl,
            encoding,
            mimeType,
            historyUrl
        )
    }
