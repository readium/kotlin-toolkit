/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.net.URL


fun URL.removeLastComponent(): URL {
    var str = this.toString()
    val i = str.lastIndexOf('/', 0, true)
    if (i != -1)
        str = str.substring(0, i)
    return URL(str)
}

fun getJSONArray(list: List<JSONable>): JSONArray {
    val array = JSONArray()
    for (i in list) {
        array.put(i.getJSON())
    }
    return array
}

fun getStringArray(list: List<Any>): JSONArray {
    val array = JSONArray()
    for (i in list) {
        array.put(i)
    }
    return array
}

fun tryPut(obj: JSONObject, list: List<JSONable>, tag: String) {
    if (list.isNotEmpty())
        obj.putOpt(tag, getJSONArray(list))
}

// Try to put class which implements JSONable only if not empty
fun tryPut(jsonObject: JSONObject, jsonable: JSONable, tag: String) {
    val tempJsonObject = jsonable.getJSON()
    if (tempJsonObject.length() != 0)
        jsonObject.put(tag, tempJsonObject)
}

class TocElement(val link: Link, val children: List<TocElement>) : JSONable {

    override fun getJSON(): JSONObject {
        val json = link.getJSON()
        tryPut(json, children, "children")
        return json
    }

}

/**
 * Publication store every information and meta data about an artwork and provides
 *      helpers to get some resources
 *
 * If you want to add new publications type, you shall add fields / methods to that
 *      class
 *
 */
class Publication : Serializable {

    /**
     * Enumeration of every handled mime type
     *
     * There you should add your new mime type to handle a new kind of publication and
     *      use it to check the type on your implementation
     *
     */
    enum class TYPE {
        EPUB, CBZ, FXL
    }

    open class EnumCompanion<T, V>(private val valueMap: Map<T, V>) {
        fun fromString(type: T) = valueMap[type]
    }

    enum class EXTENSION(var value: String) {
        EPUB(".epub"),
        CBZ(".cbz");
        companion object : EnumCompanion<String, EXTENSION>(EXTENSION.values().associateBy(EXTENSION::value))
    }


    /// The kind of publication it is ( Epub, Cbz, ... )
    var type = TYPE.EPUB
    /// The version of the publication, if the type needs any.
    var version: Double = 0.0
    /// The metadata (title, identifier, contributors, etc.).
    var metadata: Metadata = Metadata()
    /// org.readium.r2shared.Publication.org.readium.r2shared.Link to special resources which are added to the publication.
    var links: MutableList<Link> = mutableListOf()
    /// Links of the spine items of the publication.
    var spine: MutableList<Link> = mutableListOf()
    /// Link to the resources of the publication.
    var resources: MutableList<Link> = mutableListOf()
    /// Table of content of the publication.
    var tableOfContents: MutableList<Link> = mutableListOf()
    var landmarks: MutableList<Link> = mutableListOf()
    var listOfAudioFiles: MutableList<Link> = mutableListOf()
    var listOfIllustrations: MutableList<Link> = mutableListOf()
    var listOfTables: MutableList<Link> = mutableListOf()
    var listOfVideos: MutableList<Link> = mutableListOf()
    var pageList: MutableList<Link> = mutableListOf()

    var images: MutableList<Link> = mutableListOf()

    /// Extension point for links that shouldn't show up in the manifest.
    var otherLinks: MutableList<Link> = mutableListOf()
    var internalData: MutableMap<String, String> = mutableMapOf()

    var coverLink: Link? = null
        get() = linkWithRel("cover")

    fun baseUrl(): URL? {
        val selfLink = linkWithRel("self")
        if (selfLink != null) {
            val url = selfLink.let { URL(selfLink.href) }
            val index = url.toString().lastIndexOf('/')
            return URL(url.toString().substring(0, index))
        }
        return null
    }

    fun manifest(): String {
        val json = JSONObject()
        json.put("metadata", metadata.writeJSON())
        tryPut(json, links, "links")
        tryPut(json, spine, "spine")
        tryPut(json, resources, "resources")
        tryPut(json, tableOfContents, "toc")
        tryPut(json, pageList, "page-list")
        tryPut(json, landmarks, "landmarks")
        tryPut(json, listOfIllustrations, "loi")
        tryPut(json, listOfTables, "lot")
        var str = json.toString()
        str = str.replace("\\/", "/")
        return str
    }

    fun resource(relativePath: String): Link? = (spine + resources).first { (it.href == relativePath) || (it.href == "/$relativePath") }

    fun linkWithRel(rel: String): Link? {
        val findLinkWithRel: (Link) -> Boolean = { it.rel.contains(rel) }
        return findLinkInPublicationLinks(findLinkWithRel)
    }

    fun linkWithHref(href: String): Link? {
        val findLinkWithHref: (Link) -> Boolean = { (href == it.href) || ("/$href" == it.href) }
        return findLinkInPublicationLinks(findLinkWithHref)
    }

    fun addSelfLink(endPoint: String, baseURL: URL) {
        val publicationUrl: URL
        val link = Link()
        val manifestPath = "$endPoint/manifest.json"

        publicationUrl = URL(baseURL.toString() + manifestPath)
        link.href = publicationUrl.toString()
        link.typeLink = "application/webpub+json"
        link.rel.add("self")
        links.add(link)
    }

    private fun findLinkInPublicationLinks(closure: (Link) -> Boolean) =
            resources.firstOrNull(closure) ?: spine.firstOrNull(closure)
            ?: links.firstOrNull(closure) ?: pageList.firstOrNull(closure)

    enum class PublicationError(var v: String) {
        InvalidPublication("Invalid publication")
    }

}

/**
 * Parse a JSON dictionary of extra information into a publication
 *
 */
fun parsePublication(pubDict: JSONObject): Publication {
    val p = Publication()

    if (pubDict.has("metadata")) {
        pubDict.get("metadata")?.let {
            val metadataDict = it as? JSONObject
                    ?: throw Exception(Publication.PublicationError.InvalidPublication.name)
            val metadata = parseMetadata(metadataDict)
            p.metadata = metadata

        }
    }
    if (pubDict.has("links")) {
        pubDict.get("links")?.let {
            val links = it as? JSONArray
                    ?: throw Exception(Publication.PublicationError.InvalidPublication.name)
            for (i in 0..(links.length() - 1)) {
                val linkDict = links.getJSONObject(i)
                val link = parseLink(linkDict)
                p.links.add(link)
            }
        }
    }
    if (pubDict.has("images")) {
        pubDict.get("images")?.let {
            val links = it as? JSONArray
                    ?: throw Exception(Publication.PublicationError.InvalidPublication.name)
            for (i in 0..(links.length() - 1)) {
                val linkDict = links.getJSONObject(i)
                val link = parseLink(linkDict)
                p.images.add(link)
            }
        }
    }
    return p
}
