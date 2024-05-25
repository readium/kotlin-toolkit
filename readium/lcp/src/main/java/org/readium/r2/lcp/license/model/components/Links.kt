/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.license.model.components

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.mediatype.MediaType

public data class Links(val json: JSONArray) {

    val links: List<Link> = json
        .mapNotNull { item ->
            (item as? JSONObject)?.let { obj ->
                tryOrNull { Link(obj) }
            }
        }

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
