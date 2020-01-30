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
import java.net.URL


@Deprecated("Refactored into [LocalizedString]", ReplaceWith("org.readium.r2.shared.publication.LocalizedString"))
typealias MultilanguageString = org.readium.r2.shared.publication.LocalizedString

@Deprecated("Renamed into [ContentLayout]", ReplaceWith("org.readium.r2.shared.publication.ContentLayout"))
typealias ContentLayoutStyle = org.readium.r2.shared.publication.ContentLayout

@Deprecated("Renamed into [ReadingProgression]", ReplaceWith("org.readium.r2.shared.publication.ReadingProgression"))
typealias PageProgressionDirection = org.readium.r2.shared.publication.ReadingProgression

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.Publication"))
typealias Publication = org.readium.r2.shared.publication.Publication

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.link.Link"))
typealias Link = org.readium.r2.shared.publication.link.Link

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.link.Properties"))
typealias Properties = org.readium.r2.shared.publication.link.Properties

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.metadata.Metadata"))
typealias Metadata = org.readium.r2.shared.publication.metadata.Metadata

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.metadata.Contributor"))
typealias Contributor = org.readium.r2.shared.publication.metadata.Contributor

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.metadata.Collection"))
typealias Collection = org.readium.r2.shared.publication.metadata.Collection

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.metadata.Subject"))
typealias Subject = org.readium.r2.shared.publication.metadata.Subject

@Deprecated("Moved to another package", ReplaceWith("org.readium.r2.shared.publication.extensions.Encryption"))
typealias Encryption = org.readium.r2.shared.publication.extensions.Encryption

@Deprecated("Refactored into [Presentation]", ReplaceWith("org.readium.r2.shared.publication.extensions.Presentation"))
typealias Rendition = org.readium.r2.shared.publication.extensions.Presentation

@Deprecated("Refactored into [EpubLayout]", ReplaceWith("org.readium.r2.shared.publication.extensions.EpubLayout"))
typealias RenditionLayout = org.readium.r2.shared.publication.extensions.EpubLayout

@Deprecated("Refactored into [Presentation.Overflow]", ReplaceWith("org.readium.r2.shared.publication.extensions.Presentation.Overflow"))
typealias RenditionFlow = org.readium.r2.shared.publication.extensions.Presentation.Overflow

@Deprecated("Refactored into [Presentation.Orientation]", ReplaceWith("org.readium.r2.shared.publication.extensions.Presentation.Orientation"))
typealias RenditionOrientation = org.readium.r2.shared.publication.extensions.Presentation.Orientation

@Deprecated("Refactored into [Presentation.Spread]", ReplaceWith("org.readium.r2.shared.publication.extensions.Presentation.Spread"))
typealias RenditionSpread = org.readium.r2.shared.publication.extensions.Presentation.Spread

@Deprecated("Use [Publication::fromJSON] instead")
fun parsePublication(pubDict: JSONObject): org.readium.r2.shared.publication.Publication {
    return org.readium.r2.shared.publication.Publication.fromJSON(pubDict)
        ?: throw Exception("Invalid publiation")
}

@Deprecated("Use [Link::fromJSON] instead", ReplaceWith("Link.fromJSON", "org.readium.r2.shared.publication.link.Link"))
fun parseLink(linkDict: JSONObject, feedUrl: URL? = null): org.readium.r2.shared.publication.link.Link =
    org.readium.r2.shared.publication.link.Link.fromJSON(linkDict, normalizeHref = {
        if (feedUrl == null) {
            it
        } else {
            getAbsolute(it, feedUrl.toString())
        }
    }) ?: org.readium.r2.shared.publication.link.Link(href = "#")

@Deprecated("Moved to another package", ReplaceWith("removeLastComponent()", "org.readium.r2.shared.extensions.removeLastComponent"))
fun URL.removeLastComponent(): URL = removeLastComponent()
