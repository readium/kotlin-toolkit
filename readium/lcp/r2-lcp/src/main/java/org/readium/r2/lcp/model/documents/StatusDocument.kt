/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.documents

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import org.readium.r2.lcp.model.sub.Link
import org.readium.r2.lcp.model.sub.Updated
import org.readium.r2.lcp.model.sub.lsd.Event
import org.readium.r2.lcp.model.sub.lsd.PotentialRights
import org.readium.r2.lcp.model.sub.lsd.parseEvents
import org.readium.r2.lcp.model.sub.parseLinks
import java.nio.charset.Charset

/// Document that contains information about the history of a License Document,
/// along with its current status and available interactions.
class StatusDocument {

    var id: String?  = null
    var status: Status?  = null
    /// A message meant to be displayed to the User regarding the current status
    /// of the license.
    var message: String? = null
    /// Must contain at least a link to the LicenseDocument associated to this.
    /// Status Document.
    var links: List<Link>?  = null
    private var updated: Updated? = null
    /// Dictionary of potential rights associated with Dates.
    private var potentialRights: PotentialRights? = null
    /// Ordered list of events related to the change in status of a License
    /// Document.
    private var events: List<Event?>? = null

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

            if (json.has("id")) id = json.getString("id")
            if (json.has("status")) status = Status.valueOf(json.getString("status"))
            if (json.has("message")) message = json.getString("message")
            if (json.has("updated")) updated = Updated(json.getJSONObject("updated"))
            if (json.has("links")) links = parseLinks(json["links"] as JSONArray)
            if (json.has("events")) events = parseEvents(json["events"] as JSONArray)
            if (json.has("potential_rights")) potentialRights = PotentialRights(json.getJSONObject("potential_rights"))

        } catch (e: Exception){
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }

    constructor(json: JSONObject) {
        try {

            if (json.has("id")) id = json.getString("id")
            if (json.has("status")) status = Status.valueOf(json.getString("status"))
            if (json.has("message")) message = json.getString("message")
            if (json.has("updated")) updated = Updated(json.getJSONObject("updated"))
            if (json.has("links")) links = parseLinks(json["links"] as JSONArray)
            if (json.has("events")) events = parseEvents(json["events"] as JSONArray)
            if (json.has("potential_rights")) potentialRights = PotentialRights(json.getJSONObject("potential_rights"))

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
        return links?.firstOrNull { it.rel.contains(rel) }
    }
}