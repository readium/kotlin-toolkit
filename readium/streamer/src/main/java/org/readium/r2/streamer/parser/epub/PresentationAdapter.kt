package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation

internal class PresentationAdapter(
    private val epubVersion: Double,
    private val displayOptions: Map<String, String>,
    val items: List<MetadataItem>,
) {

    fun adapt(): Presentation {
        val flowProp = items.firstWithProperty(Vocabularies.RENDITION + "flow")?.value
        val spreadProp = items.firstWithProperty(Vocabularies.RENDITION + "spread")?.value
        val orientationProp = items.firstWithProperty(Vocabularies.RENDITION + "orientation")?.value
        val layoutProp =
            if (epubVersion < 3.0)
                if (displayOptions["fixed-layout"] == "true") "pre-paginated" else "reflowable"
            else items.firstWithProperty(Vocabularies.RENDITION + "layout")?.value

        val (overflow, continuous) = when (flowProp) {
            "paginated" -> Pair(Presentation.Overflow.PAGINATED, false)
            "scrolled-continuous" -> Pair(Presentation.Overflow.SCROLLED, true)
            "scrolled-doc" -> Pair(Presentation.Overflow.SCROLLED, false)
            else -> Pair(Presentation.Overflow.AUTO, false)
        }

        val layout = when (layoutProp) {
            "pre-paginated" -> EpubLayout.FIXED
            else -> EpubLayout.REFLOWABLE
        }

        val orientation = when (orientationProp) {
            "landscape" -> Presentation.Orientation.LANDSCAPE
            "portrait" -> Presentation.Orientation.PORTRAIT
            else -> Presentation.Orientation.AUTO
        }

        val spread = when (spreadProp) {
            "none" -> Presentation.Spread.NONE
            "landscape" -> Presentation.Spread.LANDSCAPE
            "both", "portrait" -> Presentation.Spread.BOTH
            else -> Presentation.Spread.AUTO
        }

        return Presentation(
            overflow = overflow, continuous = continuous,
            layout = layout, orientation = orientation, spread = spread
        )
    }
}
