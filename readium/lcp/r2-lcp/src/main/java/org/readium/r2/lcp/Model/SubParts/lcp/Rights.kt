package org.readium.r2.lcp.Model.SubParts.lcp

import org.joda.time.DateTime
import org.json.JSONObject
import java.util.*

class Rights (json: JSONObject){
    var print: Int
    var copy: Int
    var start: String? = null
    var end: String? = null
    var potentialEnd: String?

    init {
        print = json.getInt("print")
        copy = json.getInt("copy")
        if (json.has("start")) {
            start = DateTime(json.getString("start")).toDate().toString()
        }
        if (json.has("end")) {
            end = DateTime(json.getString("end")).toDate().toString()
        }

        potentialEnd = DateTime.now().plusMonths(1).toDate().toString()
    }
}