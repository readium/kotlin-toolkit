/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model

import java.nio.charset.Charset
import java.util.*
import org.json.JSONObject
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.Links
import org.readium.r2.lcp.license.model.components.lcp.Encryption
import org.readium.r2.lcp.license.model.components.lcp.Rights
import org.readium.r2.lcp.license.model.components.lcp.Signature
import org.readium.r2.lcp.license.model.components.lcp.User
import org.readium.r2.lcp.service.URLParameters
import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

public class LicenseDocument internal constructor(public val json: JSONObject) {

    public val provider: String =
        json.optNullableString("provider")
            ?: throw LcpException.Parsing.LicenseDocument

    public val id: String =
        json.optNullableString("id")
            ?: throw LcpException.Parsing.LicenseDocument

    public val issued: Date =
        json.optNullableString("issued")
            ?.iso8601ToDate()
            ?: throw LcpException.Parsing.LicenseDocument

    public val updated: Date =
        json.optNullableString("updated")
            ?.iso8601ToDate()
            ?: issued

    public val encryption: Encryption =
        json.optJSONObject("encryption")
            ?.let { Encryption(it) }
            ?: throw LcpException.Parsing.LicenseDocument

    public val links: Links =
        json.optJSONArray("links")
            ?.let { Links(it) }
            ?: throw LcpException.Parsing.LicenseDocument

    public val user: User =
        User(json.optJSONObject("user") ?: JSONObject())

    public val rights: Rights =
        Rights(json.optJSONObject("rights") ?: JSONObject())

    public val signature: Signature =
        json.optJSONObject("signature")
            ?.let { Signature(it) }
            ?: throw LcpException.Parsing.LicenseDocument

    init {
        if (link(Rel.Hint) == null || link(Rel.Publication) == null) {
            throw LcpException.Parsing.LicenseDocument
        }

        // Check that the acquisition link has a valid URL.
        try {
            link(Rel.Publication)!!.href()
        } catch (e: Exception) {
            throw LcpException.Parsing.Url(rel = LicenseDocument.Rel.Publication.value)
        }
    }

    public constructor(data: ByteArray) : this(
        try {
            JSONObject(data.decodeToString())
        } catch (e: Exception) {
            throw LcpException.Parsing.MalformedJSON
        }
    )

    public enum class Rel(public val value: String) {
        Hint("hint"),
        Publication("publication"),
        Self("self"),
        Support("support"),
        Status("status");

        @Deprecated("Use [value] instead", ReplaceWith("value"), level = DeprecationLevel.ERROR)
        public val rawValue: String get() = value

        public companion object {
            public operator fun invoke(value: String): Rel? = values().firstOrNull { it.value == value }
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
        parameters: URLParameters = emptyMap()
    ): Url {
        val link = link(rel, preferredType)
            ?: links.firstWithRelAndNoType(rel.value)
            ?: throw LcpException.Parsing.Url(rel = rel.value)

        return link.href(parameters = parameters)
    }

    public val description: String
        get() = "License($id)"

    public fun toByteArray(): ByteArray =
        json.toString().toByteArray(Charset.defaultCharset())
}
