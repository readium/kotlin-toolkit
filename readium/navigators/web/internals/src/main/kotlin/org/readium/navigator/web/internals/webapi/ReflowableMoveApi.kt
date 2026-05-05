/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import androidx.compose.foundation.gestures.Orientation
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.readium.navigator.common.CssSelector
import org.readium.navigator.common.HtmlId
import org.readium.navigator.common.Progression
import org.readium.navigator.common.TextAnchor
import org.readium.navigator.web.internals.webview.evaluateJavaScriptSuspend
import org.readium.r2.shared.ExperimentalReadiumApi

public class ReflowableMoveApi(
    private val webView: WebView,
) {

    public suspend fun getOffsetForLocation(
        progression: Progression? = null,
        htmlId: HtmlId? = null,
        cssSelector: CssSelector? = null,
        textAnchor: TextAnchor? = null,
        orientation: Orientation,
    ): Int? =
        withContext(Dispatchers.Main) {
            getOffsetForLocationUnsafe(progression, htmlId, cssSelector, textAnchor, orientation)
        }

    private suspend fun getOffsetForLocationUnsafe(
        progression: Progression?,
        htmlId: HtmlId?,
        cssSelector: CssSelector? = null,
        textAnchor: TextAnchor? = null,
        orientation: Orientation,
    ): Int? {
        val jsonLocation = JsonLocation(
            progression = progression?.value,
            htmlId = htmlId?.value,
            cssSelector = cssSelector?.value,
            textBefore = textAnchor?.textBefore,
            textAfter = textAnchor?.textAfter
        )
        val locationAsLiteral = Json.encodeToString(jsonLocation).toJavaScriptLiteral()
        val vertical = orientation == Orientation.Vertical
        val script = "move.getOffsetForLocation($locationAsLiteral, $vertical)"
        val result = webView.evaluateJavaScriptSuspend(script)
        return result.toDoubleOrNull()?.roundToInt()
    }
}

@Serializable
private data class JsonLocation(
    val progression: Double?,
    val htmlId: String?,
    val cssSelector: String?,
    val textBefore: String?,
    val textAfter: String?,
)
