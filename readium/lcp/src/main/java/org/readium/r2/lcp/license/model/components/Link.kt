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

import org.json.JSONObject
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.extensions.optNullableInt
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

public data class Link(
    val href: Href,
    val mediaType: MediaType? = null,
    val title: String? = null,
    val rels: Set<String> = setOf(),
    val profile: String? = null,
    val length: Int? = null,
    val hash: String? = null
) {

    public companion object {
        public operator fun invoke(
            json: JSONObject,
            mediaTypeRetriever: MediaTypeRetriever = MediaTypeRetriever()
        ): Link {
            val href = json.optNullableString("href")
                ?.let {
                    Href(
                        href = it,
                        templated = json.optBoolean("templated", false)
                    )
                }
                ?: throw LcpException.Parsing.Link

            return Link(
                href = href,
                mediaType = json.optNullableString("type")
                    ?.let { mediaTypeRetriever.retrieve(it) },
                title = json.optNullableString("title"),
                rels = json.optStringsFromArrayOrSingle("rel").toSet()
                    .takeIf { it.isNotEmpty() }
                    ?: throw LcpException.Parsing.Link,
                profile = json.optNullableString("profile"),
                length = json.optNullableInt("length"),
                hash = json.optNullableString("hash")
            )
        }
    }

    /**
     * Returns the URL represented by this link's HREF.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public fun url(
        parameters: Map<String, String> = emptyMap()
    ): Url = href.resolve(parameters = parameters)

    @Deprecated(
        "Use [mediaType.toString()] instead",
        ReplaceWith("mediaType.toString()"),
        level = DeprecationLevel.ERROR
    )
    public val type: String? get() = throw NotImplementedError()

    @Deprecated(
        "Renamed `rels`",
        ReplaceWith("rels"),
        level = DeprecationLevel.ERROR
    )
    public val rel: List<String> get() = throw NotImplementedError()
}
