/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.sub.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class ContentKey(json: JSONObject) {

    private val encryptedValue: String
    private var algorithm: URL

    init {
        try {
            encryptedValue = json.getString("encrypted_value")
            algorithm = URL(json.getString("algorithm"))
        } catch (e: Exception) {
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }

}