/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.sub

import org.joda.time.DateTime
import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors

class Updated (json: JSONObject){

    var license: DateTime
    var status: DateTime

    init {
        try {
            license = DateTime(json.getString("license"))
            status = DateTime(json.getString("status"))
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.updated))
        }
    }
}