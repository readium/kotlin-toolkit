package org.readium.r2.navigator3

import androidx.compose.runtime.State
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

interface SpreadState {

    suspend fun goForward(): Boolean

    suspend fun goBackward(): Boolean

    suspend fun goBeginning()

    suspend fun goEnd()

    suspend fun go(locator: Locator)

    val locations: State<Locator.Locations>

    interface Factory {

        fun createSpread(links: List<Link>): Pair<SpreadState, List<Link>>?
    }
}
