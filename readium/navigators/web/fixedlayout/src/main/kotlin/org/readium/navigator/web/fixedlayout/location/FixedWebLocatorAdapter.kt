/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.location

import org.readium.navigator.common.LocatorAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref

@ExperimentalReadiumApi
public class FixedWebLocatorAdapter internal constructor(
    private val publication: Publication,
) : LocatorAdapter<FixedWebLocation, FixedWebGoLocation, FixedWebSelectionLocation> {

    public override fun Locator.toGoLocation(): FixedWebGoLocation =
        FixedWebGoLocation(href)

    override fun FixedWebLocation.toLocator(): Locator {
        val position = publication.readingOrder.indexOfFirstWithHref(href)!!
        val totalProgression = position.toDouble() / publication.readingOrder.size
        return publication.locatorFromLink(Link(href))!!
            .copyWithLocations(position = position, totalProgression = totalProgression)
    }

    override fun FixedWebSelectionLocation.toLocator(): Locator =
        publication.locatorFromLink(Link(href))!!
            .copy(
                text = Locator.Text(
                    highlight = selectedText,
                    before = textBefore,
                    after = textAfter
                )
            )
}
