/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalUuidApi::class)

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import androidx.compose.ui.unit.DpRect
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.readium.navigator.web.internals.webview.evaluateJavaScriptSuspend

public class ReflowableSelectionApi(
    private val webView: WebView,
    private val adjustRect: (DpRect) -> DpRect,
) {

    public suspend fun getCurrentSelection(): Selection? =
        withContext(Dispatchers.Main) {
            getCurrentSelectionUnsafe()
        }

    public fun clearSelection() {
        val script = "selection.clearSelection()"
        webView.evaluateJavascript(script) {}
    }

    private suspend fun getCurrentSelectionUnsafe(): Selection? {
        val script = "selection.getCurrentSelection()"
        val result = webView.evaluateJavaScriptSuspend(script)
        val selection = Json.decodeFromString<JsonSelection?>(result)
            ?.toSelection()
            ?: return null

        return selection.copy(
            selectionRect = adjustRect(selection.selectionRect)
        )
    }
}

public sealed interface FixedSelectionApi

public class FixedSingleSelectionApi(
    private val webView: WebView,
    private val adjustRect: (DpRect) -> DpRect,
) : FixedSelectionApi {

    init {
        webView.addJavascriptInterface(this, "singleSelectionListener")
    }

    private val requests = mutableMapOf<String, Continuation<Selection?>>()

    public fun clearSelection() {
        val script = "singleSelection.clearSelection()"
        webView.evaluateJavascript(script) {}
    }

    public suspend fun getCurrentSelection(): Selection? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val requestId = Uuid.random().toString()
                requests[requestId] = cont
                val script = "singleSelection.requestSelection(${requestId.toJavaScriptLiteral()})"
                webView.evaluateJavascript(script) {}
            }
        }

    @android.webkit.JavascriptInterface
    public fun onSelectionAvailable(requestId: String, selection: String) {
        val cont = requests.remove(requestId) ?: return

        val selection = Json.decodeFromString<JsonSelection?>(selection)
            ?.toSelection()

        val adjustedSelection = selection?.let { selection ->
            selection.copy(selectionRect = adjustRect(selection.selectionRect))
        }

        cont.resume(adjustedSelection)
    }
}

public class FixedDoubleSelectionApi(
    private val webView: WebView,
    private val adjustRect: (DpRect) -> DpRect,
) : FixedSelectionApi {

    init {
        webView.addJavascriptInterface(this, "doubleSelectionListener")
    }

    private val requests = mutableMapOf<String, Continuation<SelectionWithIframe?>>()

    public fun clearSelection() {
        val script = "doubleSelection.clearSelection()"
        webView.evaluateJavascript(script) {}
    }

    public suspend fun getCurrentSelection(): SelectionWithIframe? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val requestId = Uuid.random().toString()
                requests[requestId] = cont
                val script = "doubleSelection.requestSelection(${requestId.toJavaScriptLiteral()})"
                webView.evaluateJavascript(script) {}
            }
        }

    @android.webkit.JavascriptInterface
    public fun onSelectionAvailable(requestId: String, iframe: String, selection: String) {
        val cont = requests.remove(requestId) ?: return

        val selection = Json.decodeFromString<JsonSelection?>(selection)
            ?.toSelection()

        val adjustedSelection = selection?.let { selection ->
            selection.copy(selectionRect = adjustRect(selection.selectionRect))
        }

        val iframe = requireNotNull(Iframe.get(iframe))

        val result = adjustedSelection?.let { SelectionWithIframe(iframe, it) }

        cont.resume(result)
    }
}

public data class SelectionWithIframe(
    val iframe: Iframe,
    val selection: Selection,
)

public data class Selection(
    public val selectedText: String,
    public val textBefore: String,
    public val textAfter: String,
    public val selectionRect: DpRect,
)

@Serializable
private data class JsonSelection(
    val selectedText: String,
    val textBefore: String,
    val textAfter: String,
    val selectionRect: JsonRect,
) {
    fun toSelection() =
        Selection(
            selectedText = selectedText,
            textBefore = textBefore,
            textAfter = textAfter,
            selectionRect = selectionRect.toDpRect()
        )
}
