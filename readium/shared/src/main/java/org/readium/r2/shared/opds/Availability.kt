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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.util.Instant
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Indicated the availability of a given resource.
 *
 * https://drafts.opds.io/schema/properties.schema.json
 *
 * @param since Timestamp for the previous state change.
 * @param until Timestamp for the next state change.
 */
@Parcelize
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
    override fun toJSON(): JSONObject = JSONObject().apply {
        put("state", state.value)
        put("since", since?.toString())
        put("until", until?.toString())
    }

    public companion object {

        /**
         * Creates an [Availability] from its JSON representation.
         * If the availability can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): Availability? {
            val state = State(json?.optNullableString("state"))
            if (state == null) {
                warnings?.log(Availability::class.java, "[state] is required", json)
                return null
            }

            return Availability(
                state = state,
                since = json?.optNullableString("since")?.let { Instant.parse(it) },
                until = json?.optNullableString("until")?.let { Instant.parse(it) }
            )
        }
    }
}
