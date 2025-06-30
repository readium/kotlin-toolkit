/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.decorations

import androidx.annotation.ColorInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.publication.Locator

/**
 * Trivial highlight manager. You can add persistence.
 */
class HighlightsManager {

    private val lastHighlightId: Long = -1

    private val highlightsMutable: MutableStateFlow<PersistentMap<Long, Highlight>> =
        MutableStateFlow(persistentMapOf())

    val highlights: StateFlow<PersistentMap<Long, Highlight>> =
        highlightsMutable.asStateFlow()

    val decorations: Flow<ImmutableList<Decoration>> = highlightsMutable.map {
        it.entries.flatMap { (id, highlight) ->
            highlight.toDecorations(id = id, isActive = false)
        }.toImmutableList()
    }

    fun addHighlight(
        locator: Locator,
        style: Highlight.Style,
        @ColorInt tint: Int,
        annotation: String = "",
    ): Long {
        val id = lastHighlightId + 1
        val highlight = Highlight(
            locator = locator,
            style = style,
            tint = tint,
            annotation = annotation
        )
        highlightsMutable.update { it.put(id, highlight) }
        return id
    }

    fun updateHighlightAnnotation(id: Long, annotation: String) {
        val highlight = checkNotNull(highlightsMutable.value[id])
            .copy(annotation = annotation)
        highlightsMutable.update { it.put(id, highlight) }
    }

    fun updateHighlightStyle(
        id: Long,
        style: Highlight.Style? = null,
        @ColorInt tint: Int? = null,
    ) {
        val originalHighlight = checkNotNull(highlightsMutable.value[id])
        val highlight = originalHighlight
            .copy(style = style ?: originalHighlight.style, tint = tint ?: originalHighlight.tint)
        highlightsMutable.update { it.put(id, highlight) }
    }

    fun deleteHighlight(id: Long) {
        highlightsMutable.update { it.remove(id) }
    }
}

data class Highlight(
    val locator: Locator,
    val style: Style,
    val tint: Int,
    val annotation: String,
) {
    enum class Style(val value: String) {
        HIGHLIGHT("highlight"),
        UNDERLINE("underline"),
    }
}

/**
 * Creates a list of [Decoration] for the receiver [Highlight].
 */
private fun Highlight.toDecorations(id: Long, isActive: Boolean): List<Decoration> {
    fun createDecoration(idSuffix: String, style: Decoration.Style) = Decoration(
        id = "$id-$idSuffix",
        locator = locator,
        style = style,
        extras = mapOf(
            // We store the highlight's ID in the extras map, for easy retrieval
            // later. You can store arbitrary information in the map.
            "id" to id
        )
    )

    return listOfNotNull(
        // Decoration for the actual highlight / underline.
        createDecoration(
            idSuffix = "highlight",
            style = when (style) {
                Highlight.Style.HIGHLIGHT -> Decoration.Style.Highlight(
                    tint = tint,
                    isActive = isActive
                )
                Highlight.Style.UNDERLINE -> Decoration.Style.Underline(
                    tint = tint,
                    isActive = isActive
                )
            }
        ),
        // Additional page margin icon decoration, if the highlight has an associated note.
        annotation.takeIf { it.isNotEmpty() }?.let {
            createDecoration(
                idSuffix = "annotation",
                style = DecorationStyleAnnotationMark(tint = tint)
            )
        }
    )
}
