package org.readium.r2.lcp.Model.Documents

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import org.readium.r2.lcp.Model.SubParts.Link
import org.readium.r2.lcp.Model.SubParts.Updated
import org.readium.r2.lcp.Model.SubParts.lsd.Event
import org.readium.r2.lcp.Model.SubParts.lsd.PotentialRights
import org.readium.r2.lcp.Model.SubParts.lsd.parseEvents
import org.readium.r2.lcp.Model.SubParts.parseLinks
import java.nio.charset.Charset

class StatusDocument(data: ByteArray) {

    enum class Status(val v:String) {
        ready("ready"),
        active("active"),
        revoked("revoked"),
        returned("returned"),
        cancelled("cancelled"),
        expired("expired")
    }

    var id: String
    var status: Status
    var message: String
    var links: List<Link>
    var updated: Updated?
    var potentialRights: PotentialRights? = null
    var events: List<Event?>

    init {
        try {
            val text = data.toString(Charset.defaultCharset())

            val json = JSONObject(text)
            id = json.getString("id")
            status =  Status.valueOf(json.getString("status"))
            message = json.getString("message")
            updated = Updated(json.getJSONObject("updated"))
            links = parseLinks(json["links"] as JSONArray)
            events = parseEvents(json["events"] as JSONArray)
            if (json.has("potential_rights")) {
                potentialRights = PotentialRights(json.getJSONObject("potential_rights"))
            }
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }

    fun dateOfLatestLicenseDocumentUpdate() = updated?.license

    fun link(rel: String) : Link? {
        return links.firstOrNull { it.rel.contains(rel) }
    }
}