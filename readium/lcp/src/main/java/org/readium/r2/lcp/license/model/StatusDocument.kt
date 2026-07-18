/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
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
import org.readium.r2.shared.util.mediatype.MediaType

@Serializable(with = StatusDocumentSerializer::class)
public class StatusDocument(
    public val id: String,
    public val status: Status,
    public val message: String,
    public val licenseUpdated: Instant,
    public val statusUpdated: Instant,
    public val links: Links,
    public val potentialRights: PotentialRights?,
    public val events: List<Event>,
    public val jsonString: String,
) {
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

    internal constructor(other: StatusDocument) : this(
        id = other.id,
        status = other.status,
        message = other.message,
        licenseUpdated = other.licenseUpdated,
        statusUpdated = other.statusUpdated,
        links = other.links,
        potentialRights = other.potentialRights,
        events = other.events,
        jsonString = other.jsonString
    )

    public constructor(data: ByteArray) : this(
        try {
            LcpJson.decodeFromString(StatusDocumentSerializer, data.decodeToString())
        } catch (e: Exception) {
            if (e is LcpException) throw e
            throw LcpException(LcpError.Parsing.MalformedJSON)
        }
    )

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

    @Deprecated("Use jsonString instead")
    public val json: JSONObject get() = JSONObject(jsonString)

    @Deprecated("No longer available")
    public val data: ByteArray get() = jsonString.toByteArray()

    public val description: String
        get() = "Status(${status.value})"
}

internal object StatusDocumentSerializer : KSerializer<StatusDocument> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): StatusDocument {
        val input = decoder as JsonDecoder
        val jsonElement = input.decodeJsonElement() as JsonObject
        val rawJson = jsonElement.toString()

        val id = jsonElement["id"]?.jsonPrimitive?.contentOrNull
            ?: throw LcpException(LcpError.Parsing.StatusDocument)

        val statusStr = jsonElement["status"]?.jsonPrimitive?.contentOrNull
            ?: throw LcpException(LcpError.Parsing.StatusDocument)
        val status = StatusDocument.Status(statusStr)
            ?: throw LcpException(LcpError.Parsing.StatusDocument)

        val message = jsonElement["message"]?.jsonPrimitive?.contentOrNull
            ?: throw LcpException(LcpError.Parsing.StatusDocument)

        val updated = jsonElement["updated"] as? JsonObject
            ?: throw LcpException(LcpError.Parsing.StatusDocument)
        val licenseUpdated = updated["license"]?.jsonPrimitive?.contentOrNull?.toInstant()
            ?: throw LcpException(LcpError.Parsing.StatusDocument)
        val statusUpdated = updated["status"]?.jsonPrimitive?.contentOrNull?.toInstant()
            ?: throw LcpException(LcpError.Parsing.StatusDocument)

        val linksJson = jsonElement["links"] as? JsonArray
            ?: throw LcpException(LcpError.Parsing.StatusDocument)
        val links = LcpJson.decodeFromJsonElement(Links.serializer(), linksJson)

        val potentialRights = jsonElement["potential_rights"]?.let {
            LcpJson.decodeFromJsonElement(PotentialRights.serializer(), it)
        }

        val eventsJson = jsonElement["events"] as? JsonArray
        val events = eventsJson?.map {
            LcpJson.decodeFromJsonElement(Event.serializer(), it)
        } ?: emptyList()

        return StatusDocument(
            id = id,
            status = status,
            message = message,
            licenseUpdated = licenseUpdated,
            statusUpdated = statusUpdated,
            links = links,
            potentialRights = potentialRights,
            events = events,
            jsonString = rawJson
        )
    }

    override fun serialize(encoder: Encoder, value: StatusDocument) {
        val output = encoder as JsonEncoder
        val jsonObject = LcpJson.parseToJsonElement(value.jsonString) as JsonObject
        output.encodeJsonElement(jsonObject)
    }
}
