/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model

import java.nio.charset.Charset
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
import org.readium.r2.lcp.license.model.components.lcp.Encryption
import org.readium.r2.lcp.license.model.components.lcp.Rights
import org.readium.r2.lcp.license.model.components.lcp.Signature
import org.readium.r2.lcp.license.model.components.lcp.User
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

@Serializable(with = LicenseDocumentSerializer::class)
public class LicenseDocument internal constructor(
    public val provider: String,
    public val id: String,
    public val issued: Instant,
    public val updated: Instant,
    public val encryption: Encryption,
    public val links: Links,
    public val user: User,
    public val rights: Rights,
    public val signature: Signature,
    public val jsonString: String,
) {

    public companion object {
        @Deprecated("Use fromBytes instead")
        public fun fromJSON(json: JSONObject): Try<LicenseDocument, LcpError.Parsing> {
            return fromBytes(json.toString().toByteArray())
        }

        public fun fromBytes(data: ByteArray): Try<LicenseDocument, LcpError.Parsing> {
            return try {
                Try.success(LicenseDocument(data))
            } catch (e: Exception) {
                val error = (e as? LcpException)?.error as? LcpError.Parsing
                    ?: LcpError.Parsing.MalformedJSON
                Try.failure(error)
            }
        }
    }

    init {
        if (link(Rel.Hint) == null || link(Rel.Publication) == null) {
            throw LcpException(LcpError.Parsing.LicenseDocument)
        }

        // Check that the acquisition link has a valid URL.
        try {
            link(Rel.Publication)!!.url() as AbsoluteUrl
        } catch (e: Exception) {
            Timber.e(e)
            throw LcpException(LcpError.Parsing.Url(rel = Rel.Publication.value))
        }
    }

    internal constructor(other: LicenseDocument) : this(
        provider = other.provider,
        id = other.id,
        issued = other.issued,
        updated = other.updated,
        encryption = other.encryption,
        links = other.links,
        user = other.user,
        rights = other.rights,
        signature = other.signature,
        jsonString = other.jsonString
    )

    internal constructor(data: ByteArray) : this(
        try {
            LcpJson.decodeFromString(LicenseDocumentSerializer, data.decodeToString())
        } catch (e: Exception) {
            if (e is LcpException) {
                throw e
            }
            throw LcpException(LcpError.Parsing.MalformedJSON)
        }
    )

    public enum class Rel(public val value: String) {
        Hint("hint"),
        Publication("publication"),
        Self("self"),
        Support("support"),
        Status("status"),
        ;

        public companion object {
            @Deprecated("Use fromBytes instead")
            public fun fromJSON(json: JSONObject): Try<LicenseDocument, LcpError.Parsing> {
                return fromBytes(json.toString().toByteArray())
            }

            public operator fun invoke(value: String): Rel? = entries.firstOrNull { it.value == value }
        }
    }

    public val publicationLink: Link
        get() = link(Rel.Publication)!!

    public fun link(rel: Rel, type: MediaType? = null): Link? =
        links.firstWithRel(rel.value, type)

    public fun links(rel: Rel, type: MediaType? = null): List<Link> =
        links.allWithRel(rel.value, type)

    public fun url(
        rel: Rel,
        preferredType: MediaType? = null,
        parameters: URLParameters = emptyMap(),
    ): Url {
        val link = link(rel, preferredType)
            ?: links.firstWithRelAndNoType(rel.value)
            ?: throw LcpException(LcpError.Parsing.Url(rel = rel.value))

        return link.url(parameters = parameters)
    }

    @Deprecated("Use jsonString instead", ReplaceWith("this.jsonString"))
    public val json: JSONObject get() = JSONObject(jsonString)

    public val description: String
        get() = "License($id)"

    public fun toByteArray(): ByteArray =
        jsonString.toByteArray(Charset.defaultCharset())
}

internal object LicenseDocumentSerializer : KSerializer<LicenseDocument> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): LicenseDocument {
        val input = decoder as JsonDecoder
        val jsonElement = input.decodeJsonElement() as JsonObject
        val rawJson = jsonElement.toString()

        val provider = jsonElement["provider"]?.jsonPrimitive?.contentOrNull
            ?: throw LcpException(LcpError.Parsing.LicenseDocument)
        val id = jsonElement["id"]?.jsonPrimitive?.contentOrNull
            ?: throw LcpException(LcpError.Parsing.LicenseDocument)
        val issued = jsonElement["issued"]?.jsonPrimitive?.contentOrNull?.toInstant()
            ?: throw LcpException(LcpError.Parsing.LicenseDocument)
        val updated = jsonElement["updated"]?.jsonPrimitive?.contentOrNull?.toInstant() ?: issued

        val encryptionJson = jsonElement["encryption"] as? JsonObject
            ?: throw LcpException(LcpError.Parsing.LicenseDocument)
        val encryption = LcpJson.decodeFromJsonElement(Encryption.serializer(), encryptionJson)

        val linksJson = jsonElement["links"] as? JsonArray
            ?: throw LcpException(LcpError.Parsing.LicenseDocument)
        val links = LcpJson.decodeFromJsonElement(Links.serializer(), linksJson)

        val user = jsonElement["user"]?.let {
            LcpJson.decodeFromJsonElement(User.serializer(), it)
        } ?: User()

        val rights = jsonElement["rights"]?.let {
            LcpJson.decodeFromJsonElement(Rights.serializer(), it)
        } ?: Rights()

        val signatureJson = jsonElement["signature"] as? JsonObject
            ?: throw LcpException(LcpError.Parsing.LicenseDocument)
        val signature = LcpJson.decodeFromJsonElement(Signature.serializer(), signatureJson)

        return LicenseDocument(
            provider = provider,
            id = id,
            issued = issued,
            updated = updated,
            encryption = encryption,
            links = links,
            user = user,
            rights = rights,
            signature = signature,
            jsonString = rawJson
        )
    }

    override fun serialize(encoder: Encoder, value: LicenseDocument) {
        val output = encoder as JsonEncoder
        val jsonObject = LcpJson.parseToJsonElement(value.jsonString) as JsonObject
        output.encodeJsonElement(jsonObject)
    }
}
