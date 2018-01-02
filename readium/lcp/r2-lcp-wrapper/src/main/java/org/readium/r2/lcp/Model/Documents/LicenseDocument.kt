package org.readium.r2.lcp.Model.Documents

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpErrorCase
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.Model.SubParts.Link
import org.readium.r2.lcp.Model.SubParts.lcp.Encryption
import org.readium.r2.lcp.Model.SubParts.lcp.Rights
import org.readium.r2.lcp.Model.SubParts.lcp.User
import org.readium.r2.lcp.Model.SubParts.lcp.Signature
import java.net.URL
import java.util.*

class LicenseDocument(data: ByteArray) {

    var id: String
    var issued: Date
    var updated: Date? = null
    var provider: URL
    var encryption: Encryption
    var links = listOf<Link>()
    var rights: Rights
    var user: User
    var signature: Signature
    var json: JSONObject

    val status = "status"

    init {
        json = JSONObject(data.toString())
        try {
            id = json.getString("id")
            issued = Date(json.getString("issued"))
            provider = URL(json.getString("provider"))
        } catch (e: Exception) {
            throw Exception("Lcp parsing error")
        }
        encryption = Encryption(JSONObject(json.getString("encryption")))
        links = Link(json).parseLinks("links")
        rights = Rights(json.getJSONObject("rights"))
        rights.potentialEnd = Date(json.getJSONObject("potential_rights").getString("end"))
        user = User(json.getJSONObject("user"))
        signature = Signature(json.getJSONObject("signature"))
        updated = Date(json.getString("updated"))
        if (link("hint") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.hintLinkNotFound))
        }
        if (link("publication") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.publicationLinkNotFound))
        }
    }

    fun dateOfLastUpdate() = if (updated != null) updated!! else issued

    fun link(rel: String) = links.firstOrNull{ it.rel.contains(rel) }

    fun getHint() = encryption.userKey.hint

}