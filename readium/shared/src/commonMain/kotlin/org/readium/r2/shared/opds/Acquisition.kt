/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.opds

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.Parcelable
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.json.optJsonArray
import org.readium.r2.shared.util.json.optNullableString
import org.readium.r2.shared.util.json.parseObjects
import org.readium.r2.shared.util.json.putIfNotEmpty
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * OPDS Acquisition Object.
 *
 * https://specs.opds.io/schema/acquisition-object.schema.json
 */
@Parcelize
public data class Acquisition(
    val type: String,
    val children: List<Acquisition> = emptyList(),
) : JSONable, Parcelable {

    /** Media type of the resource to acquire. */
    val mediaType: MediaType get() =
        MediaType(type) ?: MediaType.BINARY

    /**
     * Serializes an [Acquisition] to its JSON representation.
     */
    override fun toJSON(): JsonObject = buildJsonObject {
        put("type", type)
        putIfNotEmpty("child", children)
    }

    public companion object {

        /**
         * Creates an [Acquisition] from its JSON representation.
         * If the acquisition can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(json: JsonObject?, warnings: WarningLogger? = null): Acquisition? {
            val type = json?.optNullableString("type")
            if (type == null) {
                warnings?.log(Acquisition::class, "[type] is required", json)
                return null
            }

            return Acquisition(
                type = type,
                children = fromJSONArray(json.optJsonArray("child"), warnings)
            )
        }

        /**
         * Creates a list of [Acquisition] from its JSON representation.
         * If an acquisition can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSONArray(
            json: JsonArray?,
            warnings: WarningLogger? = null,
        ): List<Acquisition> {
            return json.parseObjects { fromJSON(it as? JsonObject, warnings) }
        }
    }
}
