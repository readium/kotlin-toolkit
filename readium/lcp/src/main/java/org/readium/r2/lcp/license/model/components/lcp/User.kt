/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.model.components.lcp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.readium.r2.lcp.license.model.LcpJson

@Serializable(with = UserSerializer::class)
public data class User(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val encrypted: List<String> = emptyList(),
    val extensions: JsonObject = JsonObject(emptyMap()),
) {
    @Deprecated("Use kotlinx.serialization to serialize the object")
    val json: JSONObject get() = JSONObject(LcpJson.encodeToString(this))
}

internal object UserSerializer : KSerializer<User> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): User {
        val input = decoder as JsonDecoder
        val jsonObject = input.decodeJsonElement() as JsonObject
        val id = jsonObject["id"]?.jsonPrimitive?.contentOrNull
        val email = jsonObject["email"]?.jsonPrimitive?.contentOrNull
        val name = jsonObject["name"]?.jsonPrimitive?.contentOrNull
        val encrypted =
            jsonObject["encrypted"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

        val extensionsMap =
            jsonObject.filterKeys { it !in setOf("id", "email", "name", "encrypted") }

        return User(
            id = id,
            email = email,
            name = name,
            encrypted = encrypted,
            extensions = JsonObject(extensionsMap)
        )
    }

    override fun serialize(encoder: Encoder, value: User) {
        val output = encoder as JsonEncoder
        val map = mutableMapOf<String, JsonElement>()
        value.id?.let { map["id"] = JsonPrimitive(it) }
        value.email?.let { map["email"] = JsonPrimitive(it) }
        value.name?.let { map["name"] = JsonPrimitive(it) }
        if (value.encrypted.isNotEmpty()) {
            map["encrypted"] = JsonArray(value.encrypted.map { JsonPrimitive(it) })
        }
        map.putAll(value.extensions)
        output.encodeJsonElement(JsonObject(map))
    }
}
