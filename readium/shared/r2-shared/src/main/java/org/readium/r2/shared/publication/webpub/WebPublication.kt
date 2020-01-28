/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.Publication
import org.readium.r2.shared.Warning
import org.readium.r2.shared.WarningLogger
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.publication.webpub.link.Link
import org.readium.r2.shared.publication.webpub.link.LinkHrefNormalizer
import org.readium.r2.shared.publication.webpub.link.LinkHrefNormalizerIdentity
import org.readium.r2.shared.publication.webpub.metadata.Metadata
import java.io.Serializable

/**
 * Readium Web Publication.
 *
 * https://readium.org/webpub-manifest/
 * https://readium.org/webpub-manifest/schema/publication.schema.json
 *
 * This is an interface to enable object delegation in [Publication].
 */
interface WebPublicationInterface {
    val context: List<String>
    val metadata: Metadata
    val links: List<Link>
    val readingOrder: List<Link>
    val resources: List<Link>
    val tableOfContents: List<Link>
    val otherCollections: List<PublicationCollection>
}

data class WebPublication(
    override val context: List<String> = emptyList(),
    override val metadata: Metadata,
    override val links: List<Link> = emptyList(),
    override val readingOrder: List<Link> = emptyList(),
    override val resources: List<Link> = emptyList(),
    override val tableOfContents: List<Link> = emptyList(),
    override val otherCollections: List<PublicationCollection> = emptyList()
) : WebPublicationInterface, JSONable, Serializable {

    /**
     * Serializes a [WebPublication] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        putIfNotEmpty("@context", context)
        putIfNotEmpty("metadata", metadata)
        putIfNotEmpty("links", links)
        putIfNotEmpty("readingOrder", readingOrder)
        putIfNotEmpty("resources", resources)
        putIfNotEmpty("toc", tableOfContents)
        otherCollections.appendToJSONObject(this)
    }

    companion object {

        /**
         * Parses a [WebPublication] from its RWPM JSON representation.
         *
         * If the publication can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): WebPublication? {
            json ?: return null

            val context = json.optStringsFromArrayOrSingle("@context", remove = true)

            val metadata = Metadata.fromJSON(json.remove("metadata") as? JSONObject, normalizeHref, warnings)
            if (metadata == null) {
                warnings?.log(Warning.JsonParsing(WebPublication::class.java, "[metadata] is required", json))
                return null
            }

            val links = Link.fromJSONArray(json.remove("links") as? JSONArray, normalizeHref, warnings)
                .filter { it.rels.isNotEmpty() }

            // [readingOrder] used to be [spine], so we parse [spine] as a fallback.
            val readingOrderJSON = (json.remove("readingOrder") ?: json.remove("spine")) as? JSONArray
            val readingOrder = Link.fromJSONArray(readingOrderJSON, normalizeHref, warnings)
                .filter { it.type != null }

            val resources = Link.fromJSONArray(json.remove("resources") as? JSONArray, normalizeHref, warnings)
                .filter { it.type != null }

            val tableOfContents = Link.fromJSONArray(json.remove("toc") as? JSONArray, normalizeHref, warnings)

            // Parses sub-collections from the remaining JSON properties.
            val otherCollections = PublicationCollection.collectionsFromJSON(json, normalizeHref, warnings)

            return WebPublication(
                context = context,
                metadata = metadata,
                links = links,
                readingOrder = readingOrder,
                resources = resources,
                tableOfContents = tableOfContents,
                otherCollections = otherCollections
            )
        }

    }
}