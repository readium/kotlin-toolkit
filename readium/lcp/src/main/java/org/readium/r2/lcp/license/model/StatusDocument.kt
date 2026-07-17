/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model

import java.nio.charset.Charset
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.Links
import org.readium.r2.lcp.license.model.components.lsd.Event
import org.readium.r2.lcp.license.model.components.lsd.PotentialRights
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.json.optJsonArray
import org.readium.r2.shared.util.json.optJsonObject
import org.readium.r2.shared.util.json.optNullableString
import org.readium.r2.shared.util.json.toJsonObjectOrNull
import org.readium.r2.shared.util.mediatype.MediaType

public class StatusDocument(public val data: ByteArray) {
    public val id: String
    public val status: Status
    public val message: String
    public val licenseUpdated: Instant
    public val statusUpdated: Instant
    public val links: Links
    public val potentialRights: PotentialRights?
    public val events: List<Event>

    public val json: JsonObject

    public enum class Status(public val value: String) {
        Ready("ready"),
        Active("active"),
        Revoked("revoked"),
        Returned("returned"),
        Cancelled("cancelled"),
        Expired("expired"),
        ;

        public companion object {
            public operator fun invoke(value: String): Status? = entries.firstOrNull { it.value == value }
        }
    }

    public enum class Rel(public val value: String) {
        Register("register"),
        License("license"),
        Return("return"),
        Renew("renew"),
        ;

        public companion object {
            public operator fun invoke(value: String): Rel? = entries.firstOrNull { it.value == value }
        }
    }

    init {
        json = data.toString(Charset.defaultCharset()).toJsonObjectOrNull()
            ?: throw LcpException(LcpError.Parsing.MalformedJSON)

        id = json.optNullableString("id") ?: throw LcpException(LcpError.Parsing.StatusDocument)
        status = json.optNullableString("status")?.let { Status(it) } ?: throw LcpException(
            LcpError.Parsing.StatusDocument
        )
        message = json.optNullableString("message") ?: throw LcpException(
            LcpError.Parsing.StatusDocument
        )

        val updated = json.optJsonObject("updated") ?: JsonObject(emptyMap())
        licenseUpdated = updated.optNullableString("license")?.toInstant() ?: throw LcpException(
            LcpError.Parsing.StatusDocument
        )
        statusUpdated = updated.optNullableString("status")?.toInstant() ?: throw LcpException(
            LcpError.Parsing.StatusDocument
        )

        links = json.optJsonArray("links")?.let { Links(it) } ?: throw LcpException(
            LcpError.Parsing.StatusDocument
        )

        potentialRights = json.optJsonObject("potential_rights")?.let { PotentialRights(it) }

        events = json.optJsonArray("events")
            ?.mapNotNull { ev ->
                (ev as? JsonObject)?.let { Event(it) }
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
        parameters: URLParameters = emptyMap(),
    ): Url {
        val link = link(rel, preferredType)
            ?: linkWithNoType(rel)
            ?: throw LcpException(LcpError.Parsing.Url(rel = rel.value))

        return link.url(parameters = parameters)
    }

    public fun events(type: Event.EventType): List<Event> =
        events(type.value)

    public fun events(type: String): List<Event> =
        events.filter { it.type == type }

    public val description: String
        get() = "Status(${status.value})"
}
