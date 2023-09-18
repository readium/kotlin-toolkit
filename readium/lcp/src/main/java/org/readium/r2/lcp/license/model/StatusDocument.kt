/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model

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
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

public class StatusDocument(public val data: ByteArray) {
    public val id: String
    public val status: Status
    public val message: String
    public val licenseUpdated: Date
    public val statusUpdated: Date
    public val links: Links
    public val potentialRights: PotentialRights?
    public val events: List<Event>

    public val json: JSONObject

    public enum class Status(public val value: String) {
        Ready("ready"),
        Active("active"),
        Revoked("revoked"),
        Returned("returned"),
        Cancelled("cancelled"),
        Expired("expired");

        @Deprecated("Use [value] instead", ReplaceWith("value"), level = DeprecationLevel.ERROR)
        public val rawValue: String get() = value

        public companion object {
            public operator fun invoke(value: String): Status? = values().firstOrNull { it.value == value }
        }
    }

    public enum class Rel(public val value: String) {
        Register("register"),
        License("license"),
        Return("return"),
        Renew("renew");

        @Deprecated("Use [value] instead", ReplaceWith("value"), level = DeprecationLevel.ERROR)
        public val rawValue: String get() = value

        public companion object {
            public operator fun invoke(value: String): Rel? = values().firstOrNull { it.value == value }
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

    public fun link(rel: Rel, type: MediaType? = null): Link? =
        links.firstWithRel(rel.value, type)

    public fun links(rel: Rel, type: MediaType? = null): List<Link> =
        links.allWithRel(rel.value, type)

    internal fun linkWithNoType(rel: Rel): Link? =
        links.firstWithRelAndNoType(rel.value)

    public fun url(
        rel: Rel,
        preferredType: MediaType? = null,
        parameters: URLParameters = emptyMap()
    ): Url {
        val link = link(rel, preferredType)
            ?: linkWithNoType(rel)
            ?: throw LcpException.Parsing.Url(rel = rel.value)

        return link.url(parameters = parameters)
    }

    public fun events(type: Event.EventType): List<Event> =
        events(type.value)

    public fun events(type: String): List<Event> =
        events.filter { it.type == type }

    public val description: String
        get() = "Status(${status.value})"
}
