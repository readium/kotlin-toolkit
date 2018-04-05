package org.readium.r2.lcp.Model.SubParts.lcp

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class Encryption(json: JSONObject) {
    var profile: URL
    var contentKey: ContentKey
    var userKey: UserKey

    init {
        try {
            profile = URL(json.getString("profile"))
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.encryption))
        }
        contentKey = ContentKey(json.getJSONObject("content_key"))
        userKey = UserKey(json.getJSONObject("user_key"))
    }
}