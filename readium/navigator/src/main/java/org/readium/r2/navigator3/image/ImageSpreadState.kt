package org.readium.r2.navigator3.image

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.readium.r2.navigator3.SpreadState
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class ImageSpreadState(
    val publication: Publication,
    val link: Link
) : SpreadState {

    override suspend fun goForward(): Boolean =
        false

    override suspend fun goBackward(): Boolean =
        false

    override suspend fun goBeginning(): Boolean =
        false

    override suspend fun goEnd(): Boolean =
        false

    override suspend fun go(locator: Locator): Boolean =
        false

    override val locations: State<Locator.Locations> =
        mutableStateOf(Locator.Locations())
}

class ImageSpreadStateFactory(
    private val publication: Publication,
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