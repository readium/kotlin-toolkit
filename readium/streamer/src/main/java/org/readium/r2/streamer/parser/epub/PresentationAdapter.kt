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
    private val displayOptions: Map<String, String>,
) {

    fun adapt(items: List<MetadataItem>): Pair<Presentation, List<MetadataItem>> {
        val itemsHolder = MetadataItemsHolder(items)

        val flowProp = itemsHolder
            .adapt { it.takeFirstWithProperty(Vocabularies.RENDITION + "flow") }
            ?.value

        val spreadProp = itemsHolder
            .adapt { it.takeFirstWithProperty(Vocabularies.RENDITION + "spread") }
            ?.value

        val orientationProp = itemsHolder
            .adapt { it.takeFirstWithProperty(Vocabularies.RENDITION + "orientation") }
            ?.value

        val layoutProp =
            if (epubVersion < 3.0) {
                if (displayOptions["fixed-layout"] == "true") {
                    "pre-paginated"
                } else {
                    "reflowable"
                }
            } else {
                itemsHolder
                    .adapt { it.takeFirstWithProperty(Vocabularies.RENDITION + "layout") }
                    ?.value
            }

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
            overflow = overflow,
            continuous = continuous,
            layout = layout,
            orientation = orientation,
            spread = spread
        )

        return presentation to itemsHolder.remainingItems
    }
}
