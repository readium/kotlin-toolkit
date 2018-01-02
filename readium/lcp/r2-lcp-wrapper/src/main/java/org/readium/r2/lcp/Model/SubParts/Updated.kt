package org.readium.r2.lcp.Model.SubParts

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.util.*

class Updated (json: JSONObject){

    var license: Date
    var status: Date

    init {
        try {
            license = Date(json.getString("license"))
            status = Date(json.getString("status"))
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.updated))
        }
    }
}