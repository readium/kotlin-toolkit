package org.readium.navigator.common

import androidx.compose.runtime.State
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public interface Navigator<L : Location, G : GoLocation> {

    public val location: State<L>

    public suspend fun goTo(location: L)

    public suspend fun goTo(location: G)

    public suspend fun goTo(link: Link)
}

@ExperimentalReadiumApi
public interface ReadingOrder {

    public val items: List<Item>

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
@ExperimentalReadiumApi
public interface Location {

    public val href: Url
}

@ExperimentalReadiumApi
public interface GoLocation
