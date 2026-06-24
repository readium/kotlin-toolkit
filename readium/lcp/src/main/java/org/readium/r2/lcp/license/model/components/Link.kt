/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model.components

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

@Serializable(with = LinkSerializer::class)
public data class Link(
    val href: Href,
    val mediaType: MediaType? = null,
    val title: String? = null,
    val rels: Set<String> = setOf(),
    val profile: String? = null,
    val length: Int? = null,
    val hash: String? = null,
) {
    /**
     * Returns the URL represented by this link's HREF.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public fun url(
        parameters: Map<String, String> = emptyMap(),
    ): Url = href.resolve(parameters = parameters)
}

public object LinkSerializer : KSerializer<Link> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): Link {
        val input = decoder as JsonDecoder
        val jsonObject = input.decodeJsonElement() as JsonObject

        val hrefStr = jsonObject["href"]?.jsonPrimitive?.contentOrNull
            ?: throw LcpException(LcpError.Parsing.Link)

        val templated = jsonObject["templated"]?.jsonPrimitive?.booleanOrNull ?: false
        val href = Href(hrefStr, templated) ?: throw LcpException(LcpError.Parsing.Link)

        val mediaType = jsonObject["type"]?.jsonPrimitive?.contentOrNull?.let { MediaType(it) }
        val title = jsonObject["title"]?.jsonPrimitive?.contentOrNull

        val relElement = jsonObject["rel"]
        val rels = when (relElement) {
            is JsonPrimitive -> setOf(relElement.content)
            is JsonArray -> relElement.map { it.jsonPrimitive.content }.toSet()
            else -> emptySet()
        }.takeIf { it.isNotEmpty() } ?: throw LcpException(LcpError.Parsing.Link)

        val profile = jsonObject["profile"]?.jsonPrimitive?.contentOrNull
        val length = jsonObject["length"]?.jsonPrimitive?.intOrNull
        val hash = jsonObject["hash"]?.jsonPrimitive?.contentOrNull

        return Link(
            href = href,
            mediaType = mediaType,
            title = title,
            rels = rels,
            profile = profile,
            length = length,
            hash = hash
        )
    }

    override fun serialize(encoder: Encoder, value: Link) {
        val output = encoder as JsonEncoder
        val map = mutableMapOf<String, JsonElement>()
        map["href"] = JsonPrimitive(value.href.toString())
        if (value.href.isTemplated) {
            map["templated"] = JsonPrimitive(true)
        }
        value.mediaType?.let { map["type"] = JsonPrimitive(it.toString()) }
        value.title?.let { map["title"] = JsonPrimitive(it) }

        if (value.rels.size == 1) {
            map["rel"] = JsonPrimitive(value.rels.first())
        } else if (value.rels.isNotEmpty()) {
            map["rel"] = JsonArray(value.rels.map { JsonPrimitive(it) })
        }

        value.profile?.let { map["profile"] = JsonPrimitive(it) }
        value.length?.let { map["length"] = JsonPrimitive(it) }
        value.hash?.let { map["hash"] = JsonPrimitive(it) }

        output.encodeJsonElement(JsonObject(map))
    }
}
