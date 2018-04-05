package org.readium.r2.lcp.Model.SubParts.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class Signature (json: JSONObject) {

    var algorithm: URL
    var certificate: String
    var value: String

    init {
        try {
            algorithm = URL(json.getString("algorithm"))
            certificate = json.getString("certificate")
            value = json.getString("value")
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.signature))
        }
    }

}