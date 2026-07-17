/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import org.readium.r2.shared.util.Parcelable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.json.optNullableString
import org.readium.r2.shared.util.json.putIfNotNull
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Publications can indicate whether they allow third parties to use their content for text and data
 * mining purposes using the [TDM Rep protocol](https://www.w3.org/community/tdmrep/), as defined in
 * a [W3C Community Group Report](https://www.w3.org/community/reports/tdmrep/CG-FINAL-tdmrep-20240510/).
 *
 * https://github.com/readium/webpub-manifest/blob/master/schema/metadata.schema.json
 *
 * @param policy URL pointing to a TDM Policy set be the rightsholder.
 */
@Parcelize
public data class Tdm(
    val reservation: Reservation,
    val policy: AbsoluteUrl? = null,
) : JSONable, Parcelable {

    @Parcelize
    public data class Reservation(public val value: String) : Parcelable {

        public companion object {
            /**
             * All TDM rights are reserved. If a TDM Policy is set, TDM Agents MAY use it to get
             * information on how they can acquire from the rightsholder an authorization to mine
             * the content.
             */
            public val ALL: Reservation = Reservation("all")

            /**
             * TDM rights are not reserved. TDM agents can mine the content for TDM purposes without
             * having to contact the rightsholder.
             */
            public val NONE: Reservation = Reservation("none")
        }
    }

    override fun toJSON(): JsonObject = buildJsonObject {
        put("reservation", reservation.value)
        putIfNotNull("policy", policy?.toString())
    }

    public companion object {

        /**
         * Parses a [Tdm] from its RWPM JSON representation.
         *
         * If the TDM can't be parsed, a warning will be logged with [warnings].
         */
        @OptIn(InternalReadiumApi::class)
        public fun fromJSON(
            json: JsonElement?,
            warnings: WarningLogger? = null,
        ): Tdm? {
            if (json !is JsonObject) return null
            val reservation = json.optNullableString("reservation")?.let { Reservation(it) }
            val policy = json.optNullableString("policy")?.let { AbsoluteUrl(it) }

            if (reservation == null) {
                warnings?.log(Tdm::class, "no valid reservation in TDM object", json)
                return null
            }

            return Tdm(
                reservation = reservation,
                policy = policy
            )
        }
    }
}
