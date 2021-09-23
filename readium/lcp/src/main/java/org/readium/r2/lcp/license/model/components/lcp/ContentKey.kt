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

data class ContentKey(val json: JSONObject) {
    val algorithm: String
    val encryptedValue: String

    init {
        algorithm = if (json.has("algorithm")) json.getString("algorithm") else throw LcpException.Parsing.Encryption
        encryptedValue = if (json.has("encrypted_value")) json.getString("encrypted_value") else throw LcpException.Parsing.Encryption
    }
}
