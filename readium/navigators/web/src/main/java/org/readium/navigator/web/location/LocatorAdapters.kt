/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.location

import org.readium.navigator.common.LocatorAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.indexOfFirstWithHref

@ExperimentalReadiumApi
public class FixedWebLocatorAdapter internal constructor(
    private val publication: Publication
) : LocatorAdapter<FixedWebLocation, FixedWebGoLocation> {

    public override fun Locator.toGoLocation(): FixedWebGoLocation =
        HrefLocation(href)

    override fun FixedWebLocation.toLocator(): Locator {
        val position = publication.readingOrder.indexOfFirstWithHref(href)
        return publication.locatorFromLink(Link(href))!!
            .copyWithLocations(position = position)
    }
}

@ExperimentalReadiumApi
public class ReflowableWebLocatorAdapter internal constructor(
    private val publication: Publication
) : LocatorAdapter<ReflowableWebLocation, ReflowableWebGoLocation> {

    public override fun ReflowableWebLocation.toLocator(): Locator =
        publication.locatorFromLink(Link(href))!!
            .copy(
                text = Locator.Text(
                    after = textAfter,
                    before = textBefore
                )
            )
            .copyWithLocations(
                progression = progression,
                position = position,
                otherLocations = buildMap { cssSelector?.let { put("cssSelector", cssSelector) } }
            )

    public override fun Locator.toGoLocation(): ReflowableWebGoLocation {
        val locations = buildList {
            if (text.highlight != null || text.before != null || text.after != null) {
                add(
                    TextLocation(
                        href = href,
                        textBefore = text.before,
                        textAfter = text.highlight?.let { it + text.after } ?: text.after,
                        cssSelector = locations.cssSelector
                    )
                )
            }

            if (locations.progression != null) {
                add(
                    ProgressionLocation(
                        href = href,
                        progression = locations.progression!!
                    )
                )
            }

            if (locations.position != null) {
                add(
                    PositionLocation(
                        position = locations.position!!
                    )
                )
            }

            add(HrefLocation(href = href))
        }

        return if (locations.size == 1) {
            locations.first()
        } else {
            ReflowableWebGoLocationList(locations)
        }
    }
}
