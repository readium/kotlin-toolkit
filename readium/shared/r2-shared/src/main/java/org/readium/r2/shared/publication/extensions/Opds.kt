/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.extensions

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.Warning
import org.readium.r2.shared.WarningLogger
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.parseObjects
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.link.Link
import org.readium.r2.shared.publication.link.Properties
import java.io.Serializable
import java.util.*

/**
 * OPDS Acquisition Object.
 *
 * https://drafts.opds.io/schema/acquisition-object.schema.json
 */
data class OpdsAcquisition(
    val type: String,
    val children: List<OpdsAcquisition> = emptyList()
) : JSONable, Serializable {

    /**
     * Serializes an [OpdsAcquisition] to its JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("type", type)
        putIfNotEmpty("child", children)
    }

    companion object {

        /**
         * Creates an [OpdsAcquisition] from its JSON representation.
         * If the acquisition can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): OpdsAcquisition? {
            val type = json?.optNullableString("type")
            if (type == null) {
                warnings?.log(Warning.JsonParsing(OpdsAcquisition::class.java, "[type] is required", json))
                return null
            }

            return OpdsAcquisition(
                type = type,
                children = fromJSONArray(json.optJSONArray("child"), warnings)
            )
        }

        /**
         * Creates a list of [OpdsAcquisition] from its JSON representation.
         * If an acquisition can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSONArray(
            json: JSONArray?,
            warnings: WarningLogger? = null
        ): List<OpdsAcquisition> {
            return json.parseObjects { fromJSON(it as? JSONObject, warnings) }
        }

    }

}

/**
 * The price of a publication in an OPDS link.
 *
 * https://drafts.opds.io/schema/properties.schema.json
 *
 * @param currency Currency for the price, eg. EUR.
 * @param value Price value, should only be used for display purposes, because of precision issues
 *     inherent with Double and the JSON parsing.
 */
data class OpdsPrice(
    val currency: String,
    val value: Double
) : JSONable, Serializable {

    /**
     * Serializes an [OpdsPrice] to its JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("currency", currency)
        put("value", value)
    }

    companion object {

        /**
         * Creates an [OpdsPrice] from its JSON representation.
         * If the price can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): OpdsPrice? {
            val currency = json?.optNullableString("currency")
            val value = json?.optPositiveDouble("value")
            if (currency == null || value == null) {
                warnings?.log(Warning.JsonParsing(OpdsPrice::class.java, "[currency] and [value] are required", json))
                return null
            }

            return OpdsPrice(currency = currency, value = value)
        }

    }

}

/**
 * Library-specific features when a specific book is unavailable but provides a hold list.
 *
 * https://drafts.opds.io/schema/properties.schema.json
 */
data class OpdsHolds(
    val total: Int? = null,
    val position: Int? = null
) : JSONable, Serializable {

    /**
     * Serializes an [OpdsHolds] to its JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("total", total)
        put("position", position)
    }

    companion object {

        /**
         * Creates an [OpdsHolds] from its JSON representation.
         */
        fun fromJSON(json: JSONObject?): OpdsHolds? {
            json ?: return null
            return OpdsHolds(
                total = json.optPositiveInt("total"),
                position = json.optPositiveInt("position")
            )
        }

    }

}

/**
 * Library-specific feature that contains information about the copies that a library has acquired.
 *
 * https://drafts.opds.io/schema/properties.schema.json
 */
data class OpdsCopies(
    val total: Int? = null,
    val available: Int? = null
) : JSONable, Serializable {

    /**
     * Serializes an [OpdsCopies] to its JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("total", total)
        put("available", available)
    }

    companion object {

        /**
         * Creates an [OpdsCopies] from its JSON representation.
         */
        fun fromJSON(json: JSONObject?): OpdsCopies? {
            json ?: return null
            return OpdsCopies(
                total = json.optPositiveInt("total"),
                available = json.optPositiveInt("available")
            )
        }

    }

}

/**
 * Indicated the availability of a given resource.
 *
 * https://drafts.opds.io/schema/properties.schema.json
 *
 * @param since Timestamp for the previous state change.
 * @param until Timestamp for the next state change.
 */
data class OpdsAvailability(
    val state: State,
    val since: Date? = null,
    val until: Date? = null
) : JSONable, Serializable {

    enum class State(val value: String) {
        AVAILABLE("available"),
        UNAVAILABLE("unavailable"),
        RESERVED("reserved"),
        READY("ready");

        companion object {
            fun from(value: String?) = State.values().firstOrNull { it.value == value }
        }
    }

    /**
     * Serializes an [OpdsAvailability] to its JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("state", state.value)
        put("since", since?.toIso8601String())
        put("until", until?.toIso8601String())
    }

    companion object {

        /**
         * Creates an [OpdsAvailability] from its JSON representation.
         * If the availability can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): OpdsAvailability? {
            val state = State.from(json?.optNullableString("state"))
            if (state == null) {
                warnings?.log(Warning.JsonParsing(OpdsAvailability::class.java, "[state] is required", json))
                return null
            }

            return OpdsAvailability(
                state = state,
                since = json?.optNullableString("since")?.toIso8601Date(),
                until = json?.optNullableString("until")?.toIso8601Date()
            )
        }

    }

}


// OPDS extensions for [Publication]

val Publication.images: List<Link> get() = linksWithRole("images")


// OPDS extensions for link [Properties].
// https://drafts.opds.io/schema/properties.schema.json

/**
 * Provides a hint about the expected number of items returned.
 */
val Properties.numberOfItems: Int?
    get() = (this["numberOfItems"] as? Int)
        ?.takeIf { it >= 0 }

/**
 * The price of a publication is tied to its acquisition link.
 */
val Properties.price: OpdsPrice?
    get() = (this["price"] as? Map<*, *>)
        ?.let { OpdsPrice.fromJSON(JSONObject(it)) }

/**
 * Indirect acquisition provides a hint for the expected media type that will be acquired after
 * additional steps.
 */
val Properties.indirectAcquisitions: List<OpdsAcquisition>
    get() = (this["indirectAcquisition"] as? List<*>)
        ?.mapNotNull {
            if (it !is Map<*, *>) {
                null
            } else {
                OpdsAcquisition.fromJSON(JSONObject(it))
            }
        }
        ?: emptyList()

/**
 * Library-specific features when a specific book is unavailable but provides a hold list.
 */
val Properties.holds: OpdsHolds?
    get() = (this["holds"] as? Map<*, *>)
        ?.let { OpdsHolds.fromJSON(JSONObject(it)) }

/**
 * Library-specific feature that contains information about the copies that a library has acquired.
 */
val Properties.copies: OpdsCopies?
    get() = (this["copies"] as? Map<*, *>)
        ?.let { OpdsCopies.fromJSON(JSONObject(it)) }

/**
 * Indicated the availability of a given resource.
 */
val Properties.availability: OpdsAvailability?
    get() = (this["availability"] as? Map<*, *>)
        ?.let { OpdsAvailability.fromJSON(JSONObject(it)) }
