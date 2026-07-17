/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.opds

import org.readium.r2.shared.util.Parcelable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.json.optPositiveInt
import org.readium.r2.shared.util.json.putIfNotNull

/**
 * Library-specific feature that contains information about the copies that a library has acquired.
 *
 * https://specs.opds.io/schema/properties.schema.json
 */
@Parcelize
public data class Copies(
    val total: Int? = null,
    val available: Int? = null,
) : JSONable, Parcelable {

    /**
     * Serializes an [Copies] to its JSON representation.
     */
    override fun toJSON(): JsonObject = buildJsonObject {
        putIfNotNull("total", total)
        putIfNotNull("available", available)
    }

    public companion object {

        /**
         * Creates an [Copies] from its JSON representation.
         */
        public fun fromJSON(json: JsonObject?): Copies? {
            json ?: return null
            return Copies(
                total = json.optPositiveInt("total"),
                available = json.optPositiveInt("available")
            )
        }
    }
}
