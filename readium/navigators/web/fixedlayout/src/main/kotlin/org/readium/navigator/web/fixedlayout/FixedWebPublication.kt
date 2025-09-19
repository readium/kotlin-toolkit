package org.readium.navigator.web.fixedlayout

import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

internal class FixedWebPublication(
    val readingOrder: ReadingOrder,
    @Suppress("unused") val otherResources: List<Item>,
    val container: Container<Resource>,
) {
    sealed interface Item {
        val href: Url
        val mediaType: MediaType?
    }

    data class ReadingOrderItem(
        override val href: Url,
        override val mediaType: MediaType?,
        val page: Presentation.Page?,
    ) : Item

    data class OtherItem(
        override val href: Url,
        override val mediaType: MediaType?,
    ) : Item

    internal data class ReadingOrder(
        val items: List<ReadingOrderItem>,
    ) {
        val size: Int get() = items.size

        operator fun get(index: Int): ReadingOrderItem =
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
