/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model

import org.joda.time.DateTime
import org.json.JSONObject
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.Links
import org.readium.r2.lcp.license.model.components.lsd.Event
import org.readium.r2.lcp.license.model.components.lsd.PotentialRights
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.util.mediatype.MediaType
import java.net.URL
import java.nio.charset.Charset

class StatusDocument(val data: ByteArray) {
    val id: String
    val status: Status
    val message: String
    val licenseUpdated: DateTime
    val statusUpdated: DateTime
    val links: Links
    val potentialRights: PotentialRights?
    var events: MutableList<Event> = mutableListOf()

    val json: JSONObject

    enum class Status(val rawValue: String) {
        ready("ready"),
        active("active"),
        revoked("revoked"),
        returned("returned"),
        cancelled("cancelled"),
        expired("expired");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    enum class Rel(val rawValue: String) {
        register("register"),
        license("license"),
        `return`("return"),
        renew("renew");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    init {

        try {
            json = JSONObject(data.toString(Charset.defaultCharset()))
        } catch (e: Exception) {
            throw LcpException.Parsing.MalformedJSON
        }

        id = if (json.has("id")) json.getString("id") else throw LcpException.Parsing.StatusDocument
        status = if (json.has("status")) Status(json.getString("status"))!! else throw LcpException.Parsing.StatusDocument
        message = if (json.has("message")) json.getString("message") else throw LcpException.Parsing.StatusDocument

        val updated = if (json.has("updated")) json.getJSONObject("updated") else JSONObject()

        licenseUpdated = if (updated.has("license")) DateTime(updated.getString("license")) else throw LcpException.Parsing.StatusDocument
        statusUpdated = if (updated.has("status")) DateTime(updated.getString("status")) else throw LcpException.Parsing.StatusDocument


        links = if (json.has("links")) Links(json.getJSONArray("links")) else throw LcpException.Parsing.StatusDocument

        potentialRights = if (json.has("potential_rights")) PotentialRights(json.getJSONObject("potential_rights")) else null

        if (json.has("events")) {
            val ev = json.getJSONArray("events")
            for (i in 0 until ev.length()) {
                events.add(Event(ev.getJSONObject(i)))
            }
        }

    }

    fun link(rel: Rel, type: MediaType? = null): Link? =
        links.firstWithRel(rel.rawValue, type)

    fun links(rel: Rel, type: MediaType? = null): List<Link> =
        links.allWithRel(rel.rawValue, type)

    fun url(rel: Rel, preferredType: MediaType? = null, parameters:  URLParameters = emptyMap()): URL {
        val link = link(rel, preferredType)
            ?: links.firstWithRelAndNoType(rel.rawValue)
            ?: throw LcpException.Parsing.Url(rel = rel.rawValue)

        return link.url(parameters)
    }

    fun events(type: Event.EventType): List<Event> =
            events(type.rawValue)

    fun events(type: String): List<Event> =
            events.filter { it.type == type }

    val description: String
        get() = "Status(${status.rawValue})"

}

