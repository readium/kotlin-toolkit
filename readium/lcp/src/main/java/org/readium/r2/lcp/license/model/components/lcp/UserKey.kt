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
public data class UserKey(val json: JsonObject) {
    val textHint: String
    val algorithm: String
    val keyCheck: String

    init {
        textHint = if ("text_hint" in json) {
            json.optString("text_hint")
        } else {
            throw LcpException(
                LcpError.Parsing.Encryption
            )
        }
        algorithm = if ("algorithm" in json) {
            json.optString("algorithm")
        } else {
            throw LcpException(
                LcpError.Parsing.Encryption
            )
        }
        keyCheck = if ("key_check" in json) {
            json.optString("key_check")
        } else {
            throw LcpException(
                LcpError.Parsing.Encryption
            )
        }
    }
}
