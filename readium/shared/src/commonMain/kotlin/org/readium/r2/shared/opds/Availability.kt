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
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.util.InstantParceler
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.Parcelize
import org.readium.r2.shared.util.TypeParceler
import org.readium.r2.shared.util.json.optNullableString
import org.readium.r2.shared.util.json.putIfNotNull
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Indicated the availability of a given resource.
 *
 * https://specs.opds.io/schema/properties.schema.json
 *
 * @param since Timestamp for the previous state change.
 * @param until Timestamp for the next state change.
 */
@Parcelize
@TypeParceler<Instant, InstantParceler>
public data class Availability(
    val state: State,
    val since: Instant? = null,
    val until: Instant? = null,
) : JSONable, Parcelable {

    public enum class State(public val value: String) {
        AVAILABLE("available"),
        UNAVAILABLE("unavailable"),
        RESERVED("reserved"),
        READY("ready"),
        ;

        public companion object : MapCompanion<String, State>(entries.toTypedArray(), State::value)
    }

    /**
     * Serializes an [Availability] to its JSON representation.
     */
    override fun toJSON(): JsonObject = buildJsonObject {
        put("state", state.value)
        putIfNotNull("since", since?.toString())
        putIfNotNull("until", until?.toString())
    }

    public companion object {

        /**
         * Creates an [Availability] from its JSON representation.
         * If the availability can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(json: JsonObject?, warnings: WarningLogger? = null): Availability? {
            val state = State(json?.optNullableString("state"))
            if (state == null) {
                warnings?.log(Availability::class, "[state] is required", json)
                return null
            }

            return Availability(
                state = state,
                since = json?.optNullableString("since")?.toInstant(),
                until = json?.optNullableString("until")?.toInstant()
            )
        }
    }
}
