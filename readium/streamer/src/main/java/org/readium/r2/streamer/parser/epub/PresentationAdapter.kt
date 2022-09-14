/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation

internal class PresentationAdapter(
    private val epubVersion: Double,
    private val displayOptions: Map<String, String>
) {

    fun adapt(items: List<MetadataItem>): Pair<Presentation, List<MetadataItem>> {
        var remainingItems: List<MetadataItem> = items

        val flowProp = remainingItems
            .takeFirstWithProperty(Vocabularies.RENDITION + "flow")
            .let { remainingItems = it.second; it.first }
            ?.value

        val spreadProp = remainingItems
            .takeFirstWithProperty(Vocabularies.RENDITION + "spread")
            .let { remainingItems = it.second; it.first }
            ?.value

        val orientationProp = remainingItems
            .takeFirstWithProperty(Vocabularies.RENDITION + "orientation")
            .let { remainingItems = it.second; it.first }
            ?.value

        val layoutProp =
            if (epubVersion < 3.0) {
                if (displayOptions["fixed-layout"] == "true")
                    "pre-paginated"
                else
                    "reflowable"
            } else  remainingItems
                .takeFirstWithProperty(Vocabularies.RENDITION + "layout")
                .let { remainingItems = it.second; it.first }
                ?.value

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

        val presentation = Presentation(
            overflow = overflow, continuous = continuous,
            layout = layout, orientation = orientation, spread = spread
        )

        return presentation to remainingItems
    }
}
