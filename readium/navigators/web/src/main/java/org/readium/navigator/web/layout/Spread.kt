@file:OptIn(DelicateReadiumApi::class)

package org.readium.navigator.web.layout

import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.util.Url

internal sealed class Spread {

    abstract fun contains(href: Url): Boolean

    data class Single(
        val page: Url
    ) : Spread() {

        override fun contains(href: Url) =
            page == href
    }

    data class Double(
        val leftPage: Url?,
        val rightPage: Url?
    ) : Spread() {
        override fun contains(href: Url): Boolean =
            leftPage == href || rightPage == href
    }
}
