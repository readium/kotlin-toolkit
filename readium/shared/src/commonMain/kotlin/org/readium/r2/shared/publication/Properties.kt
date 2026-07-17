/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import org.readium.r2.shared.util.Parcelable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.WriteWith
import org.readium.r2.shared.util.json.JsonMapParceler
import org.readium.r2.shared.util.json.putAll
import org.readium.r2.shared.util.json.toMap

/**
 * Properties associated to the linked resource.
 *
 * This is opened for extensions.
 * https://readium.org/webpub-manifest/schema/link.schema.json
 */
@Parcelize
public data class Properties(
    val otherProperties: @WriteWith<JsonMapParceler> Map<String, Any> = emptyMap(),
) : JSONable, Parcelable {

    /**
     * Serializes a [Properties] to its RWPM JSON representation.
     */
    override fun toJSON(): JsonObject = buildJsonObject { putAll(otherProperties) }

    /**
     * Makes a copy of this [Properties] after merging in the given additional other [properties].
     */
    public fun add(properties: Map<String, Any>): Properties {
        val otherProperties = otherProperties.toMutableMap()
        otherProperties.putAll(properties)
        return copy(otherProperties = otherProperties)
    }

    /**
     * Makes a copy of this [Properties] after merging in the given additional other [properties].
     */
    public fun add(properties: Properties): Properties =
        add(properties.otherProperties)

    /**
     * Syntactic sugar to access the [otherProperties] values by subscripting [Properties] directly.
     * `properties["price"] == properties.otherProperties["price"]`
     */
    public operator fun get(key: String): Any? = otherProperties[key]

    public companion object {

        /**
         * Creates a [Properties] from its RWPM JSON representation.
         */
        public fun fromJSON(json: JsonObject?): Properties = Properties(
            otherProperties = json?.toMap() ?: emptyMap()
        )
    }
}
