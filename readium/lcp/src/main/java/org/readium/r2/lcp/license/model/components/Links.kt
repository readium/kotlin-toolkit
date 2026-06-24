/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model.components

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.mediatype.MediaType

@Serializable(with = LinksSerializer::class)
public data class Links(val links: List<Link>) {

    public fun firstWithRel(rel: String, type: MediaType? = null): Link? =
        links.firstOrNull { it.matches(rel, type) }

    internal fun firstWithRelAndNoType(rel: String): Link? =
        links.firstOrNull { it.rels.contains(rel) && it.mediaType == null }

    public fun allWithRel(rel: String, type: MediaType? = null): List<Link> =
        links.filter { it.matches(rel, type) }

    private fun Link.matches(rel: String, mediaType: MediaType?): Boolean =
        this.rels.contains(rel) && (mediaType?.matches(this.mediaType) ?: true)

    public operator fun get(rel: String): List<Link> = allWithRel(rel)
}

public object LinksSerializer : KSerializer<Links> {
    private val delegateSerializer = ListSerializer(Link.serializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): Links {
        return Links(decoder.decodeSerializableValue(delegateSerializer))
    }

    override fun serialize(encoder: Encoder, value: Links) {
        encoder.encodeSerializableValue(delegateSerializer, value.links)
    }
}
