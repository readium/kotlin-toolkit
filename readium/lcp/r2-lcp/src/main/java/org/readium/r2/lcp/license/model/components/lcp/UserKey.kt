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

data class UserKey(val json: JSONObject) {
    val textHint: String
    val algorithm: String
    val keyCheck: String

    init {
        textHint = if (json.has("text_hint")) json.getString("text_hint") else throw LcpException.Parsing.Encryption
        algorithm = if (json.has("algorithm")) json.getString("algorithm") else throw LcpException.Parsing.Encryption
        keyCheck = if (json.has("key_check")) json.getString("key_check") else throw LcpException.Parsing.Encryption
    }
}
