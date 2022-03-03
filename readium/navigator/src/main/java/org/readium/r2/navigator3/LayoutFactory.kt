package org.readium.r2.navigator3

import androidx.compose.ui.unit.IntSize
import org.readium.r2.navigator3.html.HtmlSpreadStateFactory
import org.readium.r2.navigator3.image.ImageSpreadStateFactory
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

internal class LayoutFactory(
    private val publication: Publication,
    private val links: List<Link>,
) {
    data class Layout(
        val isVertical: Boolean,
        val reverseDirection: Boolean,
        val isPaginated: Boolean,
        val viewerScrollable: Boolean,
        val readingProgression: ReadingProgression,
        val viewport: IntSize,
        val spreadStates: List<SpreadState>
    )

    fun layout(viewport: IntSize): Layout {
        val factories = defaultSpreadStateFactories(viewport)
        val spreads = computeSpreads(links, factories)
        val isVertical = false
        val reverseDirection = false
        val isPaginated = true
        val viewerScrollable = true
        val readingProgression = ReadingProgression.LTR
        return Layout(
            isVertical,
            reverseDirection,
            isPaginated,
            viewerScrollable,
            readingProgression,
            viewport,
            spreads
        )
    }

    private fun defaultSpreadStateFactories(viewport: IntSize): List<SpreadState.Factory> {
        val htmlSpreadFactory = HtmlSpreadStateFactory(publication, viewport)
        val imageSpreadFactory = ImageSpreadStateFactory(publication)
        return listOf(htmlSpreadFactory, imageSpreadFactory)
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
