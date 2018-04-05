package org.readium.r2.lcp.Model.SubParts.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class ContentKey(json: JSONObject) {

    val encryptedValue: String
    var algorithm: URL

    init {
        try {
            encryptedValue = json.getString("encrypted_value")
            algorithm = URL(json.getString("algorithm"))
        } catch (e: Exception) {
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }

}