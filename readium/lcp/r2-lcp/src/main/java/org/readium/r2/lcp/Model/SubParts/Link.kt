package org.readium.r2.lcp.Model.SubParts

import org.json.JSONArray
import org.json.JSONObject
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
        href = URL(json.getString("href"))

        val rel = json.getString("rel")
        this.rel.add(rel)

//        val rel = json.getJSONArray("rel")
//        for (i in 0..rel.length() - 1) {
//            this.rel.add(rel[i].toString())
//        }
        if(json.has("title")) {
            title = json.getString("title")
        }
        if(json.has("type")) {
            type = json.getString("type")
        }
        if(json.has("templated")) {
            templated = json.getString("templated").toBoolean()
        }
        if(json.has("profile")) {
            profile = URL(json.getString("profile"))
        }
        if(json.has("length")) {
            length = json.getString("length").toInt()
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