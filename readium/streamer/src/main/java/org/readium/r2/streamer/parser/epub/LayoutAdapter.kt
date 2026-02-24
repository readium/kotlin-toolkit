/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Layout

internal class LayoutAdapter(
    private val epubVersion: Double,
    private val displayOptions: Map<String, String>,
) {

    fun adapt(items: List<MetadataItem>): Pair<Layout, List<MetadataItem>> {
        val itemsHolder = MetadataItemsHolder(items)

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

        val layout = when (layoutProp) {
            "pre-paginated" -> Layout.FIXED
            else -> Layout.REFLOWABLE
        }

        return layout to itemsHolder.remainingItems
    }
}
