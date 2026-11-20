/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model.components.lcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject
import org.readium.r2.lcp.license.model.LcpJson

@Serializable
public data class Signature(
    @SerialName("algorithm")
    val algorithm: String,
    @SerialName("certificate")
    val certificate: String,
    @SerialName("value")
    val value: String,
) {
    @Deprecated("No longer available")
    val json: JSONObject get() = JSONObject(LcpJson.encodeToString(this))
}
