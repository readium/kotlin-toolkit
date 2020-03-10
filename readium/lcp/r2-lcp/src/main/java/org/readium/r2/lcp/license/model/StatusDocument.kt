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
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.Links
import org.readium.r2.lcp.license.model.components.lsd.Event
import org.readium.r2.lcp.license.model.components.lsd.PotentialRights
import org.readium.r2.lcp.ParsingError
import java.net.URL
import java.nio.charset.Charset

data class StatusDocument(val data: ByteArray) {

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
            throw ParsingError.malformedJSON
        }

        id = if (json.has("id")) json.getString("id") else throw ParsingError.statusDocument
        status = if (json.has("status")) Status(json.getString("status"))!! else throw ParsingError.statusDocument
        message = if (json.has("message")) json.getString("message") else throw ParsingError.statusDocument

        val updated = if (json.has("updated")) json.getJSONObject("updated") else JSONObject()

        licenseUpdated = if (updated.has("license")) DateTime(updated.getString("license")) else throw ParsingError.statusDocument
        statusUpdated = if (updated.has("status")) DateTime(updated.getString("status")) else throw ParsingError.statusDocument


        links = if (json.has("links")) Links(json.getJSONArray("links")) else throw ParsingError.statusDocument

        potentialRights = if (json.has("potential_rights")) PotentialRights(json.getJSONObject("potential_rights")) else null

        if (json.has("events")) {
            val ev = json.getJSONArray("events")
            for (i in 0 until ev.length()) {
                events.add(Event(ev.getJSONObject(i)))
            }
        }

    }

    fun link(rel: Rel): Link? =
            links[rel.rawValue].firstOrNull()

    fun links(rel: Rel): List<Link> =
            links[rel.rawValue]

    fun url(rel: Rel, parameters:  List<Pair<String, Any?>> = listOf()): URL {
        return link(rel)?.url(parameters) ?: throw ParsingError.url(rel = rel.rawValue)
    }

    fun events(type: Event.EventType): List<Event> =
            events(type.rawValue)

    fun events(type: String): List<Event> =
            events.filter { it.type == type }

    val description: String
        get() = "Status(${status.rawValue})"

}

