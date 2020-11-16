/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:Suppress("RemoveRedundantQualifierName")

package org.readium.r2.shared

import org.json.JSONObject
import org.readium.r2.shared.extensions.removeLastComponent
import org.readium.r2.shared.publication.Collection
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Subject
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Href
import java.net.URL

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Locator"))
typealias Locator = org.readium.r2.shared.publication.Locator

@Deprecated("Renamed into [Locator.Locations]", ReplaceWith("Locator.Locations", "org.readium.r2.shared.publication.Locator"))
typealias Locations = org.readium.r2.shared.publication.Locator.Locations

@Deprecated("Renamed into [Locator.Text]", ReplaceWith("Locator.Text", "org.readium.r2.shared.publication.Locator"))
typealias LocatorText = org.readium.r2.shared.publication.Locator.Text

@Deprecated("Moved to another package", ReplaceWith("Locator.Text", "org.readium.r2.shared.publication.html.DomRange"))
typealias DomRange = org.readium.r2.shared.publication.html.DomRange

@Deprecated("Renamed into [DomRange.Point]", ReplaceWith("DomRange.Point", "org.readium.r2.shared.publication.html.DomRange"))
typealias Range = org.readium.r2.shared.publication.html.DomRange.Point

@Deprecated("Refactored into [LocalizedString]", ReplaceWith("org.readium.r2.shared.publication.LocalizedString"))
typealias MultilanguageString = org.readium.r2.shared.publication.LocalizedString

@Deprecated("Renamed into [ReadingProgression]", ReplaceWith("org.readium.r2.shared.publication.ReadingProgression"))
typealias PageProgressionDirection = org.readium.r2.shared.publication.ReadingProgression

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Publication"))
typealias Publication = org.readium.r2.shared.publication.Publication

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Link"))
typealias Link = Link

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Properties"))
typealias Properties = Properties

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Metadata"))
typealias Metadata = Metadata

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Contributor"))
typealias Contributor = Contributor

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Collection"))
typealias Collection = Collection

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Subject"))
typealias Subject = Subject

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.encryption.Encryption"))
typealias Encryption = Encryption

@Deprecated("Refactored into [Presentation]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation"))
typealias Rendition = Presentation

@Deprecated("Refactored into [EpubLayout]", ReplaceWith("org.readium.r2.shared.publication.epub.EpubLayout"))
typealias RenditionLayout = org.readium.r2.shared.publication.epub.EpubLayout

@Deprecated("Refactored into [Presentation.Overflow]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation.Overflow"))
typealias RenditionFlow = Presentation.Overflow

@Deprecated("Refactored into [Presentation.Orientation]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation.Orientation"))
typealias RenditionOrientation = Presentation.Orientation

@Deprecated("Refactored into [Presentation.Spread]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation.Spread"))
typealias RenditionSpread = Presentation.Spread

@Deprecated("Use [Manifest::fromJSON] instead", ReplaceWith("Manifest.fromJSON(pubDict)", "org.readium.r2.shared.publication.Manifest"))
fun parsePublication(pubDict: JSONObject): org.readium.r2.shared.publication.Publication {
    return org.readium.r2.shared.publication.Manifest.fromJSON(pubDict)?.let { Publication(it) }
        ?: throw Exception("Invalid publication")
}

@Deprecated("Use [Link::fromJSON] instead", ReplaceWith("Link.fromJSON(linkDict)", "org.readium.r2.shared.publication.Link"))
fun parseLink(linkDict: JSONObject, feedUrl: URL? = null): Link =
    Link.fromJSON(linkDict, normalizeHref = {
        if (feedUrl == null) {
            it
        } else {
            Href(it, baseHref = feedUrl.toString()).string
        }
    }) ?: Link(href = "#")

@Deprecated("Moved to another package", ReplaceWith("removeLastComponent()", "org.readium.r2.shared.extensions.removeLastComponent"))
fun URL.removeLastComponent(): URL = removeLastComponent()

@Deprecated("Use `Href().string` instead", replaceWith = ReplaceWith("Href(href, base).string"))
fun normalize(base: String, href: String?): String =
    Href(href ?: "", baseHref = base).string
