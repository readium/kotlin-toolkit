/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model.components.lcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Encryption(
    @SerialName("profile")
    val profile: String,
    @SerialName("content_key")
    val contentKey: ContentKey,
    @SerialName("user_key")
    val userKey: UserKey,
)
