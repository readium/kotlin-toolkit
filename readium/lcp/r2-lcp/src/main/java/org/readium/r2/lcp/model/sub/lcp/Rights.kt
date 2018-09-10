/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.sub.lcp

import org.joda.time.DateTime
import org.json.JSONObject

class Rights (json: JSONObject){
    var print: Int? = null
    var copy: Int? = null
    var start: DateTime? = null
    var end: DateTime? = null
    var potentialEnd: DateTime? = null

    init {
        if (json.has("print")) {
            print = json.getInt("print")
        }
        if (json.has("copy")) {
            copy = json.getInt("copy")
        }
        if (json.has("start")) {
            start = DateTime(json.getString("start"))
        }
        if (json.has("end")) {
            val endDate = DateTime(json.getString("end"))
            end = endDate
            potentialEnd = start?.plusDays(60)
        }

    }
}