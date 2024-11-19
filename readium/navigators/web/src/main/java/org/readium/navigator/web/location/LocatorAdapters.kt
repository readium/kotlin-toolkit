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
    private val publication: Publication,
) : LocatorAdapter<FixedWebLocation, FixedWebGoLocation> {

    public override fun Locator.toGoLocation(): FixedWebGoLocation =
        FixedWebGoLocation(href)

    override fun FixedWebLocation.toLocator(): Locator {
        val position = publication.readingOrder.indexOfFirstWithHref(href)!!
        val totalProgression = position.toDouble() / publication.readingOrder.size
        return publication.locatorFromLink(Link(href))!!
            .copyWithLocations(position = position, totalProgression = totalProgression)
    }
}

@ExperimentalReadiumApi
public class ReflowableWebLocatorAdapter internal constructor(
    private val publication: Publication,
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

    public override fun Locator.toGoLocation(): ReflowableWebGoLocation =
        ReflowableWebGoLocation(
            href = href,
            progression = locations.progression,
            cssSelector = locations.cssSelector,
            textBefore = text.before,
            textAfter = text.highlight?.let { it + text.after } ?: text.after,
            position = locations.position
        )
}
