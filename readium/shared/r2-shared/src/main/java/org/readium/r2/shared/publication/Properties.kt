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
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.extensions.toMap

/**
 * Properties associated to the linked resource.
 *
 * This is opened for extensions.
 * https://readium.org/webpub-manifest/schema/link.schema.json
 */
@Parcelize
data class Properties(
    val otherProperties: @WriteWith<JSONParceler> Map<String, Any> = emptyMap()
) : JSONable, Parcelable {

    /**
     * Serializes a [Properties] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject(otherProperties)

    /**
     * Makes a copy of this [Properties] after merging in the given additional other [properties].
     */
    fun add(properties: Map<String, Any>): Properties {
        val otherProperties = otherProperties.toMutableMap()
        otherProperties.putAll(properties)
        return copy(otherProperties = otherProperties)
    }

    /**
     * Syntactic sugar to access the [otherProperties] values by subscripting [Properties] directly.
     * `properties["price"] == properties.otherProperties["price"]`
     */
    operator fun get(key: String): Any? = otherProperties[key]

    companion object {

        /**
         * Creates a [Properties] from its RWPM JSON representation.
         */
        fun fromJSON(json: JSONObject?) = Properties(
            otherProperties = json?.toMap() ?: emptyMap()
        )

    }

}
