package org.readium.r2.lcp.Model.SubParts

import org.joda.time.DateTime
import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.util.*

class Updated (json: JSONObject){

    var license: Date
    var status: Date

    init {
        try {
            license = DateTime(json.getString("license")).toDate()
            status = DateTime(json.getString("status")).toDate()
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.updated))
        }
    }
}