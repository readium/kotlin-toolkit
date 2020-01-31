/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.parseObjects
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.log
import java.io.Serializable

/**
 * OPDS Acquisition Object.
 *
 * https://drafts.opds.io/schema/acquisition-object.schema.json
 */
data class Acquisition(
    val type: String,
    val children: List<Acquisition> = emptyList()
) : JSONable, Serializable {

    /**
     * Serializes an [Acquisition] to its JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("type", type)
        putIfNotEmpty("child", children)
    }

    companion object {

        /**
         * Creates an [Acquisition] from its JSON representation.
         * If the acquisition can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: JSONObject?, warnings: WarningLogger<JsonWarning>? = null): Acquisition? {
            val type = json?.optNullableString("type")
            if (type == null) {
                warnings?.log(Acquisition::class.java, "[type] is required", json)
                return null
            }

            return Acquisition(
                type = type,
                children = fromJSONArray(json.optJSONArray("child"), warnings)
            )
        }

        /**
         * Creates a list of [Acquisition] from its JSON representation.
         * If an acquisition can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSONArray(
            json: JSONArray?,
            warnings: WarningLogger<JsonWarning>? = null
        ): List<Acquisition> {
            return json.parseObjects { fromJSON(it as? JSONObject, warnings) }
        }

    }

    @Deprecated("Use [type] instead", ReplaceWith("type"))
    val typeAcquisition: String?
        get() = type

    @Deprecated("Use [children] instead", ReplaceWith("children"))
    val child: List<Acquisition>
        get() = children

}

@Deprecated("Renamed into [Acquisition]", ReplaceWith("Acquisition"))
typealias IndirectAcquisition = Acquisition

@Deprecated("Use [Acquisition::fromJSON] instead", ReplaceWith("Acquisition.fromJSON"))
fun parseIndirectAcquisition(indirectAcquisitionDict: JSONObject): Acquisition =
    Acquisition.fromJSON(indirectAcquisitionDict)
        ?: throw Exception("Invalid indirect acquisition")
