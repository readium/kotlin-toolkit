/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.lcp.Model.SubParts

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.net.URL

class Link(val json: JSONObject) {


    var href: URL
    /// Indicates the relationship between the resource and its containing collection.
    var rel = mutableListOf<String>()
    /// Title for the Link.
    var title: String? = null
    /// MIME type of resource.
    var type: String? = null
    /// Indicates that the linked resource is a URI template.
    var templated: Boolean? = null
    /// Expected profile used to identify the external resource. (URI)
    var profile: URL? = null
    /// Content length in octets.
    var length: Int? = null
    /// SHA-256 hash of the resource.
    var hash: String? = null

    init{
        if(json.has("href")) {
            href = URL(json.getString("href"))
        }
        else {
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.link))
        }

        if(json.has("rel")) {
            val rel = json["rel"]
            if (rel is String) {
                this.rel.add(rel)
            }
//            else if (rel is JSONArray) {
//                for (i in 0 until rel.length()) {
//                    this.rel.add(rel[i].toString())
//                }
//            }
        }
        if (this.rel.isEmpty()) {
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.link))
        }


        if(json.has("title")) {
            title = json.getString("title")
        }
        if(json.has("type")) {
            type = json.getString("type")
        }
        if(json.has("templated")) {
            templated = json.getBoolean("templated")
        }
        if(json.has("profile")) {
            profile = URL(json.getString("profile"))
        }
        if(json.has("length")) {
            length = json.getInt("length")
        }
        if(json.has("hash")) {
            hash = json.getString("hash")
        }
    }


}

fun parseLinks(json: JSONArray) : List<Link> {
//    val array = json.getJSONArray(key)
    val links = mutableListOf<Link>()
    for (i in 0..json.length()- 1 ){
        links.add(Link(json[i] as JSONObject))
    }
    return links
}