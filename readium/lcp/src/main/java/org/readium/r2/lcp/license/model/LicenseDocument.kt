/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model

import java.net.URL
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
import org.readium.r2.shared.util.mediatype.MediaType

public class LicenseDocument(public val data: ByteArray) {
    public val provider: String
    public val id: String
    public val issued: Date
    public val updated: Date
    public val encryption: Encryption
    public val links: Links
    public val user: User
    public val rights: Rights
    public val signature: Signature
    public val json: JSONObject

    public enum class Rel(public val rawValue: String) {
        hint("hint"),
        publication("publication"),
        self("self"),
        support("support"),
        status("status");

        public companion object {
            public operator fun invoke(rawValue: String): Rel? = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    init {
        try {
            json = JSONObject(data.toString(Charset.defaultCharset()))
        } catch (e: Exception) {
            throw LcpException.Parsing.MalformedJSON
        }

        provider = json.optNullableString("provider") ?: throw LcpException.Parsing.LicenseDocument
        id = json.optNullableString("id") ?: throw LcpException.Parsing.LicenseDocument
        issued = json.optNullableString("issued")?.iso8601ToDate() ?: throw LcpException.Parsing.LicenseDocument
        encryption = json.optJSONObject("encryption")?.let { Encryption(it) } ?: throw LcpException.Parsing.LicenseDocument
        signature = json.optJSONObject("signature")?.let { Signature(it) } ?: throw LcpException.Parsing.LicenseDocument
        links = json.optJSONArray("links")?.let { Links(it) } ?: throw LcpException.Parsing.LicenseDocument
        updated = json.optNullableString("updated")?.iso8601ToDate() ?: issued
        user = User(json.optJSONObject("user") ?: JSONObject())
        rights = Rights(json.optJSONObject("rights") ?: JSONObject())

        if (link(Rel.hint) == null || link(Rel.publication) == null) {
            throw LcpException.Parsing.LicenseDocument
        }
    }

    public fun link(rel: Rel, type: MediaType? = null): Link? =
        links.firstWithRel(rel.rawValue, type)

    public fun links(rel: Rel, type: MediaType? = null): List<Link> =
        links.allWithRel(rel.rawValue, type)

    public fun url(
        rel: Rel,
        preferredType: MediaType? = null,
        parameters: URLParameters = emptyMap()
    ): URL {
        val link = link(rel, preferredType)
            ?: links.firstWithRelAndNoType(rel.rawValue)
            ?: throw LcpException.Parsing.Url(rel = rel.rawValue)

        return link.url(parameters)
    }

    public val description: String
        get() = "License($id)"
}
