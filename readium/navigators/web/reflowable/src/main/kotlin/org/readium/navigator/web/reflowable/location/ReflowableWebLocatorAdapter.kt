/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.location

import org.readium.navigator.common.LocatorAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
public class ReflowableWebLocatorAdapter internal constructor(
    private val publication: Publication,
) : LocatorAdapter<ReflowableWebLocation, ReflowableWebGoLocation, ReflowableWebSelectionLocation> {

    public override fun Locator.toGoLocation(): ReflowableWebGoLocation =
        ReflowableWebGoLocation(
            href = href,
            progression = locations.progression,
            // cssSelector = locations.cssSelector,
            // textBefore = text.before,
            // textAfter = text.highlight?.let { it + text.after } ?: text.after,
            // position = locations.position
        )

    public override fun ReflowableWebLocation.toLocator(): Locator =
        publication.locatorFromLink(Link(href))!!
            .copy(
                text = Locator.Text(
                    // after = textAfter,
                    // before = textBefore
                )
            )
            .copyWithLocations(
                progression = progression,
                // position = position,
                // otherLocations = buildMap { cssSelector?.let { put("cssSelector", cssSelector) } }
            )

    public override fun ReflowableWebSelectionLocation.toLocator(): Locator =
        publication.locatorFromLink(Link(href))!!
            .copy(
                text = Locator.Text(
                    highlight = selectedText,
                    before = textBefore,
                    after = textAfter
                )
            )
}
