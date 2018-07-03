package org.readium.r2.shared

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.opds.Price
import org.readium.r2.shared.opds.parseIndirectAcquisition
import java.io.Serializable
import java.net.URL
import java.sql.Timestamp

//  A link to a resource
class Link : JSONable, Serializable {

    private val TAG = this::class.java.simpleName

    //  The link destination
    var href: String? = null
    /// MIME type of resource.
    var typeLink: String? = null
    /// Indicates the relationship between the resource and its containing collection.
    var rel: MutableList<String> = mutableListOf()
    /// Indicates the height of the linked resource in pixels.
    var height: Int = 0
    /// Indicates the width of the linked resource in pixels.
    var width: Int = 0

    var title: String? = null
    /// Properties associated to the linked resource.
    var properties: Properties = Properties()
    /// Indicates the length of the linked resource in seconds.
    var duration: Double? = null
    /// Indicates that the linked resource is a URI template.
    var templated: Boolean? = false
    /// Indicate the bitrate for the link resource.
    var bitrate: Int? = null

    //  The underlaying nodes in a tree structure of Links
    var children: MutableList<Link> = mutableListOf()
    //  The MediaOverlays associated to the resource of the Link
    var mediaOverlays: MediaOverlays? = null

    fun isEncrypted() : Boolean {
        return properties.encryption != null
    }

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("title", title)
        json.putOpt("type", typeLink)
        json.putOpt("href", href)
        if (rel.isNotEmpty())
            json.put("rel", getStringArray(rel))
        json.putOpt("properties", properties)
        if (height != 0)
            json.putOpt("height", height)
        if (width != 0)
            json.putOpt("width", width)
        json.putOpt("duration", duration)
        return json
    }

}

enum class LinkError(v:String) {
    invalidLink("Invalid link"),
}

fun parseLink(linkDict: JSONObject, feedUrl: URL?) : Link {
    val link = Link()
    if(linkDict.has("title")) {
        link.title = linkDict.getString("title")
    }
    if(linkDict.has("href")) {
        link.href = getAbsolute(linkDict.getString("href")!!, feedUrl.toString())
    }
    if(linkDict.has("type")) {
        link.typeLink = linkDict.getString("type")
    }
    if(linkDict.has("rel")) {
        if (linkDict.get("rel") is String) {
            link.rel.add(linkDict.getString("rel"))
        } else if (linkDict.get("rel") is JSONArray) {
            val array = linkDict.getJSONArray("rel")
            for (i in 0..(array.length() - 1)) {
                val string = array.getString(i)
                link.rel.add(string)
            }
        }
    }
    if(linkDict.has("height")) {
        link.height = linkDict.getInt("height")
    }
    if(linkDict.has("width")) {
        link.width = linkDict.getInt("width")
    }
    if(linkDict.has("bitrate")) {
        link.bitrate = linkDict.getInt("bitrate")
    }
    if(linkDict.has("duration")) {
        link.duration = linkDict.getDouble("duration")
    }
    if(linkDict.has("properties")) {
        val properties = Properties()
        val propertiesDict = linkDict.getJSONObject("properties")
        if (propertiesDict.has("numberOfItems")) {
            properties.numberOfItems = propertiesDict.getInt("numberOfItems")
        }
        if (propertiesDict.has("indirectAcquisition")) {
            val acquisitions = propertiesDict.getJSONArray("indirectAcquisition") ?: throw Exception(LinkError.invalidLink.name)
            for (i in 0..(acquisitions.length() - 1)) {
                val acquisition = acquisitions.getJSONObject(i)
                val indirectAcquisition = parseIndirectAcquisition(indirectAcquisitionDict = acquisition)
                properties.indirectAcquisition.add(indirectAcquisition)
            }
        }
        if (propertiesDict.has("price")) {
            val priceDict = propertiesDict.getJSONObject("price")
            val currency = priceDict["currency"] as? String
            val value = priceDict["value"] as? Double
            if (priceDict == null || currency == null || value == null) {
                throw Exception(LinkError.invalidLink.name)
            }
            val price = Price(currency = currency, value = value)
            properties.price = price
        }
    }
    if(linkDict.has("children")) {
        val childLinkDict = linkDict.getJSONObject("children") ?: throw Exception(LinkError.invalidLink.name)
        val childLink = parseLink(childLinkDict, feedUrl)
        link.children.add(childLink)
    }
    return link
}
