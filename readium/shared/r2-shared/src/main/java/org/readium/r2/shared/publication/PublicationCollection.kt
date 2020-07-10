/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Core Collection Model
 *
 * https://readium.org/webpub-manifest/schema/subcollection.schema.json
 * Can be used as extension point in the Readium Web Publication Manifest.
 */
@Parcelize
data class PublicationCollection(
    val metadata: @WriteWith<JSONParceler> Map<String, Any> = emptyMap(),
    val links: List<Link> = emptyList(),
    val subcollections: Map<String, List<PublicationCollection>> = emptyMap()
) : JSONable, Parcelable {

    /**
     * Serializes a [PublicationCollection] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("metadata", metadata)
        putIfNotEmpty("links", links)
        subcollections.appendToJSONObject(this)
    }

    companion object {

        /**
         * Parses a [PublicationCollection] from its RWPM JSON representation.
         *
         * If the collection can't be parsed, a warning will be logged with [warnings].
         * The [links]' href and their children's will be normalized recursively using the
         * provided [normalizeHref] closure.
         */
        fun fromJSON(
            json: Any?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): PublicationCollection? {
            json ?: return null

            val links: List<Link>
            var metadata: Map<String, Any>? = null
            var subcollections: Map<String, List<PublicationCollection>>? = null

            when (json) {
                // Parses a sub-collection object.
                is JSONObject -> {
                    links = Link.fromJSONArray(json.remove("links") as? JSONArray, normalizeHref, warnings)
                    metadata = (json.remove("metadata") as? JSONObject)?.toMap()
                    subcollections = collectionsFromJSON(json, normalizeHref, warnings)
                }

                // Parses an array of links.
                is JSONArray -> {
                    links = Link.fromJSONArray(json, normalizeHref, warnings)
                }

                else -> {
                    warnings?.log(PublicationCollection::class.java, "core collection not valid")
                    return null
                }
            }

            if (links.isEmpty()) {
                warnings?.log(PublicationCollection::class.java, "core collection's [links] must not be empty")
                return null
            }

            return PublicationCollection(
                metadata = metadata ?: emptyMap(),
                links = links,
                subcollections = subcollections ?: emptyMap()
            )
        }

        /**
         * Parses a map of [PublicationCollection] indexed by their roles from its RWPM JSON representation.
         *
         * If the collection can't be parsed, a warning will be logged with [warnings].
         * The [links]' href and their children's will be normalized recursively using the
         * provided [normalizeHref] closure.
         */
        fun collectionsFromJSON(
            json: JSONObject,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): Map<String, List<PublicationCollection>> {
            val collections = mutableMapOf<String, MutableList<PublicationCollection>>()
            for (role in json.keys().asSequence().sorted()) {
                val subJSON = json.get(role)

                // Parses a list of links or a single collection object.
                val collection = fromJSON(subJSON, normalizeHref, warnings)
                if (collection != null) {
                    collections.getOrPut(role) { mutableListOf() }.add(collection)

                // Parses a list of collection objects.
                } else if (subJSON is JSONArray) {
                    collections.getOrPut(role) { mutableListOf() }.addAll(
                        subJSON.mapNotNull { fromJSON(it, normalizeHref, warnings) }
                    )
                }
            }
            return collections
        }

    }

}

/**
 * Serializes a map of [PublicationCollection] indexed by their role into a RWPM JSON representation.
 */
internal fun Map<String, List<PublicationCollection>>.toJSONObject(): JSONObject =
    appendToJSONObject(JSONObject())

/**
 * Serializes a map of [PublicationCollection] indexed by their role into a RWPM JSON representation
 * and add them to the given [jsonObject].
 */
internal fun Map<String, List<PublicationCollection>>.appendToJSONObject(jsonObject: JSONObject): JSONObject =
    jsonObject.also {
        for ((role, collections) in this) {
            if (collections.size == 1) {
                it.putIfNotEmpty(role, collections.first())
            } else {
                it.putIfNotEmpty(role, collections)
            }
        }
    }

@Deprecated("Use [subcollections[role].firstOrNull()] instead", ReplaceWith("subcollections[role].firstOrNull()"))
fun Map<String, List<PublicationCollection>>.firstWithRole(role: String): PublicationCollection? =
    get(role)?.firstOrNull()

@Deprecated("Use [subcollections[role]] instead", ReplaceWith("subcollections[role]"))
fun Map<String, List<PublicationCollection>>.findAllWithRole(role: String): List<PublicationCollection> =
    get(role) ?: emptyList()
