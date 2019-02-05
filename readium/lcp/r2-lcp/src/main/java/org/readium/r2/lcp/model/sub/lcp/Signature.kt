/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.sub.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class Signature (json: JSONObject) {

    private var algorithm: URL
    var certificate: String
    private var value: String

    init {
        try {
            algorithm = URL(json.getString("algorithm"))
            certificate = json.getString("certificate")
            value = json.getString("value")
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.signature))
        }
    }

}