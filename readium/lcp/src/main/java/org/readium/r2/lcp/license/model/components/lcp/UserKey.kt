/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model.components.lcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject

@Serializable
public data class UserKey(
    @SerialName("text_hint")
    val textHint: String,
    @SerialName("algorithm")
    val algorithm: String,
    @SerialName("key_check")
    val keyCheck: String,
) {
    @Deprecated("Use kotlinx.serialization to serialize the object")
    val json: JSONObject get() = JSONObject()
}
