package org.readium.r2.navigator3

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

class LayoutFactory(
    private val publication: Publication,
    private val links: List<Link>,
    private val spreadStateFactories: List<SpreadState.Factory>
) {
    data class Layout(
        val isVertical: Boolean,
        val reverseDirection: Boolean,
        val isPaginated: Boolean,
        val readingProgression: ReadingProgression,
        val spreadStates: List<SpreadState>
    )

    fun createLayout(): Layout {
        val spreads = computeSpreads(links, spreadStateFactories)
        val isVertical = false
        val reverseDirection = false
        val isPaginated = true
        val readingProgression = ReadingProgression.LTR
        return Layout(isVertical, reverseDirection, isPaginated, readingProgression, spreads)
    }

    private fun computeSpreads(links: List<Link>, factories: List<SpreadState.Factory>): List<SpreadState> {
        val spreads: MutableList<SpreadState> = mutableListOf()
        var remaining: List<Link> = links

        while (remaining.isNotEmpty()) {
            var found = false

            for (factory in factories) {
                val result = factory.createSpread(remaining)
                if (result != null) {
                    val spread = result.first
                    spreads.add(spread)
                    remaining = result.second
                    found = true
                    break
                }
            }

            if (!found) {
                val first = remaining.first()
                remaining = remaining.subList(1, remaining.size)
                Timber.w("Skipping resource $first because no adapter supports it.")
            }
        }

        return spreads
    }
}
