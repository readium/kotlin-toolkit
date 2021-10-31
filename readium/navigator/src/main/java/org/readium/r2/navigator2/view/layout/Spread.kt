package org.readium.r2.navigator2.view.layout

import org.readium.r2.shared.publication.Link

sealed class Spread {

    data class SinglePage(val page: Link) : Spread()

    data class DoublePage(val left: Link?, val right: Link?) : Spread()

    val links: List<Link> get() =
        when (this) {
            is DoublePage -> listOfNotNull(this.left, this.right)
            is SinglePage -> listOf(this.page)
        }
}