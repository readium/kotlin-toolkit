package org.readium.navigator.common

import androidx.compose.runtime.State
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public interface Navigator<R : ReadingOrder, L : Location> {

    public val readingOrder: R

    public val location: State<L>

    public suspend fun goTo(location: L)

    public suspend fun goTo(readingOrderItem: Int)

    public suspend fun goTo(url: Url) {
        val itemIndex = readingOrder.indexOfHref(url) ?: return
        goTo(itemIndex)
    }
}

/**
 * Data about the content to play.
 */
public interface ReadingOrder {

    /**
     * List of items to play.
     */
    public val items: List<Item>

    /**
     * A piece of the content to play.
     */
    public interface Item {

        public val href: Url
    }

    @OptIn(DelicateReadiumApi::class)
    public fun indexOfHref(href: Url): Int? = items
        .indexOfFirst { it.href == href }
        .takeUnless { it == -1 }
}

/**
 *  Location of the navigator.
 */
public interface Location {

    public val href: Url
}
