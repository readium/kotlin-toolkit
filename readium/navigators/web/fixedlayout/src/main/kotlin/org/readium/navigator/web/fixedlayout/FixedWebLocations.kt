/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.fixedlayout

import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.navigator.common.SelectionLocation
import org.readium.navigator.common.TextAnchor
import org.readium.navigator.common.TextLocation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Locator.Text
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

@ExperimentalReadiumApi
public data class FixedWebGoLocation(
    val href: Url,
) : GoLocation {

    public constructor(location: Location) : this(
        href = location.href
    )

    public constructor(locator: Locator) : this(
        href = locator.href
    )
}

@ExperimentalReadiumApi
public interface FixedWebLocation : Location {

    override val href: Url
}

@ExperimentalReadiumApi
public interface FixedWebSelectionLocation : SelectionLocation, TextLocation // , CssLocation {

internal data class FixedWebLocationImpl(
    override val href: Url,
    val mediaType: MediaType?,
) : FixedWebLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML
        )
}

internal data class FixedWebSelectionLocationImpl(
    override val href: Url,
    val mediaType: MediaType?,
    val selectedText: String,
    // override val cssSelector: CssSelector?,
    override val textAnchor: TextAnchor,
) : FixedWebSelectionLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML,
            text = Text(
                before = textAnchor.textBefore,
                after = textAnchor.textAfter,
                highlight = selectedText
            ),
            /*locations = Locations(
                otherLocations = buildMap {
                    cssSelector?.let { put("cssSelector", cssSelector) }
                }
            ),*/
        )
}
