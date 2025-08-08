/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.navigator.web.reflowable

import org.readium.navigator.common.CssSelector
import org.readium.navigator.common.CssSelectorLocation
import org.readium.navigator.common.Decoration
import org.readium.navigator.common.DecorationLocation
import org.readium.navigator.common.ExportableLocation
import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.navigator.common.Progression
import org.readium.navigator.common.ProgressionLocation
import org.readium.navigator.common.SelectionLocation
import org.readium.navigator.common.TextQuote
import org.readium.navigator.common.TextQuoteLocation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Locator.Locations
import org.readium.r2.shared.publication.Locator.Text
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

@ExperimentalReadiumApi
public data class ReflowableWebGoLocation(
    override val href: Url,
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
public sealed interface ReflowableWebDecorationLocation : DecorationLocation {

    public companion object {

        public operator fun invoke(
            href: Url,
            cssSelector: CssSelector,
        ): ReflowableWebDecorationLocation =
            ReflowableWebDecorationCssSelectorLocation(href, cssSelector)

        public operator fun invoke(
            href: Url,
            textQuote: TextQuote,
            cssSelector: CssSelector?,
        ): ReflowableWebDecorationLocation =
            ReflowableWebDecorationTextQuoteLocation(href, textQuote, cssSelector)

        public operator fun invoke(location: Location): ReflowableWebDecorationLocation? {
            val cssSelector = (location as? CssSelectorLocation)?.cssSelector
            val textQuote = (location as? TextQuoteLocation)?.textQuote

            return when {
                textQuote != null ->
                    ReflowableWebDecorationTextQuoteLocation(location.href, textQuote, cssSelector)
                cssSelector != null ->
                    ReflowableWebDecorationCssSelectorLocation(location.href, cssSelector)
                else ->
                    null
            }
        }

        public operator fun invoke(locator: Locator): ReflowableWebDecorationLocation? {
            val cssSelector = (
                locator.locations.cssSelector
                    ?: locator.locations.fragments.firstOrNull()?.addPrefix("#")
                )?.let { CssSelector(it) }

            val textQuote = locator.text.highlight?.let {
                TextQuote(
                    text = it,
                    prefix = locator.text.before.orEmpty(),
                    suffix = locator.text.after.orEmpty()
                )
            }

            return when {
                textQuote != null ->
                    ReflowableWebDecorationTextQuoteLocation(locator.href, textQuote, cssSelector)
                cssSelector != null ->
                    ReflowableWebDecorationCssSelectorLocation(locator.href, cssSelector)
                else ->
                    null
            }
        }
    }
}

internal data class ReflowableWebDecorationCssSelectorLocation(
    override val href: Url,
    val cssSelector: CssSelector,
) : ReflowableWebDecorationLocation

internal data class ReflowableWebDecorationTextQuoteLocation(
    override val href: Url,
    val textQuote: TextQuote,
    val cssSelector: CssSelector?,
) : ReflowableWebDecorationLocation

@ExperimentalReadiumApi
@ConsistentCopyVisibility
public data class ReflowableWebLocation internal constructor(
    override val href: Url,
    private val mediaType: MediaType?,
    override val progression: Progression,
) : ExportableLocation, ProgressionLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML,
            locations = Locations(progression = progression.value)
        )
}

@ExperimentalReadiumApi
@ConsistentCopyVisibility
public data class ReflowableWebSelectionLocation internal constructor(
    override val href: Url,
    private val mediaType: MediaType?,
    val selectedText: String,
    // override val cssSelector: CssSelector?,
    override val textQuote: TextQuote,
) : SelectionLocation, TextQuoteLocation { // , CssLocation {

    override fun toLocator(): Locator =
        Locator(
            href = href,
            mediaType = mediaType ?: MediaType.XHTML,
            text = Text(
                highlight = textQuote.text,
                before = textQuote.prefix,
                after = textQuote.suffix
            ),
            /*locations = Locations(
                otherLocations = buildMap {
                    cssSelector?.let { put("cssSelector", cssSelector) }
                }
            )*/
        )
}

internal typealias ReflowableWebDecoration = Decoration<ReflowableWebDecorationLocation>
