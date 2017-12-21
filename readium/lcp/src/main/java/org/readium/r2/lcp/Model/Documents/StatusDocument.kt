package org.readium.r2.lcp.Model.Documents

import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import org.readium.r2.lcp.Model.SubParts.Link
import org.readium.r2.lcp.Model.SubParts.Updated
import org.readium.r2.lcp.Model.SubParts.lsd.Event
import org.readium.r2.lcp.Model.SubParts.lsd.PotentialRights

class StatusDocument(data: ByteArray) {

    var id: String
    var status: String
    var message: String
    var links: List<Link>
    var updated: Updated?
    var potentialRights: PotentialRights?
    var events: List<Event?>

    init {
        try {
            val json = JSONObject(data.toString())
            id = json.getString("id")
            status = json.getString("status")
            message = json.getString("message")
            updated = Updated(json.getJSONObject("updated"))
            links = Link(json).parseLinks("links")
            events = Event(json).parseEvents("events")
            potentialRights = PotentialRights(json.getJSONObject("potential_rights"))
        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }

    fun dateOfLatestLicenseDocumentUpdate() = updated?.license

    fun link(rel: String) : Link {
        return links.first { it.rel.contains(rel) }
    }
}