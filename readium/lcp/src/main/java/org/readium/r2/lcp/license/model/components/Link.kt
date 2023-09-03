// TODO templated
/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components

import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.URITemplate
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

public data class Link(val json: JSONObject) {
    private val rawHref: String
    var rel: MutableList<String> = mutableListOf()
    val title: String?
    val type: String?
    val templated: Boolean
    val profile: String?
    val length: Int?
    val hash: String?

    init {

        rawHref = if (json.has("href")) json.getString("href") else throw LcpException.Parsing.Link

        if (json.has("rel")) {
            val rel = json["rel"]
            if (rel is String) {
                this.rel.add(rel)
            } else if (rel is JSONArray) {
                for (i in 0 until rel.length()) {
                    this.rel.add(rel[i].toString())
                }
            }
        }

        if (rel.isEmpty()) {
            throw LcpException.Parsing.Link
        }

        title = if (json.has("title")) json.getString("title") else null
        type = if (json.has("type")) json.getString("type") else null
        templated = if (json.has("templated")) json.getBoolean("templated") else false
        profile = if (json.has("profile")) json.getString("profile") else null
        length = if (json.has("length")) json.getInt("length") else null
        hash = if (json.has("hash")) json.getString("hash") else null
    }

    public fun href(parameters: URLParameters = emptyMap()): Url? {
        if (!templated) {
            return Url(rawHref)
        }

        val expandedHref = URITemplate(rawHref).expand(parameters.mapValues { it.value ?: "" })
        return Url(expandedHref)
    }

    val mediaType: MediaType
        get() = type?.let { MediaType(it) } ?: MediaType.BINARY

    /**
     * List of URI template parameter keys, if the [Link] is templated.
     */
    internal val templateParameters: List<String> by lazy {
        if (!templated) {
            emptyList()
        } else {
            URITemplate(rawHref).parameters
        }
    }

    @Deprecated("Use `href()` instead", ReplaceWith("href()"), level = DeprecationLevel.ERROR)
    public val url: URL =
        throw NotImplementedError()

    @Deprecated("Renamed `href`", ReplaceWith("href"), level = DeprecationLevel.ERROR)
    public fun url(parameters: URLParameters): URL =
        throw NotImplementedError()
}
