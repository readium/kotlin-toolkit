/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.demo.navigator.decorations

import androidx.annotation.ColorInt
import kotlin.text.isNotEmpty
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import org.readium.navigator.common.Decoration
import org.readium.navigator.common.DecorationLocation
import org.readium.navigator.common.GoLocation
import org.readium.navigator.web.fixedlayout.FixedWebDecorationLocation
import org.readium.navigator.web.fixedlayout.FixedWebGoLocation
import org.readium.navigator.web.reflowable.ReflowableWebDecorationLocation
import org.readium.navigator.web.reflowable.ReflowableWebGoLocation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

class ReflowableWebHighlightsManager :
    HighlightsManager<ReflowableWebDecorationLocation, ReflowableWebGoLocation>(
        DecorationFactory::createReflowableDecorationsForHighlight,
        { ReflowableWebGoLocation(it.locator) }
    )

class FixedWebHighlightsManager :
    HighlightsManager<FixedWebDecorationLocation, FixedWebGoLocation>(
        DecorationFactory::createFixedDecorationsForHighlight,
        { FixedWebGoLocation(it.locator) }
    )

/**
 * Trivial highlight manager. You can add persistence.
 */
sealed class HighlightsManager<D : DecorationLocation, G : GoLocation>(
    decorationFactory: (Highlight, Long) -> List<Decoration<D>>,
    private val locationFactory: (Highlight) -> G?,
) {

    private var lastHighlightId: Long = -1

    private val highlightsMutable: MutableStateFlow<PersistentMap<Long, Highlight>> =
        MutableStateFlow(persistentMapOf())

    val highlights: Flow<List<Highlight>> =
        highlightsMutable
            .map { map ->
                map.entries
                    .sortedBy { it.key }
                    .map { it.value }
                    .toList()
            }

    val decorations: Flow<PersistentList<Decoration<D>>> = highlightsMutable.map {
        it.entries.flatMap { (id, highlight) -> decorationFactory(highlight, id) }.toPersistentList()
    }

    fun getHighlight(
        id: Long,
    ): Highlight? =
        highlightsMutable.value[id]

    fun getLocation(highlight: Highlight): G? =
        locationFactory(highlight)

    fun addHighlight(
        locator: Locator,
        style: Highlight.Style,
        @ColorInt tint: Int,
        annotation: String = "",
    ): Long {
        lastHighlightId += 1
        val id = lastHighlightId

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

object DecorationFactory {

    /**
     * Creates a list of [Decoration] usable in a reflowable rendition for the receiver [Highlight].
     */
    fun createReflowableDecorationsForHighlight(
        highlight: Highlight,
        id: Long,
    ): List<Decoration<ReflowableWebDecorationLocation>> {
        val location = ReflowableWebDecorationLocation(highlight.locator)
            ?: return emptyList()

        return createDecorationsForHighlight(highlight, id, location)
    }

    /**
     * Creates a list of [Decoration] usable in a fixed layout rendition for the receiver [Highlight].
     */
    fun createFixedDecorationsForHighlight(
        highlight: Highlight,
        id: Long,
    ): List<Decoration<FixedWebDecorationLocation>> {
        val location = FixedWebDecorationLocation(highlight.locator)
            ?: return emptyList()

        return createDecorationsForHighlight(highlight, id, location)
    }

    private fun <L : DecorationLocation> createDecorationsForHighlight(
        highlight: Highlight,
        highlightId: Long,
        location: L,
    ): List<Decoration<L>> = buildList {
        add(
            createDecoration(
                highlightId = highlightId,
                idSuffix = "highlight",
                style = highlight.highlightStyle(isActive = false),
                location = location
            )
        )

        // Additional page margin icon decoration, if the highlight has an associated note.
        highlight.annotation.takeIf { it.isNotEmpty() }?.let {
            add(
                createDecoration(
                    highlightId = highlightId,
                    idSuffix = "annotation",
                    style = DecorationStyleAnnotationMark(tint = highlight.tint),
                    location = location
                )
            )
        }
    }

    private fun <L : DecorationLocation> createDecoration(
        highlightId: Long,
        idSuffix: String,
        style: Decoration.Style,
        location: L,
    ): Decoration<L> =
        Decoration(
            id = Decoration.Id("$highlightId-$idSuffix"),
            location = location,
            style = style,
            /*extras = mapOf(
                // We store the highlight's ID in the extras map, for easy retrieval
                // later. You can store arbitrary information in the map.
                "id" to id
            )*/
        )

    private fun Highlight.highlightStyle(isActive: Boolean): Decoration.Style =
        when (style) {
            Highlight.Style.HIGHLIGHT -> Decoration.Style.Highlight(
                tint = tint,
                isActive = isActive
            )
            Highlight.Style.UNDERLINE -> Decoration.Style.Underline(
                tint = tint,
                isActive = isActive
            )
        }
}
