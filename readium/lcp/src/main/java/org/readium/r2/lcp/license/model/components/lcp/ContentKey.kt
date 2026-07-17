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
import org.readium.r2.shared.util.json.optString

@OptIn(InternalReadiumApi::class)
public data class ContentKey(val json: JsonObject) {
    val algorithm: String
    val encryptedValue: String

    init {
        algorithm = if ("algorithm" in json) {
            json.optString("algorithm")
        } else {
            throw LcpException(
                LcpError.Parsing.Encryption
            )
        }
        encryptedValue = if ("encrypted_value" in json) {
            json.optString("encrypted_value")
        } else {
            throw LcpException(
                LcpError.Parsing.Encryption
            )
        }
    }
}
