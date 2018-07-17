/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.Model.SubParts.lcp

import org.joda.time.DateTime
import org.json.JSONObject
import java.util.*

class Rights (json: JSONObject){
    var print: Int? = null
    var copy: Int? = null
    var start: Date? = null
    var end: Date? = null
    var potentialEnd: Date? = null

    init {
        if (json.has("print")) {
            print = json.getInt("print")
        }
        if (json.has("copy")) {
            copy = json.getInt("copy")
        }
        if (json.has("start")) {
            start = DateTime(json.getString("start")).toDate()
        }
        if (json.has("end")) {
            val enddate = DateTime(json.getString("end"))
            end = enddate.toDate()
            potentialEnd = enddate.plusMonths(1).toDate()
        }

    }
}