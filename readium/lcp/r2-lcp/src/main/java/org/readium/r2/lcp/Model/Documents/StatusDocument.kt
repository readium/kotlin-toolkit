/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

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

/// Document that contains information about the history of a License Document,
/// along with its current status and available interactions.
class StatusDocument {

    var id: String
    var status: Status
    /// A message meant to be displayed to the User regarding the current status
    /// of the license.
    var message: String
    /// Must contain at least a link to the LicenseDocument associated to this.
    /// Status Document.
    var links: List<Link>
    var updated: Updated?
    /// Dictionnary of potential rights associated with Dates.
    var potentialRights: PotentialRights? = null
    /// Ordered list of events related to the change in status of a License
    /// Document.
    var events: List<Event?>

    /// Describes the status of the license.
    ///
    /// - ready: The License Document is available, but the user hasn't accessed
    ///          the License and/or Status Document yet.
    /// - active: The license is active, and a device has been successfully
    ///           registered for this license. This is the default value if the
    ///           License Document does not contain a registration link, or a
    ///           registration mechanism through the license itself.
    /// - revoked: The license is no longer active, it has been invalidated by
    ///            the Issuer.
    /// - returned: The license is no longer active, it has been invalidated by
    ///             the User.
    /// - cancelled: The license is no longer active because it was cancelled
    ///              prior to activation.
    /// - expired: The license is no longer active because it has expired.
    enum class Status(val v:String) {
        ready("ready"),
        active("active"),
        revoked("revoked"),
        returned("returned"),
        cancelled("cancelled"),
        expired("expired")
    }

    enum class Rel(val v:String) {
        register("register"),
        license("license"),
        ret("return"),
        renew("renew")
    }

    constructor(data: ByteArray) {
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

    constructor(json: JSONObject) {
        try {

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

    /// Returns the first link containing the given rel.
    ///
    /// - Parameter rel: The rel to look for.
    /// - Returns: The first link containing the rel.
    fun link(rel: String) : Link? {
        return links.firstOrNull { it.rel.contains(rel) }
    }
}