package org.readium.r2.navigator3.image

import org.readium.r2.navigator3.SpreadState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

class ImageSpreadState(
    val publication: Publication,
    val link: Link
) : SpreadState {

    override suspend fun goForward(): Boolean =
        false

    override suspend fun goBackward(): Boolean =
        false
}

class ImageSpreadStateFactory(
    private val publication: Publication
): SpreadState.Factory {

    override fun createSpread(links: List<Link>): Pair<SpreadState, List<Link>>? {
        require(links.isNotEmpty())

        val first = links.first()
        if (!first.mediaType.isBitmap) {
            return null
        }
        val spread = ImageSpreadState(publication, first)
        return Pair(spread, links.subList(1, links.size))
    }
}