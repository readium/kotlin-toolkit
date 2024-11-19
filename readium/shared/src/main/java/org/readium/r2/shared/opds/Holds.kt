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
import org.readium.r2.shared.extensions.optPositiveInt

/**
 * Library-specific features when a specific book is unavailable but provides a hold list.
 *
 * https://drafts.opds.io/schema/properties.schema.json
 */
@Parcelize
public data class Holds(
    val total: Int? = null,
    val position: Int? = null,
) : JSONable, Parcelable {

    /**
     * Serializes an [Holds] to its JSON representation.
     */
    override fun toJSON(): JSONObject = JSONObject().apply {
        put("total", total)
        put("position", position)
    }

    public companion object {

        /**
         * Creates an [Holds] from its JSON representation.
         */
        public fun fromJSON(json: JSONObject?): Holds? {
            json ?: return null
            return Holds(
                total = json.optPositiveInt("total"),
                position = json.optPositiveInt("position")
            )
        }
    }
}
