/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model.components.lcp

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toInstant

@Serializable(with = RightsSerializer::class)
public data class Rights(
    val print: Int? = null,
    val copy: Int? = null,
    val start: Instant? = null,
    val end: Instant? = null,
    val extensions: JsonObject = JsonObject(emptyMap()),
) {
    @Deprecated("Use kotlinx.serialization to serialize the object")
    val json: JSONObject get() = JSONObject()
}

internal object RightsSerializer : KSerializer<Rights> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): Rights {
        val input = decoder as JsonDecoder
        val jsonObject = input.decodeJsonElement() as JsonObject
        val print = jsonObject["print"]?.jsonPrimitive?.intOrNull
        val copy = jsonObject["copy"]?.jsonPrimitive?.intOrNull
        val start = jsonObject["start"]?.jsonPrimitive?.contentOrNull?.toInstant()
        val end = jsonObject["end"]?.jsonPrimitive?.contentOrNull?.toInstant()

        val extensionsMap = jsonObject.filterKeys { it !in setOf("print", "copy", "start", "end") }

        return Rights(
            print = print,
            copy = copy,
            start = start,
            end = end,
            extensions = JsonObject(extensionsMap)
        )
    }

    override fun serialize(encoder: Encoder, value: Rights) {
        val output = encoder as JsonEncoder
        val map = mutableMapOf<String, JsonElement>()
        value.print?.let { map["print"] = JsonPrimitive(it) }
        value.copy?.let { map["copy"] = JsonPrimitive(it) }
        value.start?.let { map["start"] = JsonPrimitive(it.toString()) }
        value.end?.let { map["end"] = JsonPrimitive(it.toString()) }
        map.putAll(value.extensions)
        output.encodeJsonElement(JsonObject(map))
    }
}
