/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub.link

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.MediaOverlays
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.publication.JSONParsingException

/**
 * Function used to recursively transform the [href] of a [Link] when parsing its JSON
 * representation.
 */
typealias LinkHrefNormalizer = (String) -> String

/**
 * Default [href] normalizer for [Link], doing nothing.
 */
val LinkHrefNormalizerIdentity: LinkHrefNormalizer = { it }

/**
 * Link Object for the Readium Web Publication Manifest.
 * https://readium.org/webpub-manifest/schema/link.schema.json
 *
 * @param href URI or URI template of the linked resource.
 * @param type MIME type of the linked resource.
 * @param templated Indicates that a URI template is used in href.
 * @param title Title of the linked resource.
 * @param rels Relation between the linked resource and its containing collection.
 * @param properties Properties associated to the linked resource.
 * @param height Height of the linked resource in pixels.
 * @param width Width of the linked resource in pixels.
 * @param bitrate Bitrate of the linked resource in kbps.
 * @param duration Length of the linked resource in seconds.
 * @param languages Expected language of the linked resource (BCP 47 tag).
 * @param alternates Alternate resources for the linked resource.
 * @param children Resources that are children of the linked resource, in the context of a given
 *     collection role.
 * @param mediaOverlays The MediaOverlays associated to the resource of the `Link`.
 *     WARNING: Media overlays are in beta and the API is subject to change in the future.
 */
data class Link(
    var href: String,
    var type: String? = null,
    var templated: Boolean = false,
    var title: String? = null,
    var rels: List<String> = listOf(),
    var properties: Properties = Properties(),
    var height: Int? = null,
    var width: Int? = null,
    var bitrate: Double? = null,
    var duration: Double? = null,
    var languages: List<String> = listOf(),
    var alternates: List<Link> = listOf(),
    var children: List<Link> = listOf(),
    var mediaOverlays: MediaOverlays = MediaOverlays()
) : JSONable, Serializable {

    override fun toJSON(): JSONObject = JSONObject().apply {
        put("href", href)
        put("type", type)
        put("templated", templated)
        put("title", title)
        putIfNotEmpty("rel", rels)
        putIfNotEmpty("properties", properties)
        put("height", height)
        put("width", width)
        put("bitrate", bitrate)
        put("duration", duration)
        putIfNotEmpty("language", languages)
        putIfNotEmpty("alternate", alternates.map(Link::toJSON))
        putIfNotEmpty("children", children.map(Link::toJSON))
    }

    @Deprecated(message = "Use `type` instead", replaceWith = ReplaceWith(expression = "type"))
    var typeLink: String?
        get() = type
        set(value) { type = value }

    @Deprecated(message = "Use `rels` instead.", replaceWith = ReplaceWith(expression = "rels"))
    var rel: List<String>
        get() = rels
        set(value) { rels = value }

    companion object {

        fun fromJSON(json: JSONObject?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): Link? {
            val href = json?.optNullableString("href")
                ?: return null

            return Link(
                href = normalizeHref(href),
                type = json.optNullableString("type"),
                templated = json.optBoolean("templated", false),
                title = json.optNullableString("title"),
                rels = json.optStringsFromArrayOrSingle("rel"),
                properties = Properties.fromJSON(json.optJSONObject("properties")),
                height = json.optPositiveInt("height"),
                width = json.optPositiveInt("width"),
                bitrate = json.optPositiveDouble("bitrate"),
                duration = json.optPositiveDouble("duration"),
                languages = json.optStringsFromArrayOrSingle("language"),
                alternates = fromJSONArray(json.optJSONArray("alternate"), normalizeHref),
                children = fromJSONArray(json.optJSONArray("children"), normalizeHref)
            )
        }

        fun fromJSONArray(json: JSONArray?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): List<Link> {
            json ?: return emptyList()

            val links = mutableListOf<Link>()
            for (i in 0 until json.length()) {
                val link = fromJSON(json.optJSONObject(i), normalizeHref)
                if (link != null) {
                    links.add(link)
                }
            }
            return links
        }

    }

}

/**
 * Converts a list of [Link] into a [JSONArray].
 */
fun List<Link>.toJSON(): JSONArray = JSONArray(map(Link::toJSON))

/*
enum class LinkError(var v: String) {
    InvalidLink("Invalid link"),
}

fun parseLink(linkDict: JSONObject, feedUrl: URL? = null): Link {
    val link = Link()
    if (linkDict.has("title")) {
        link.title = linkDict.getString("title")
    }
    if (linkDict.has("href")) {
        feedUrl?.let {
            link.href = getAbsolute(linkDict.getString("href"), feedUrl.toString())
        } ?: run {
            link.href = linkDict.getString("href")
        }
    }
    if (linkDict.has("type")) {
        link.typeLink = linkDict.getString("type")
    }
    if (linkDict.has("rel")) {
        if (linkDict.get("rel") is String) {
            link.rel.add(linkDict.getString("rel"))
        } else if (linkDict.get("rel") is JSONArray) {
            val array = linkDict.getJSONArray("rel")
            for (i in 0 until array.length()) {
                val string = array.getString(i)
                link.rel.add(string)
            }
        }
    }
    if (linkDict.has("height")) {
        link.height = linkDict.getInt("height")
    }
    if (linkDict.has("width")) {
        link.width = linkDict.getInt("width")
    }
    if (linkDict.has("bitrate")) {
        link.bitrate = linkDict.getInt("bitrate")
    }
    if (linkDict.has("duration")) {
        link.duration = linkDict.getDouble("duration")
    }
    if (linkDict.has("properties")) {
        val properties = Properties()
        val propertiesDict = linkDict.getJSONObject("properties")
        if (propertiesDict.has("numberOfItems")) {
            properties.numberOfItems = propertiesDict.getInt("numberOfItems")
        }
        if (propertiesDict.has("indirectAcquisition")) {
            val acquisitions = propertiesDict.getJSONArray("indirectAcquisition")
                ?: throw Exception(LinkError.InvalidLink.name)
            for (i in 0 until acquisitions.length()) {
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
                throw Exception(LinkError.InvalidLink.name)
            }
            val price = Price(currency = currency, value = value)
            properties.price = price
        }
        link.properties = properties
    }
    if (linkDict.has("children")) {
        linkDict.get("children").let {
            val children = it as? JSONArray
                ?: throw Exception(LinkError.InvalidLink.name)
            for (i in 0 until children.length()) {
                val childLinkDict = children.getJSONObject(i)
                val childLink = parseLink(childLinkDict)
                link.children.add(childLink)
            }
        }
    }
    return link
}
*/
