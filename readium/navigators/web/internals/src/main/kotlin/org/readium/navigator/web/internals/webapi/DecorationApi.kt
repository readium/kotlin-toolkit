/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.readium.navigator.common.CssSelector
import org.readium.navigator.common.Decoration.Id
import org.readium.navigator.common.Decoration.Style
import org.readium.navigator.common.TextQuote
import org.readium.navigator.web.common.WebDecorationTemplate
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import timber.log.Timber

public class ReflowableDecorationApi(
    private val webView: WebView,
    decorationTemplates: WebDecorationTemplates,
) {
    init {
        webView.registerTemplates("decorations", decorationTemplates)
    }

    public fun addDecoration(decoration: Decoration, group: String) {
        val decorationAsLiteral = decoration.toJsonJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val script = "decorations.addDecoration($decorationAsLiteral, $groupAsLiteral);"
        Timber.d("Decoration $script")
        webView.evaluateJavascript(script) {}
    }

    public fun removeDecoration(id: Id, group: String) {
        val idAsLiteral = id.value.toJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val script = "decorations.removeDecoration($idAsLiteral, $groupAsLiteral);"
        webView.evaluateJavascript(script) {}
    }
}

public class FixedSingleDecorationApi(
    private val webView: WebView,
    decorationTemplates: WebDecorationTemplates,
) {

    init {
        webView.registerTemplates("singleDecorations", decorationTemplates)
    }

    public fun addDecoration(decoration: Decoration, group: String) {
        val decorationAsLiteral = decoration.toJsonJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val script = "singleDecorations.addDecoration($decorationAsLiteral, $groupAsLiteral);"
        Timber.d("Decoration $script")
        webView.evaluateJavascript(script) {}
    }

    public fun removeDecoration(id: Id, group: String) {
        val idAsLiteral = id.value.toJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val script = "singleDecorations.removeDecoration($idAsLiteral, $groupAsLiteral);"
        webView.evaluateJavascript(script) {}
    }
}

public class FixedDoubleDecorationApi(
    private val webView: WebView,
    decorationTemplates: WebDecorationTemplates,
) {
    init {
        webView.registerTemplates("doubleDecorations", decorationTemplates)
    }

    public fun addDecoration(
        decoration: Decoration,
        iframe: Iframe,
        group: String,
    ) {
        val decorationAsLiteral = decoration.toJsonJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val iframeAsLiteral = iframe.toString().toJavaScriptLiteral()
        val script = "doubleDecorations.addDecoration($decorationAsLiteral, $iframeAsLiteral, $groupAsLiteral);"
        Timber.d("Decoration $script")
        webView.evaluateJavascript(script) {}
    }

    public fun removeDecoration(id: Id, group: String) {
        val idAsLiteral = id.value.toJavaScriptLiteral()
        val groupAsLiteral = group.toJavaScriptLiteral()
        val script = "doubleDecorations.removeDecoration($idAsLiteral, $groupAsLiteral);"
        webView.evaluateJavascript(script) {}
    }
}

private fun WebView.registerTemplates(apiName: String, templates: WebDecorationTemplates) {
    val templatesAsJsonObject = JsonObject(
        templates.toMap().mapKeys { (klass: KClass<*>, _: WebDecorationTemplate) ->
            klass.qualifiedName.toString()
        }.mapValues { (_, template) ->
            Json.encodeToJsonElement(template.toJsonTemplate())
        }
    )
    val templatesAsJsLiteral = Json.encodeToString(templatesAsJsonObject).toJavaScriptLiteral()
    val script = "$apiName.registerTemplates($templatesAsJsLiteral);"
    evaluateJavascript(script) {}
}

private fun Decoration.toJsonJavaScriptLiteral(): String =
    Json.encodeToString(toJsonDecoration()).toJavaScriptLiteral()

public data class Decoration(
    public val id: Id,
    public val style: Style,
    public val element: String,
    val cssSelector: CssSelector?,
    val textQuote: TextQuote?,
) {
    init {
        require(cssSelector != null || textQuote != null)
    }
}

private fun Decoration.toJsonDecoration(): JsonDecoration =
    JsonDecoration(
        id = id.value,
        style = checkNotNull(style::class.qualifiedName),
        element = element,
        cssSelector = cssSelector?.value,
        textQuote = textQuote?.toSerializableTextQuote()
    )

private fun TextQuote.toSerializableTextQuote() =
    JsonTextQuote(
        quotedText = text,
        textBefore = prefix,
        textAfter = suffix
    )

@Serializable
public sealed class DecorationTarget

@Serializable
public data class TextDecorationTarget(
    val targetedText: String,
    val textBefore: String,
    val textAfter: String,
    val cssSelector: String?,
)

@Serializable
public data class ElementDecorationTarget(
    val cssSelector: String,
)

@Serializable
private data class JsonDecoration(
    val id: String,
    val style: String,
    val element: String,
    val cssSelector: String?,
    val textQuote: JsonTextQuote?,
)

@Serializable
private data class JsonTextQuote(
    val quotedText: String,
    val textBefore: String,
    val textAfter: String,
)

@Serializable
private data class JsonTemplate(
    val layout: String,
    val width: String,
    val stylesheet: String?,
)

private fun WebDecorationTemplate.toJsonTemplate(): JsonTemplate =
    JsonTemplate(
        layout = layout.value,
        width = width.value,
        stylesheet = stylesheet
    )
