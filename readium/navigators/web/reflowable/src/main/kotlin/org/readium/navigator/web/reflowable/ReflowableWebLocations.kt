/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable

import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.navigator.common.Progression
import org.readium.navigator.common.ProgressionLocation
import org.readium.navigator.common.SelectionLocation
import org.readium.navigator.common.TextAnchor
import org.readium.navigator.common.TextLocation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Locator.Locations
import org.readium.r2.shared.publication.Locator.Text
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

@ExperimentalReadiumApi
public data class ReflowableWebGoLocation(
    val href: Url,
    val progression: Progression? = null,
    // val cssSelector: String? = null,
    // val textBefore: String? = null,
    // val textAfter: String? = null,
    // val position: Int? = null
) : GoLocation {

    public constructor(location: Location) : this(
        href = location.href,
        progression = (location as? ProgressionLocation)?.progression
    )

    public constructor(locator: Locator) : this(
        href = locator.href,
        progression = locator.locations.progression?.let { Progression(it) }
    )
}

@ExperimentalReadiumApi
public interface ReflowableWebLocation : ProgressionLocation

@ExperimentalReadiumApi
public interface ReflowableWebSelectionLocation : SelectionLocation, TextLocation // , CssLocation

internal data class ReflowableWebLocationImpl(
    override val href: Url,
    val mediaType: MediaType?,
    override val progression: Progression,
) : ReflowableWebLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML,
            locations = Locations(progression = progression.value)
        )
}

internal data class ReflowableWebSelectionLocationImpl(
    override val href: Url,
    val mediaType: MediaType?,
    val selectedText: String,
    // override val cssSelector: CssSelector?,
    override val textAnchor: TextAnchor,
) : ReflowableWebSelectionLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML,
            text = Text(
                highlight = selectedText,
                before = textAnchor.textBefore,
                after = textAnchor.textAfter
            ),
            /*locations = Locations(
                otherLocations = buildMap {
                    cssSelector?.let { put("cssSelector", cssSelector) }
                }
            )*/
        )
}
