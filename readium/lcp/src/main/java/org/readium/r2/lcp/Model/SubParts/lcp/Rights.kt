package org.readium.r2.lcp.Model.SubParts.lcp

import android.text.format.DateUtils
import org.json.JSONObject
import java.util.*

class Rights (json: JSONObject){
    var print: Int
    var copy: Int
    var start: Date?
    var end: Date?
    var potentialEnd: Date?

    init {
        print = json.getInt("print")
        copy = json.getInt("copy")
        start = Date(json.getString("start"))
        end = Date(json.getString("end"))
        potentialEnd = Date()
        potentialEnd!!.month += 1
    }
}