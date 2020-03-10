/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model

import org.joda.time.DateTime
import org.json.JSONObject
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.Links
import org.readium.r2.lcp.license.model.components.lcp.Encryption
import org.readium.r2.lcp.license.model.components.lcp.Rights
import org.readium.r2.lcp.license.model.components.lcp.Signature
import org.readium.r2.lcp.license.model.components.lcp.User
import org.readium.r2.lcp.ParsingError
import java.net.URL
import java.nio.charset.Charset

data class LicenseDocument(val data: ByteArray) {
    val provider: String
    val id: String
    val issued: DateTime
    val updated: DateTime
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
            throw ParsingError.malformedJSON
        }
        provider = if (json.has("provider")) json.getString("provider") else throw ParsingError.licenseDocument
        id = if (json.has("id")) json.getString("id") else throw ParsingError.licenseDocument
        issued = if (json.has("issued")) DateTime(json.getString("issued")) else throw ParsingError.licenseDocument
        encryption = if (json.has("encryption")) Encryption(json.getJSONObject("encryption")) else throw ParsingError.licenseDocument
        signature = if (json.has("signature")) Signature(json.getJSONObject("signature")) else throw ParsingError.licenseDocument
        links = if (json.has("links")) Links(json.getJSONArray("links")) else throw ParsingError.licenseDocument
        updated = if (json.has("updated")) DateTime(json.getString("updated")) else issued
        user = if (json.has("user")) User(json.getJSONObject("user")) else User(JSONObject())
        rights = if (json.has("rights")) Rights(json.getJSONObject("rights")) else Rights(JSONObject())
        if (link(Rel.hint) == null || link(Rel.publication) == null) {
            throw ParsingError.licenseDocument
        }
    }

    fun link(rel: Rel): Link? =
            links[rel.rawValue].firstOrNull()

    fun links(rel: Rel): List<Link> =
            links[rel.rawValue]

    fun url(rel: Rel, parameters:  List<Pair<String, Any?>> = listOf()): URL {
        return link(rel)?.url(parameters) ?: throw ParsingError.url(rel = rel.rawValue)
    }

    val description: String
        get() = "License($id)"

}
