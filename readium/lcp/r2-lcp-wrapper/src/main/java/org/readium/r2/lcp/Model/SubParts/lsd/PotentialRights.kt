package org.readium.r2.lcp.Model.SubParts.lsd

import org.json.JSONObject
import java.util.*

class PotentialRights(json: JSONObject) {
    var end: Date?

    init {
        end = Date(json.getString("end"))
    }
}