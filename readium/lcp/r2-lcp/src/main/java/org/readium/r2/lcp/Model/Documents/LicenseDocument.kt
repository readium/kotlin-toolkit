package org.readium.r2.lcp.Model.Documents

import org.json.JSONObject
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpErrorCase
import org.readium.r2.lcp.Model.SubParts.Link
import org.readium.r2.lcp.Model.SubParts.lcp.Encryption
import org.readium.r2.lcp.Model.SubParts.lcp.Rights
import org.readium.r2.lcp.Model.SubParts.lcp.User
import org.readium.r2.lcp.Model.SubParts.lcp.Signature
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import org.joda.time.DateTime
import org.json.JSONArray
import org.readium.r2.lcp.Model.SubParts.parseLinks


class LicenseDocument(data: ByteArray) {

    var id: String
    var issued: String
    var updated: String? = null
    var provider: URL
    var encryption: Encryption
    var links = listOf<Link>()
    var rights: Rights
    var user: User
    var signature: Signature
    var json: JSONObject

    val status = "status"

    init {
        val text = data.toString(Charset.defaultCharset())
        try {

        json = JSONObject(text)

        } catch (e: Exception) {
            throw Exception("Lcp parsing error")
        }

        try {
            id = json.getString("id")
            issued = DateTime(json.getString("issued")).toDate().toString()
            provider = URL(json.getString("provider"))
        } catch (e: Exception) {
            throw Exception("Lcp parsing error")
        }
        encryption = Encryption(JSONObject(json.getString("encryption")))
        links = parseLinks(json["links"] as JSONArray)
        rights = Rights(json.getJSONObject("rights"))
        if (json.has("potential_rights")) {
            rights.potentialEnd = DateTime(json.getJSONObject("potential_rights").getString("end")).toDate().toString()
        }
        user = User(json.getJSONObject("user"))
        signature = Signature(json.getJSONObject("signature"))
        if (json.has("updated")) {
            updated = DateTime(json.getString("updated")).toDate().toString()
        }
        if (link("hint") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.hintLinkNotFound))
        }
        if (link("publication") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.publicationLinkNotFound))
        }
    }

    fun dateOfLastUpdate() = if (updated != null) updated!! else issued

    fun link(rel: String) = links.first{
        it.rel.contains(rel)
    }

    fun getHint() = encryption.userKey.hint

}