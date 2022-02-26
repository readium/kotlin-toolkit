package org.readium.r2.navigator3

import org.readium.r2.shared.publication.Link

interface SpreadState {

    suspend fun goForward(): Boolean

    suspend fun goBackward(): Boolean

    interface Factory {

        fun createSpread(links: List<Link>): Pair<SpreadState, List<Link>>?
    }
}

interface SizedSpreadState : SpreadState {

    val width: Int

    val height: Int
}