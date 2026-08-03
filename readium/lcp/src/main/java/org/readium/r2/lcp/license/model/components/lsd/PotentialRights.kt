/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class, ExperimentalReadiumApi::class)

package org.readium.r2.lcp.license.model.components.lsd

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import org.readium.r2.lcp.license.model.LcpJson
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.KotlinInstantSerializer

@Serializable
public data class PotentialRights(
    @SerialName("end")
    @Serializable(with = KotlinInstantSerializer::class)
    val end: Instant? = null,
) {
    @Deprecated("Use kotlinx.serialization to serialize the object")
    val json: JSONObject get() = JSONObject(LcpJson.encodeToString(this))
}
