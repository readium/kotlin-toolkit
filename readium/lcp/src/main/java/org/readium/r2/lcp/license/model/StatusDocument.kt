/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model

import java.net.URL
import java.nio.charset.Charset
import java.util.*
import org.json.JSONObject
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.Links
import org.readium.r2.lcp.license.model.components.lsd.Event
import org.readium.r2.lcp.license.model.components.lsd.PotentialRights
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.util.mediatype.MediaType

class StatusDocument(val data: ByteArray) {
    val id: String
    val status: Status
    val message: String
    val licenseUpdated: Date
    val statusUpdated: Date
    val links: Links
    val potentialRights: PotentialRights?
    val events: List<Event>

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

        id = json.optNullableString("id") ?: throw LcpException.Parsing.StatusDocument
        status = json.optNullableString("status")?.let { Status(it) } ?: throw LcpException.Parsing.StatusDocument
        message = json.optNullableString("message") ?: throw LcpException.Parsing.StatusDocument

        val updated = json.optJSONObject("updated") ?: JSONObject()
        licenseUpdated = updated.optNullableString("license")?.iso8601ToDate() ?: throw LcpException.Parsing.StatusDocument
        statusUpdated = updated.optNullableString("status")?.iso8601ToDate() ?: throw LcpException.Parsing.StatusDocument

        links = json.optJSONArray("links")?.let { Links(it) } ?: throw LcpException.Parsing.StatusDocument

        potentialRights = json.optJSONObject("potential_rights")?.let { PotentialRights(it) }

        events = json.optJSONArray("events")
            ?.mapNotNull { ev ->
                (ev as? JSONObject)?.let { Event(it) }
            }
            ?: emptyList()
    }

    fun link(rel: Rel, type: MediaType? = null): Link? =
        links.firstWithRel(rel.rawValue, type)

    fun links(rel: Rel, type: MediaType? = null): List<Link> =
        links.allWithRel(rel.rawValue, type)

    internal fun linkWithNoType(rel: Rel): Link? =
        links.firstWithRelAndNoType(rel.rawValue)

    fun url(rel: Rel, preferredType: MediaType? = null, parameters: URLParameters = emptyMap()): URL {
        val link = link(rel, preferredType)
            ?: linkWithNoType(rel)
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
