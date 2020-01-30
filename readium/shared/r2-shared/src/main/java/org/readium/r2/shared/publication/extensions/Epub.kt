/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.extensions

import org.json.JSONObject
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.link.Link
import org.readium.r2.shared.publication.link.Properties
import org.readium.r2.shared.publication.metadata.Metadata

/**
 * Hints how the layout of the resource should be presented.
 * https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json
 */
enum class EpubLayout(val value: String) {
    FIXED("fixed"),
    REFLOWABLE("reflowable");

    companion object {

        fun from(value: String?) = EpubLayout.values().firstOrNull { it.value == value }

        /**
         * Resolves from an EPUB rendition:layout property.
         */
        fun fromEpub(value: String, fallback: EpubLayout = REFLOWABLE): EpubLayout {
            return when(value) {
                "reflowable" -> REFLOWABLE
                "pre-paginated" -> FIXED
                else -> fallback
            }
        }

    }

}


// EPUB extensions for [Publication].
// https://readium.org/webpub-manifest/schema/extensions/epub/subcollections.schema.json
// https://idpf.github.io/epub-vocabs/structure/#navigation

/**
 * Provides navigation to positions in the Publication content that correspond to the locations of
 * page boundaries present in a print source being represented by this EPUB Publication.
 */
val Publication.pageList: List<Link> get() = linksWithRole("page-list")

/**
 * Identifies fundamental structural components of the publication in order to enable Reading
 * Systems to provide the User efficient access to them.
 */
val Publication.landmarks: List<Link> get() = linksWithRole("landmarks")

val Publication.listOfAudioClips: List<Link> get() = linksWithRole("loa")
val Publication.listOfIllustrations: List<Link> get() = linksWithRole("loi")
val Publication.listOfTables: List<Link> get() = linksWithRole("lot")
val Publication.listOfVideoClips: List<Link> get() = linksWithRole("lov")


// EPUB extensions for [Metadata].
// https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json

/**
 * Hints how the layout of the resource should be presented.
 */
val Metadata.layout: EpubLayout?
    get() = EpubLayout.from(this["layout"] as? String)


// EPUB extensions for link [Properties].
// https://readium.org/webpub-manifest/schema/extensions/epub/properties.schema.json

/**
 * Identifies content contained in the linked resource, that cannot be strictly identified using a
 * media type.
 */
val Properties.contains: Set<String>
    get() = (this["contains"] as? List<*>)
        ?.filterIsInstance(String::class.java)
        ?.toSet()
        ?: emptySet()

/**
 * Hints how the layout of the resource should be presented.
 */
val Properties.layout: EpubLayout?
    get() = EpubLayout.from(this["layout"] as? String)

/**
 * Location of a media-overlay for the resource referenced in the Link Object.
 */
val Properties.mediaOverlay: String?
    get() = this["mediaOverlay"] as? String
