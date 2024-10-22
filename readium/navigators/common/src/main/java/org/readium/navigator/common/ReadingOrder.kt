package org.readium.navigator.common

import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.util.Url

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
