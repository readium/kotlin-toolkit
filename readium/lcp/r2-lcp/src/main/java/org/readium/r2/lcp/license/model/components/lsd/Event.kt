/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lsd

import org.joda.time.DateTime
import org.json.JSONObject

data class Event(val json: JSONObject) {
    val type: String
    val name: String
    val id: String
    val date: DateTime

    enum class EventType(val rawValue: String) {
        register("register"),
        renew("renew"),
        `return`("return"),
        revoke("revoke"),
        cancel("cancel");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    init {
        type = try {json.getString("type")}finally { }
        name = try {json.getString("name")}finally { }
        id = try {json.getString("id") }finally { }
        date = try {DateTime(json.getString("timestamp")) }finally { }

    }
}
