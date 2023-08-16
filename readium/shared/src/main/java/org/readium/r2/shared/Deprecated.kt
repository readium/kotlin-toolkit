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

import java.net.URL
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

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Locator"), level = DeprecationLevel.ERROR)
public typealias Locator = org.readium.r2.shared.publication.Locator

@Deprecated("Renamed into [Locator.Locations]", ReplaceWith("Locator.Locations", "org.readium.r2.shared.publication.Locator"), level = DeprecationLevel.ERROR)
public typealias Locations = org.readium.r2.shared.publication.Locator.Locations

@Deprecated("Renamed into [Locator.Text]", ReplaceWith("Locator.Text", "org.readium.r2.shared.publication.Locator"), level = DeprecationLevel.ERROR)
public typealias LocatorText = org.readium.r2.shared.publication.Locator.Text

@Deprecated("Moved to another package", ReplaceWith("Locator.Text", "org.readium.r2.shared.publication.html.DomRange"), level = DeprecationLevel.ERROR)
public typealias DomRange = org.readium.r2.shared.publication.html.DomRange

@Deprecated("Renamed into [DomRange.Point]", ReplaceWith("DomRange.Point", "org.readium.r2.shared.publication.html.DomRange"), level = DeprecationLevel.ERROR)
public typealias Range = org.readium.r2.shared.publication.html.DomRange.Point

@Deprecated("Refactored into [LocalizedString]", ReplaceWith("org.readium.r2.shared.publication.LocalizedString"), level = DeprecationLevel.ERROR)
public typealias MultilanguageString = org.readium.r2.shared.publication.LocalizedString

@Deprecated("Renamed into [ReadingProgression]", ReplaceWith("org.readium.r2.shared.publication.ReadingProgression"), level = DeprecationLevel.ERROR)
public typealias PageProgressionDirection = org.readium.r2.shared.publication.ReadingProgression

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Publication"), level = DeprecationLevel.ERROR)
public typealias Publication = org.readium.r2.shared.publication.Publication

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Link"), level = DeprecationLevel.ERROR)
public typealias Link = Link

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Properties"), level = DeprecationLevel.ERROR)
public typealias Properties = Properties

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Metadata"), level = DeprecationLevel.ERROR)
public typealias Metadata = Metadata

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Contributor"), level = DeprecationLevel.ERROR)
public typealias Contributor = Contributor

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Collection"), level = DeprecationLevel.ERROR)
public typealias Collection = Collection

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Subject"), level = DeprecationLevel.ERROR)
public typealias Subject = Subject

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.encryption.Encryption"), level = DeprecationLevel.ERROR)
public typealias Encryption = Encryption

@Deprecated("Refactored into [Presentation]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation"), level = DeprecationLevel.ERROR)
public typealias Rendition = Presentation

@Deprecated("Refactored into [EpubLayout]", ReplaceWith("org.readium.r2.shared.publication.epub.EpubLayout"), level = DeprecationLevel.ERROR)
public typealias RenditionLayout = org.readium.r2.shared.publication.epub.EpubLayout

@Deprecated("Refactored into [Presentation.Overflow]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation.Overflow"), level = DeprecationLevel.ERROR)
public typealias RenditionFlow = Presentation.Overflow

@Deprecated("Refactored into [Presentation.Orientation]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation.Orientation"), level = DeprecationLevel.ERROR)
public typealias RenditionOrientation = Presentation.Orientation

@Deprecated("Refactored into [Presentation.Spread]", ReplaceWith("org.readium.r2.shared.publication.presentation.Presentation.Spread"), level = DeprecationLevel.ERROR)
public typealias RenditionSpread = Presentation.Spread

@Deprecated("Use [Manifest::fromJSON] instead", ReplaceWith("Manifest.fromJSON(pubDict)", "org.readium.r2.shared.publication.Manifest"), level = DeprecationLevel.ERROR)
public fun parsePublication(pubDict: JSONObject): org.readium.r2.shared.publication.Publication {
    return org.readium.r2.shared.publication.Manifest.fromJSON(pubDict)?.let { Publication(it) }
        ?: throw Exception("Invalid publication")
}

@Suppress("Unused_parameter")
@Deprecated("Use [Link::fromJSON] instead", ReplaceWith("Link.fromJSON(linkDict)", "org.readium.r2.shared.publication.Link"), level = DeprecationLevel.ERROR)
public fun parseLink(linkDict: JSONObject, feedUrl: URL? = null): Link {
    throw NotImplementedError()
}

@Deprecated("Moved to another package", ReplaceWith("removeLastComponent()", "org.readium.r2.shared.extensions.removeLastComponent"), level = DeprecationLevel.ERROR)
public fun URL.removeLastComponent(): URL = removeLastComponent()

@Suppress("Unused_parameter")
@Deprecated("Use `Href().string` instead", replaceWith = ReplaceWith("Href(href, base).string"), level = DeprecationLevel.ERROR)
public fun normalize(base: String, href: String?): String {
    throw NotImplementedError()
}
