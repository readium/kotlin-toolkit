/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.navigator.web.fixedlayout

import org.readium.navigator.common.CssSelector
import org.readium.navigator.common.CssSelectorLocation
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
    override val href: Url,
) : GoLocation {

    public constructor(location: Location) : this(
        href = location.href
    )

    public constructor(locator: Locator) : this(
        href = locator.href
    )
}

@ExperimentalReadiumApi
public sealed interface FixedWebDecorationLocation : DecorationLocation {

    public companion object {

        public operator fun invoke(location: Location): FixedWebDecorationLocation? {
            val cssSelector = (location as? CssSelectorLocation)?.cssSelector
            val textQuote = (location as? TextQuoteLocation)?.textQuote

            return when {
                textQuote != null ->
                    FixedWebDecorationTextQuoteLocation(location.href, textQuote, cssSelector)
                cssSelector != null ->
                    FixedWebDecorationCssSelectorLocation(location.href, cssSelector)
                else -> null
            }
        }

        public operator fun invoke(locator: Locator): FixedWebDecorationLocation? {
            val cssSelector = (
                locator.locations.cssSelector
                    ?: locator.locations.fragments.firstOrNull()?.addPrefix("#")
                )
                ?.let { CssSelector(it) }

            val textQuote = locator.text.highlight?.let {
                TextQuote(
                    text = it,
                    prefix = locator.text.before.orEmpty(),
                    suffix = locator.text.after.orEmpty()
                )
            }

            return when {
                textQuote != null ->
                    FixedWebDecorationTextQuoteLocation(locator.href, textQuote, cssSelector)
                cssSelector != null ->
                    FixedWebDecorationCssSelectorLocation(locator.href, cssSelector)
                else -> null
            }
        }
    }
}

internal data class FixedWebDecorationCssSelectorLocation(
    override val href: Url,
    val cssSelector: CssSelector,
) : FixedWebDecorationLocation

internal data class FixedWebDecorationTextQuoteLocation(
    override val href: Url,
    val textQuote: TextQuote,
    val cssSelector: CssSelector?,
) : FixedWebDecorationLocation

@ExperimentalReadiumApi
@ConsistentCopyVisibility
public data class FixedWebLocation internal constructor(
    override val href: Url,
    private val mediaType: MediaType?,
) : ExportableLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML
        )
}

@ExperimentalReadiumApi
@ConsistentCopyVisibility
public data class FixedWebSelectionLocation internal constructor(
    override val href: Url,
    private val mediaType: MediaType?,
    val selectedText: String,
    // override val cssSelector: CssSelector?,
    override val textQuote: TextQuote,
) : SelectionLocation, TextQuoteLocation { // , CssLocation { { {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML,
            text = Text(
                before = textQuote.prefix,
                after = textQuote.suffix,
                highlight = selectedText
            ),
            /*locations = Locations(
                otherLocations = buildMap {
                    cssSelector?.let { put("cssSelector", cssSelector) }
                }
            ),*/
        )
}

internal typealias FixedWebDecoration = Decoration<FixedWebDecorationLocation>
