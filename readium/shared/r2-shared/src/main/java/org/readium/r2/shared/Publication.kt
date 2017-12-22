package org.readium.r2.shared

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.net.URL

fun URL.removeLastComponent() : URL{
    var str = this.toString()
    val i = str.lastIndexOf('/', 0, true)
    if (i != -1)
        str = str.substring(0, i)
    return URL(str)
}

fun getJSONArray(list: List<JSONable>) : JSONArray{
    val array = JSONArray()
    for(i in list){
        array.put(i.getJSON())
    }
    return array
}

fun getStringArray(list: List<Any>) : JSONArray {
    val array = JSONArray()
    for(i in list){
        array.put(i)
    }
    return array
}

fun tryPut(obj: JSONObject, list: List<JSONable>, tag: String){
    if (list.isNotEmpty())
        obj.putOpt(tag, getJSONArray(list))
}

class TocElement(val link: Link, val children: List<TocElement>) : JSONable {

    override fun getJSON(): JSONObject {
        val json = link.getJSON()
        tryPut(json, children, "children")
        return json
    }

}

class Publication : Serializable {

    /// The version of the publication, if the type needs any.
    var version: Double = 0.0
    /// The metadata (title, identifier, contributors, etc.).
    var metadata: Metadata = Metadata()
    /// org.readium.r2shared.Publication.org.readium.r2shared.Link to special ressources which are added to the publication.
    private var links: MutableList<Link> = mutableListOf()
    /// Links of the spine items of the publication.
    var spine: MutableList<Link> = mutableListOf()
    /// Link to the ressources of the publication.
    var resources: MutableList<Link> = mutableListOf()
    /// Table of content of the publication.
    var tableOfContents: MutableList<Link> = mutableListOf()
    var landmarks: MutableList<Link> = mutableListOf()
    var listOfAudioFiles: MutableList<Link> = mutableListOf()
    var listOfIllustrations: MutableList<Link> = mutableListOf()
    var listOfTables: MutableList<Link> = mutableListOf()
    var listOfVideos: MutableList<Link> = mutableListOf()
    var pageList: MutableList<Link> = mutableListOf()

    /// Extension point for links that shouldn't show up in the manifest.
    var otherLinks: MutableList<Link> = mutableListOf()
    var internalData: MutableMap<String, String> = mutableMapOf()
    //var manifestDictionnary: Map<String, Any> = mapOf()
    var coverLink: Link?  = null
        get() = linkWithRel("cover")

    fun baseUrl() : URL? {
        val selfLink = linkWithRel("self")
        val url = selfLink?.let{ URL(selfLink.href)}
        val index = url.toString().lastIndexOf('/')
        return URL(url.toString().substring(0, index))
    }

    //  To see later : build the manifest
    fun manifest() : String{
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

    fun resource(relativePath: String) : Link? = (spine + resources).first({it.href == relativePath})

    fun spineLink(href: String) : Link? = spine.first({it.href == href})

    fun linkWithRel(rel: String) : Link? {
        val findLinkWithRel: (Link) -> Boolean = { it.rel.contains(rel) }
        return findLinkInPublicationLinks(findLinkWithRel)
    }

    fun linkWithHref(href: String) : Link? {
        val findLinkWithHref: (Link) -> Boolean = { (href == it.href) || ("/" + href == it.href)}
        return findLinkInPublicationLinks(findLinkWithHref)
    }

    fun uriTo(link: Link?) : URL? {
        val linkHref = link?.href
        val publicationBaseUrl = baseUrl()
        if (link != null && linkHref != null && publicationBaseUrl != null)
            return null
        //  Issue : ???
        val trimmedBaseUrlString = publicationBaseUrl.toString().trim('/')
        return URL(trimmedBaseUrlString + "/" + linkHref)
    }

    fun addSelfLink(endPoint: String, baseURL: URL){
        val publicationUrl: URL
        val link = Link()
        val manifestPath = "$endPoint/manifest.json"

        publicationUrl = URL(baseURL.toString() + manifestPath)
        link.href = publicationUrl.toString()
        link.typeLink = "application/webpub+json"
        link.rel.add("self")
        links.add(link)
    }

    private fun findLinkInPublicationLinks (closure: (Link) -> Boolean) =
            resources.firstOrNull(closure) ?:
                spine.firstOrNull(closure) ?:
                links.firstOrNull(closure)

}