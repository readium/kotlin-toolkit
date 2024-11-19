/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model.components.lsd

import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.util.Instant

public data class Event(val json: JSONObject) {
    val type: String = json.optNullableString("type") ?: ""
    val name: String = json.optNullableString("name") ?: ""
    val id: String = json.optNullableString("id") ?: ""
    val date: Instant? = json.optNullableString("timestamp")?.let { Instant.parse(it) }

    public enum class EventType(public val value: String) {
        Register("register"),
        Renew("renew"),
        Return("return"),
        Revoke("revoke"),
        Cancel("cancel"),
        ;

        public companion object {
            public operator fun invoke(value: String): EventType? = entries.firstOrNull { it.value == value }
        }
    }
}
