package org.readium.r2.lcp.Model.SubParts.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class UserKey (json: JSONObject){

    var hint: String
    var algorithm: URL
    var keyCheck: String

    init {
        try {
            hint = json.getString("text_hint")
            algorithm = URL(json.getString("algorithm"))
            keyCheck = json.getString("key_check")
        } catch (e: Exception) {
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }
}