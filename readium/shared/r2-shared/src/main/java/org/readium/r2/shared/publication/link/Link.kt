/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.link

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.MediaOverlays
import org.readium.r2.shared.Warning
import org.readium.r2.shared.WarningLogger
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.putIfNotEmpty

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

    /**
     * Serializes a [Link] to its RWPM JSON representation.
     */
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
        putIfNotEmpty("alternate", alternates)
        putIfNotEmpty("children", children)
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

        /**
         * Creates an [Link] from its RWPM JSON representation.
         * It's [href] and its children's recursively will be normalized using the provided
         * [normalizeHref] closure.
         * If the link can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): Link? {
            val href = json?.optNullableString("href")
            if (href == null) {
                warnings?.log(Warning.JsonParsing(Link::class.java, "[href] is required", json))
                return null
            }

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

        /**
         * Creates a list of [Link] from its RWPM JSON representation.
         * It's [href] and its children's recursively will be normalized using the provided
         * [normalizeHref] closure.
         * If a link can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSONArray(
            json: JSONArray?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): List<Link> {
            return json.parseObjects { fromJSON(it as? JSONObject, normalizeHref, warnings) }
        }

    }

}
