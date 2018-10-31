/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.documents

import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpErrorCase
import org.readium.r2.lcp.model.sub.Link
import org.readium.r2.lcp.model.sub.lcp.Encryption
import org.readium.r2.lcp.model.sub.lcp.Rights
import org.readium.r2.lcp.model.sub.lcp.Signature
import org.readium.r2.lcp.model.sub.lcp.User
import org.readium.r2.lcp.model.sub.parseLinks
import java.net.URL
import java.nio.charset.Charset

/// Document that contains references to the various keys, links to related
/// external resources, rights and restrictions that are applied to the
/// Protected Publication, and user information.
class LicenseDocument {

    var id: String
    /// Date when the license was first issued.
    var issued: DateTime
    /// Date when the license was last updated.
    var updated: DateTime? = null
    /// Unique identifier for the Provider (URI).
    var provider: URL
    // Encryption object.
    var encryption: Encryption
    /// Used to associate the License Document with resources that are not
    /// locally available.
    var links = listOf<Link>()
    var rights: Rights? = null
    /// The user owning the License.
    var user: User? = null
    /// Used to validate the license integrity.
    var signature: Signature
    var json: JSONObject

    // The possible rel of Links.
    enum class Rel(val v:String) {
        hint("hint"),
        publication("publication"),
        status("status")
    }

    constructor(data: ByteArray) {
        val text = data.toString(Charset.defaultCharset())
        try {
            json = JSONObject(text)
        } catch (e: Exception) {
            throw Exception("Lcp parsing error")
        }

        try {
            id = json.getString("id")
            issued = DateTime(json.getString("issued"))
            provider = URL(json.getString("provider"))
        } catch (e: Exception) {
            throw Exception("Lcp parsing error")
        }

        encryption = Encryption(JSONObject(json.getString("encryption")))
        links = parseLinks(json["links"] as JSONArray)

        if (json.has("rights")) {
            rights = Rights(json.getJSONObject("rights"))
        }

        if (json.has("potential_rights")) {
            rights?.potentialEnd = DateTime(json.getJSONObject("potential_rights").getString("end"))
        }

        if (json.has("user")) {
            user = User(json.getJSONObject("user"))
        }

        signature = Signature(json.getJSONObject("signature"))

        if (json.has("updated")) {
            updated = DateTime(json.getString("updated"))
        }

        if (link("hint") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.hintLinkNotFound))
        }

        if (link("publication") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.publicationLinkNotFound))
        }
    }

    /// Returns the date of last update if any, or issued date.
    fun dateOfLastUpdate() = if (updated != null) updated!! else issued

    /// Returns the first link containing the given rel.
    ///
    /// - Parameter rel: The rel to look for.
    /// - Returns: The first link containing the rel.
    fun link(rel: String) = links.firstOrNull{
        it.rel.contains(rel)
    }

    fun getHint() = encryption.userKey.hint

}