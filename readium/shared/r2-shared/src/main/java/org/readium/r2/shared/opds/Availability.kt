/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.toIso8601String
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.log
import java.util.*

/**
 * Indicated the availability of a given resource.
 *
 * https://drafts.opds.io/schema/properties.schema.json
 *
 * @param since Timestamp for the previous state change.
 * @param until Timestamp for the next state change.
 */
@Parcelize
data class Availability(
    val state: State,
    val since: Date? = null,
    val until: Date? = null
) : JSONable, Parcelable {

    enum class State(val value: String) {
        AVAILABLE("available"),
        UNAVAILABLE("unavailable"),
        RESERVED("reserved"),
        READY("ready");

        companion object : MapCompanion<String, State>(values(), State::value)

    }

    /**
     * Serializes an [Availability] to its JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("state", state.value)
        put("since", since?.toIso8601String())
        put("until", until?.toIso8601String())
    }

    companion object {

        /**
         * Creates an [Availability] from its JSON representation.
         * If the availability can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): Availability? {
            val state = State(json?.optNullableString("state"))
            if (state == null) {
                warnings?.log(Availability::class.java, "[state] is required", json)
                return null
            }

            return Availability(
                state = state,
                since = json?.optNullableString("since")?.iso8601ToDate(),
                until = json?.optNullableString("until")?.iso8601ToDate()
            )
        }

    }

}
