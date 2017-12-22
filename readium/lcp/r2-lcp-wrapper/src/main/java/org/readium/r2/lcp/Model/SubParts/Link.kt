package org.readium.r2.lcp.Model.SubParts

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class Link(val json: JSONObject) {


    var href: URL
    /// Indicates the relationship between the resource and its containing collection.
    var rel = mutableListOf<String>()
    /// Title for the Link.
    var title: String?
    /// MIME type of resource.
    var type: String?
    /// Indicates that the linked resource is a URI template.
    var templated: Boolean
    /// Expected profile used to identify the external resource. (URI)
    var profile: URL?
    /// Content length in octets.
    var length: Int?
    /// SHA-256 hash of the resource.
    var hash: String?

    init{
        href = URL(json.getString("href"))

        val rel = json.getJSONArray("rel")
        for (i in 0..rel.length() - 1) {
            this.rel.add(rel[i].toString())
        }

        title = json.getString("title")
        type = json.getString("type")
        templated = json.getString("templated").toBoolean()
        profile = URL(json.getString("profile"))
        length = json.getString("length").toInt()
        hash = json.getString("hash")
    }

    fun parseLinks(key: String) : List<Link> {
        val array = json.getJSONArray(key)
        val links = mutableListOf<Link>()
        for (i in 0..array.length()- 1 ){
            links.add(Link(array[i] as JSONObject))
        }
        return links
    }
}
