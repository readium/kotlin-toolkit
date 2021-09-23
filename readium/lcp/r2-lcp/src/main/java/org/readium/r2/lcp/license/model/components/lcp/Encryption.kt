/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpException

data class Encryption(val json: JSONObject) {
    val profile: String
    val contentKey: ContentKey
    val userKey: UserKey

    init {
        profile = if (json.has("profile")) json.getString("profile") else throw LcpException.Parsing.Encryption
        contentKey = if (json.has("content_key")) ContentKey(json.getJSONObject("content_key")) else throw LcpException.Parsing.Encryption
        userKey = if (json.has("user_key")) UserKey(json.getJSONObject("user_key")) else throw LcpException.Parsing.Encryption
    }
}
