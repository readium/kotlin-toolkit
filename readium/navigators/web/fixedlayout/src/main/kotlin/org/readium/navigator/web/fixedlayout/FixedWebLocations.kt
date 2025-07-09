/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.navigator.web.fixedlayout

import org.readium.navigator.common.CssLocation
import org.readium.navigator.common.CssSelector
import org.readium.navigator.common.Decoration
import org.readium.navigator.common.DecorationLocation
import org.readium.navigator.common.ExportableLocation
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.navigator.common.SelectionLocation
import org.readium.navigator.common.TextQuote
import org.readium.navigator.common.TextQuoteLocation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Locator.Text
import org.readium.r2.shared.publication.html.cssSelector
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
public data class FixedWebDecorationLocation(
    override val href: Url,
    val cssSelector: CssSelector?,
    val textQuote: TextQuote?,
) : DecorationLocation {

    init {
        require(cssSelector != null || textQuote != null)
    }

    public companion object {
        public operator fun invoke(location: Location): FixedWebDecorationLocation? {
            val cssSelector = (location as? CssLocation)?.cssSelector
            val textQuote = (location as? TextQuoteLocation)?.textQuote

            if (cssSelector == null && textQuote == null) {
                return null
            }

            return FixedWebDecorationLocation(location.href, cssSelector, textQuote)
        }

        public operator fun invoke(locator: Locator): FixedWebDecorationLocation? {
            val cssSelector = (
                locator.locations.cssSelector
                    ?: locator.locations.fragments.firstOrNull()?.addPrefix("#")
                )
                ?.let { CssSelector(it) }

            val textQuote = locator.text.highlight?.let {
                TextQuote(
                    quotedText = it,
                    textBefore = locator.text.before.orEmpty(),
                    textAfter = locator.text.after.orEmpty()
                )
            }

            if (cssSelector == null && textQuote == null) {
                return null
            }

            return FixedWebDecorationLocation(locator.href, cssSelector, textQuote)
        }
    }
}

internal typealias FixedWebDecoration = Decoration<FixedWebDecorationLocation>

@ExperimentalReadiumApi
public interface FixedWebLocation : ExportableLocation

@ExperimentalReadiumApi
public interface FixedWebSelectionLocation : ExportableLocation, SelectionLocation, TextQuoteLocation // , CssLocation {

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
    override val textQuote: TextQuote,
) : FixedWebSelectionLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML,
            text = Text(
                before = textQuote.textBefore,
                after = textQuote.textAfter,
                highlight = selectedText
            ),
            /*locations = Locations(
                otherLocations = buildMap {
                    cssSelector?.let { put("cssSelector", cssSelector) }
                }
            ),*/
        )
}
