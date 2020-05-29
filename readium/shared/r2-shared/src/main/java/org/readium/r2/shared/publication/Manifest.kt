/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

@Parcelize
data class Manifest(
    val context: List<String> = emptyList(),
    val metadata: Metadata,
    // FIXME: Currently Readium requires to set the [Link] with [rel] "self" when adding it to the
    //     server. So we need to keep [links] as a mutable property.
    var links: List<Link> = emptyList(),
    val readingOrder: List<Link> = emptyList(),
    val resources: List<Link> = emptyList(),
    val tableOfContents: List<Link> = emptyList(),
    val otherCollections: List<PublicationCollection> = emptyList(),

    // FIXME: To be refactored, with the TYPE and EXTENSION enums as well
    var type: Publication.TYPE = Publication.TYPE.EPUB,
    var version: Double = 0.0

) : JSONable, Parcelable {

    /**
     * Serializes a [Publication] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        putIfNotEmpty("@context", context)
        put("metadata", metadata.toJSON())
        put("links", links.toJSON())
        put("readingOrder", readingOrder.toJSON())
        putIfNotEmpty("resources", resources)
        putIfNotEmpty("toc", tableOfContents)
        otherCollections.appendToJSONObject(this)
    }

    /**
     * Returns the RWPM JSON representation for this manifest, as a string.
     */
    override fun toString(): String = toJSON().toString().replace("\\/", "/")

    companion object {

        fun fromJSON(json: JSONObject?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): Manifest? =
            fromJSON(json, normalizeHref, null)

        /**
         * Parses a [Publication] from its RWPM JSON representation.
         *
         * If the publication can't be parsed, a warning will be logged with [warnings].
         * https://readium.org/webpub-manifest/
         * https://readium.org/webpub-manifest/schema/publication.schema.json
         */
        internal fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger<JsonWarning>?
        ): Manifest? {
            json ?: return null

            val context = json.optStringsFromArrayOrSingle("@context", remove = true)

            val metadata = Metadata.fromJSON(json.remove("metadata") as? JSONObject, normalizeHref, warnings)
            if (metadata == null) {
                warnings?.log(Publication::class.java, "[metadata] is required", json)
                return null
            }

            val links = Link.fromJSONArray(json.remove("links") as? JSONArray, normalizeHref, warnings)

            // [readingOrder] used to be [spine], so we parse [spine] as a fallback.
            val readingOrderJSON = (json.remove("readingOrder") ?: json.remove("spine")) as? JSONArray
            val readingOrder = Link.fromJSONArray(readingOrderJSON, normalizeHref, warnings)
                .filter { it.type != null }

            val resources = Link.fromJSONArray(json.remove("resources") as? JSONArray, normalizeHref, warnings)
                .filter { it.type != null }

            val tableOfContents = Link.fromJSONArray(json.remove("toc") as? JSONArray, normalizeHref, warnings)

            // Parses sub-collections from the remaining JSON properties.
            val otherCollections = PublicationCollection.collectionsFromJSON(json, normalizeHref, warnings)

            return Manifest(
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

