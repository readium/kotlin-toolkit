/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.readium.r2.lcp.license.model.components

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

@Serializable(with = LinkSerializer::class)
@KeepGeneratedSerializer
public data class Link(
    val href: Href,
    @SerialName("type")
    val mediaType: MediaType? = null,
    val title: String? = null,
    @SerialName("rel")
    @Serializable(with = RelSerializer::class)
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

public fun List<Link>.firstWithRel(rel: String, type: MediaType? = null): Link? =
    firstOrNull { it.matches(rel, type) }

internal fun List<Link>.firstWithRelAndNoType(rel: String): Link? =
    firstOrNull { it.rels.contains(rel) && it.mediaType == null }

public fun List<Link>.allWithRel(rel: String, type: MediaType? = null): List<Link> =
    filter { it.matches(rel, type) }

private fun Link.matches(rel: String, mediaType: MediaType?): Boolean =
    this.rels.contains(rel) && (mediaType?.matches(this.mediaType) ?: true)

public object LinkSerializer :
    JsonTransformingSerializer<Link>(Link.generatedSerializer()) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element

        val hrefValue = element["href"] ?: JsonNull
        val templatedValue = element["templated"] ?: JsonPrimitive(false)

        return JsonObject(
            buildMap {
                putAll(element)
                remove("templated")
                put(
                    "href",
                    JsonObject(
                        mapOf(
                            "url" to hrefValue,
                            "templated" to templatedValue
                        )
                    )
                )
            }
        )
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        val hrefObj = element["href"] as? JsonObject ?: return element

        val url = hrefObj["url"] ?: JsonNull
        val templated = hrefObj["templated"]?.jsonPrimitive?.booleanOrNull ?: false

        return JsonObject(
            buildMap {
                putAll(element)
                put("href", url)
                if (templated) {
                    put("templated", JsonPrimitive(true))
                }
            }
        )
    }
}

public object RelSerializer :
    JsonTransformingSerializer<Set<String>>(SetSerializer(String.serializer())) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive) {
            return transformDeserialize(JsonArray(listOf(element)))
        }

        return super.transformDeserialize(element)
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        (element as? JsonArray)?.let { rels ->
            val modifiedRels = when (rels.size) {
                0 -> JsonNull
                1 -> rels.first()
                else -> rels
            }
            return super.transformSerialize(modifiedRels)
        }

        return super.transformSerialize(element)
    }
}
