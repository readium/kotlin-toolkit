/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model.components.lsd

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.KotlinInstantSerializer

@Serializable
public data class Event(
    @SerialName("type")
    val type: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("id")
    val id: String = "",
    @SerialName("timestamp")
    @Serializable(with = KotlinInstantSerializer::class)
    val date: Instant? = null,
) {
    @Deprecated("Use kotlinx.serialization to serialize the object")
    val json: JSONObject get() = JSONObject()

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
