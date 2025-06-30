/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorationId
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import timber.log.Timber

public class DecorationApi(
    private val webView: WebView,
) {

    public fun registerTemplates(templates: HtmlDecorationTemplates) {
        Timber.d("templatesJSON ${templates.toJSON()}")
        val templatesAsLiteral = templates.toJSON().toString().toJavaScriptLiteral()
        val script = "decorations.registerTemplates($templatesAsLiteral);"
        webView.evaluateJavascript(script) {}
    }

    public fun addDecoration(decoration: Decoration, template: HtmlDecorationTemplate, group: String) {
        val decorationAsLiteral = decoration.toJSON()
            .apply { put("element", template.element(decoration)) }
            .toString()
            .toJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val script = "decorations.addDecoration($decorationAsLiteral, $groupAsLiteral);"
        Timber.d("Decoration $script")
        webView.evaluateJavascript(script) {}
    }

    public fun removeDecoration(id: DecorationId, group: String) {
        val idAsLiteral = id.toJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val script = "decorations.removeDecoration($idAsLiteral, $groupAsLiteral);"
        webView.evaluateJavascript(script) {}
    }
}
