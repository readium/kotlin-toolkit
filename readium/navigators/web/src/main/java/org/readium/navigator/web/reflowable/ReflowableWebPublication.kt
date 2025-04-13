/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

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
    ) {
        val size: Int get() = items.size

        operator fun get(index: Int): Item =
            items[index]

        fun indexOfHref(href: Url): Int? = items
            .indexOfFirst { it.href == href }
            .takeUnless { it == -1 }
    }

    private val allItems = readingOrder.items + otherResources

    val mediaTypes = allItems
        .mapNotNull { item -> item.mediaType?.let { item.href to it } }
        .associate { it }

    fun itemWithHref(href: Url): Item? =
        allItems.firstOrNull { it.href == href }
}
