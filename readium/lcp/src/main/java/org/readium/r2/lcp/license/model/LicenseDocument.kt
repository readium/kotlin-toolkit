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

class LicenseDocument(val data: ByteArray) {
    val provider: String
    val id: String
    val issued: Date
    val updated: Date
    val encryption: Encryption
    val links: Links
    val user: User
    val rights: Rights
    val signature: Signature
    val json: JSONObject

    enum class Rel(val rawValue: String) {
        hint("hint"),
        publication("publication"),
        self("self"),
        support("support"),
        status("status");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
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

    fun link(rel: Rel, type: MediaType? = null): Link? =
        links.firstWithRel(rel.rawValue, type)

    fun links(rel: Rel, type: MediaType? = null): List<Link> =
        links.allWithRel(rel.rawValue, type)

    fun url(rel: Rel, preferredType: MediaType? = null, parameters: URLParameters = emptyMap()): URL {
        val link = link(rel, preferredType)
            ?: links.firstWithRelAndNoType(rel.rawValue)
            ?: throw LcpException.Parsing.Url(rel = rel.rawValue)

        return link.url(parameters)
    }

    val description: String
        get() = "License($id)"
}
