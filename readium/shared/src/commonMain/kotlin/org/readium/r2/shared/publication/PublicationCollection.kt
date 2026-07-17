/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import org.readium.r2.shared.util.Parcelable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.WriteWith
import org.readium.r2.shared.util.json.JsonMapParceler
import org.readium.r2.shared.util.json.putIfNotEmpty
import org.readium.r2.shared.util.json.toMap
import org.readium.r2.shared.util.json.toJsonObject
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Core Collection Model
 *
 * https://readium.org/webpub-manifest/schema/subcollection.schema.json
 * Can be used as extension point in the Readium Web Publication Manifest.
 */
@Parcelize
public data class PublicationCollection(
    val metadata: @WriteWith<JsonMapParceler> Map<String, Any> = emptyMap(),
    val links: List<Link> = emptyList(),
    val subcollections: Map<String, List<PublicationCollection>> = emptyMap(),
) : JSONable, Parcelable {

    /**
     * Serializes a [PublicationCollection] to its RWPM JSON representation.
     */
    override fun toJSON(): JsonObject = buildJsonObject {
        put("metadata", metadata.toJsonObject() ?: JsonObject(emptyMap()))
        putIfNotEmpty("links", links)
        subcollections.appendToJSONObject(this)
    }

    public companion object {

        /**
         * Parses a [PublicationCollection] from its RWPM JSON representation.
         *
         * If the collection can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(
            json: JsonElement?,
            warnings: WarningLogger? = null,
        ): PublicationCollection? {
            json ?: return null

            val links: List<Link>
            var metadata: Map<String, Any>? = null
            var subcollections: Map<String, List<PublicationCollection>>? = null

            when (json) {
                // Parses a sub-collection object.
                is JsonObject -> {
                    val map = json.toMutableMap()
                    links = Link.fromJSONArray(
                        map.remove("links") as? JsonArray,
                        warnings
                    )
                    metadata = (map.remove("metadata") as? JsonObject)?.toMap()
                    subcollections = collectionsFromJSON(
                        JsonObject(map),
                        warnings
                    )
                }

                // Parses an array of links.
                is JsonArray -> {
                    links = Link.fromJSONArray(json, warnings)
                }

                else -> {
                    warnings?.log(PublicationCollection::class, "core collection not valid")
                    return null
                }
            }

            if (links.isEmpty()) {
                warnings?.log(
                    PublicationCollection::class,
                    "core collection's [links] must not be empty"
                )
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
         */
        public fun collectionsFromJSON(
            json: JsonObject,
            warnings: WarningLogger? = null,
        ): Map<String, List<PublicationCollection>> {
            val collections = mutableMapOf<String, MutableList<PublicationCollection>>()
            for (role in json.keys.sorted()) {
                val subJSON = json.getValue(role)

                // Parses a list of links or a single collection object.
                val collection = fromJSON(subJSON, warnings)
                if (collection != null) {
                    collections.getOrPut(role) { mutableListOf() }.add(collection)

                    // Parses a list of collection objects.
                } else if (subJSON is JsonArray) {
                    collections.getOrPut(role) { mutableListOf() }.addAll(
                        subJSON.mapNotNull {
                            fromJSON(
                                it,
                                warnings
                            )
                        }
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
internal fun Map<String, List<PublicationCollection>>.toJSONObject(): JsonObject =
    buildJsonObject { appendToJSONObject(this) }

/**
 * Serializes a map of [PublicationCollection] indexed by their role into a RWPM JSON representation
 * and add them to the given [builder].
 */
internal fun Map<String, List<PublicationCollection>>.appendToJSONObject(builder: JsonObjectBuilder) {
    for ((role, collections) in this) {
        if (collections.size == 1) {
            builder.putIfNotEmpty(role, collections.first())
        } else {
            builder.putIfNotEmpty(role, collections)
        }
    }
}
