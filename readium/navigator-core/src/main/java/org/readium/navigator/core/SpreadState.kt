package org.readium.navigator.core

import androidx.compose.runtime.State
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

interface SpreadState {

    suspend fun goForward(): Boolean

    suspend fun goBackward(): Boolean

    suspend fun goBeginning(): Boolean

    suspend fun goEnd(): Boolean

    suspend fun go(locator: Locator): Boolean

    val locations: State<Locator.Locations>

    val resources: List<ResourceState>

    interface Factory {

        fun createSpread(links: List<Link>): Pair<SpreadState, List<Link>>?
    }
}
