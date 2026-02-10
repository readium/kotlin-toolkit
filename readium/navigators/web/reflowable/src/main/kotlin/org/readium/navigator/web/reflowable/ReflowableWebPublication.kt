/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable

import kotlin.math.floor
import org.readium.navigator.common.Position
import org.readium.navigator.common.Progression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

internal class ReflowableWebPublication(
    val readingOrder: ReadingOrder,
    @Suppress("unused") val otherResources: List<Item>,
    val container: Container<Resource>,
) {
    data class Item(
        val href: Url,
        val mediaType: MediaType?,
    )

    data class ReadingOrder(
        val items: List<Item>,
        val positionNumbers: List<Int>,
    ) {
        val size: Int get() = items.size

        operator fun get(index: Int): Item =
            items[index]

        fun indexOfHref(href: Url): Int? = items
            .indexOfFirst { it.href == href }
            .takeUnless { it == -1 }
    }

    private val allItems = readingOrder.items + otherResources

    private val startPositions = buildList {
        var position = 1
        for (item in readingOrder.positionNumbers) {
            add(position)
            position += item
        }
    }

    private val totalPositionCount = readingOrder.positionNumbers.sum()

    val mediaTypes = allItems
        .mapNotNull { item -> item.mediaType?.let { item.href to it } }
        .associate { it }

    fun itemWithHref(href: Url): Item? =
        allItems.firstOrNull { it.href == href }

    fun positionForProgression(href: Url, progression: Progression): Position {
        val index = readingOrder.indexOfHref(href)!!
        return positionForProgression(index, progression)
    }

    fun positionForProgression(index: Int, progression: Progression): Position {
        val itemPositionNumber = readingOrder.positionNumbers[index]
        val localPosition = floor(progression.value * itemPositionNumber).toInt()
            // If progression == 1.0, don't go for the next resource.
            .coerceAtMost(itemPositionNumber - 1)
        return Position(startPositions[index] + localPosition)!!
    }

    fun totalProgressionForPosition(position: Position): Progression {
        return Progression((position.value - 1) / totalPositionCount.toDouble())!!
    }
}
