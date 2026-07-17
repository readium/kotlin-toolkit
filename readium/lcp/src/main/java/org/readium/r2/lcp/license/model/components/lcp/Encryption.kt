/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lcp

import kotlinx.serialization.json.JsonObject
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.json.optJsonObject
import org.readium.r2.shared.util.json.optString

@OptIn(InternalReadiumApi::class)
public data class Encryption(val json: JsonObject) {
    val profile: String
    val contentKey: ContentKey
    val userKey: UserKey

    init {
        profile = if ("profile" in json) {
            json.optString("profile")
        } else {
            throw LcpException(
                LcpError.Parsing.Encryption
            )
        }
        contentKey = json.optJsonObject("content_key")
            ?.let { ContentKey(it) }
            ?: throw LcpException(
                LcpError.Parsing.Encryption
            )
        userKey = json.optJsonObject("user_key")
            ?.let { UserKey(it) }
            ?: throw LcpException(
                LcpError.Parsing.Encryption
            )
    }
}
