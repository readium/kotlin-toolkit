/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import androidx.compose.ui.unit.DpRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.readium.navigator.web.internals.webview.evaluateJavaScriptSuspend

public class SelectionApi(
    private val webView: WebView,
    private val adjustRect: (DpRect) -> DpRect,
) {

    private val json = Json { ignoreUnknownKeys = true }

    public suspend fun getCurrentSelection(): Selection? =
        withContext(Dispatchers.Main) {
            getCurrentSelectionUnsafe()
        }

    private suspend fun getCurrentSelectionUnsafe(): Selection? {
        val script = "selection.getCurrentSelection()"
        val result = webView.evaluateJavaScriptSuspend(script)
        val selection = json.decodeFromString<JsonSelection?>(result)
            ?.toSelection()
            ?: return null

        return selection.copy(
            selectionRect = adjustRect(selection.selectionRect)
        )
    }
}

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
