/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.extensions

import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorationChange
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import timber.log.Timber

// Decoration extensions related to HTML/EPUB.

/**
 * Generates the JavaScript used to apply the receiver list of [DecorationChange] in a web view.
 */
internal fun List<DecorationChange>.javascriptForGroup(
    group: String,
    templates: HtmlDecorationTemplates,
): String? {
    if (isEmpty()) return null

    return """
        // Using requestAnimationFrame helps to make sure the page is fully laid out before adding the
        // decorations.
        requestAnimationFrame(function () {
            let group = readium.getDecorations('$group');
            ${mapNotNull { it.javascript(templates) }.joinToString("\n")}
        });
        """
}

/**
 * Generates the JavaScript used to apply the receiver [DecorationChange] in a web view.
 */
internal fun DecorationChange.javascript(templates: HtmlDecorationTemplates): String? {
    fun toJSON(decoration: Decoration): JSONObject? {
        val template = templates[decoration.style::class] ?: run {
            Timber.e("Decoration style not registered: ${decoration.style::class}")
            return null
        }
        return decoration.toJSON().apply {
            put("element", template.element(decoration))
        }
    }

    return when (this) {
        is DecorationChange.Added ->
            toJSON(decoration)?.let { "group.add($it);" }

        is DecorationChange.Moved ->
            null // Not supported for now

        is DecorationChange.Removed ->
            "group.remove('$id');"

        is DecorationChange.Updated ->
            toJSON(decoration)?.let { "group.update($it);" }
    }
}
