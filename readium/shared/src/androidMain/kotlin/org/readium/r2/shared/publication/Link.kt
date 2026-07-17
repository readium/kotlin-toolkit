/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.optPositiveDouble
import org.readium.r2.shared.extensions.optPositiveInt
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.parseObjects
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Link Object for the Readium Web Publication Manifest.
 * https://readium.org/webpub-manifest/schema/link.schema.json
 *
 * @param href URI or URI template of the linked resource.
 * @param mediaType Media type of the linked resource.
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
 */
@Parcelize
public data class Link(
    val href: Href,
    val mediaType: MediaType? = null,
    val title: String? = null,
    val rels: Set<String> = setOf(),
    val properties: Properties = Properties(),
    val height: Int? = null,
    val width: Int? = null,
    val bitrate: Double? = null,
    val duration: Double? = null,
    val languages: List<String> = listOf(),
    val alternates: List<Link> = listOf(),
    val children: List<Link> = listOf(),
) : JSONable, Parcelable {

    /**
     * Convenience constructor for a [Link] with a [Url] as [href].
     */
    public constructor(
        href: Url,
        mediaType: MediaType? = null,
        title: String? = null,
        rels: Set<String> = setOf(),
        properties: Properties = Properties(),
        alternates: List<Link> = listOf(),
        children: List<Link> = listOf(),
    ) : this(
        href = Href(href),
        mediaType = mediaType,
        title = title,
        rels = rels,
        properties = properties,
        alternates = alternates,
        children = children
    )

    /**
     * Returns the URL represented by this link's HREF, resolved to the given [base] URL.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public fun url(
        base: Url? = null,
        parameters: Map<String, String> = emptyMap(),
    ): Url = href.resolve(base, parameters)

    /**
     * Serializes a [Link] to its RWPM JSON representation.
     */
    override fun toJSON(): JSONObject = JSONObject().apply {
        put("href", href.toString())
        put("type", mediaType?.toString())
        put("templated", href.isTemplated)
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

    /**
     * Makes a copy of this [Link] after merging in the given additional other [properties].
     */
    public fun addProperties(properties: Map<String, Any>): Link =
        copy(properties = this.properties.add(properties))

    public companion object {

        /**
         * Creates an [Link] from its RWPM JSON representation.
         *
         * If the link can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(
            json: JSONObject?,
            warnings: WarningLogger? = null,
        ): Link? {
            json ?: return null

            return Link(
                href = parseHref(json, warnings) ?: return null,
                mediaType = json.optNullableString("type")
                    ?.let { MediaType(it) },
                title = json.optNullableString("title"),
                rels = json.optStringsFromArrayOrSingle("rel").toSet(),
                properties = Properties.fromJSON(json.optJSONObject("properties")),
                height = json.optPositiveInt("height"),
                width = json.optPositiveInt("width"),
                bitrate = json.optPositiveDouble("bitrate"),
                duration = json.optPositiveDouble("duration"),
                languages = json.optStringsFromArrayOrSingle("language"),
                alternates = fromJSONArray(
                    json.optJSONArray("alternate")
                ),
                children = fromJSONArray(
                    json.optJSONArray("children")
                )
            )
        }

        private fun parseHref(
            json: JSONObject,
            warnings: WarningLogger? = null,
        ): Href? {
            val hrefString = json.optNullableString("href")
            if (hrefString == null) {
                warnings?.log(Link::class.java, "[href] is required", json)
                return null
            }

            val templated = json.optBoolean("templated", false)
            val href = if (templated) {
                Href(hrefString, templated = true)
            } else {
                // We support existing publications with incorrect HREFs (not valid percent-encoded
                // URIs). We try to parse them first as valid, but fall back on a percent-decoded
                // path if it fails.
                val url = Url(hrefString) ?: run {
                    warnings?.log(
                        Link::class.java,
                        "[href] is not a valid percent-encoded URL",
                        json
                    )
                    Url.fromDecodedPath(hrefString)
                }
                url?.let { Href(it) }
            }

            if (href == null) {
                warnings?.log(Link::class.java, "[href] is not a valid URL or URL template", json)
            }

            return href
        }

        /**
         * Creates a list of [Link] from its RWPM JSON representation.
         *
         * If a link can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSONArray(
            json: JSONArray?,
            warnings: WarningLogger? = null,
        ): List<Link> {
            return json.parseObjects {
                fromJSON(
                    it as? JSONObject,
                    warnings
                )
            }
        }
    }
}

/**
 * Returns the first [Link] with the given [href], or null if not found.
 */
public fun List<Link>.indexOfFirstWithHref(href: Url): Int? =
    indexOfFirst { it.url().isEquivalent(href) }
        .takeUnless { it == -1 }

/**
 * Finds the first link matching the given HREF.
 */
public fun List<Link>.firstWithHref(href: Url): Link? = firstOrNull { it.url().isEquivalent(href) }

/**
 * Finds the first link with the given relation.
 */
public fun List<Link>.firstWithRel(rel: String): Link? = firstOrNull { it.rels.contains(rel) }

/**
 * Finds all the links with the given relation.
 */
public fun List<Link>.filterByRel(rel: String): List<Link> = filter { it.rels.contains(rel) }

/**
 * Finds the first link matching the given media type.
 */
public fun List<Link>.firstWithMediaType(mediaType: MediaType): Link? = firstOrNull {
    mediaType.matches(it.mediaType)
}

/**
 * Finds all the links matching the given media type.
 */
public fun List<Link>.filterByMediaType(mediaType: MediaType): List<Link> = filter {
    mediaType.matches(it.mediaType)
}

/**
 * Finds all the links matching any of the given media types.
 */
public fun List<Link>.filterByMediaTypes(mediaTypes: List<MediaType>): List<Link> = filter {
    mediaTypes.any { mediaType -> mediaType.matches(it.mediaType) }
}

/**
 * Returns whether all the resources in the collection are bitmaps.
 */
public val List<Link>.allAreBitmap: Boolean get() = isNotEmpty() && all {
    it.mediaType?.isBitmap ?: false
}

/**
 * Returns whether all the resources in the collection are audio clips.
 */
public val List<Link>.allAreAudio: Boolean get() = isNotEmpty() && all {
    it.mediaType?.isAudio ?: false
}

/**
 * Returns whether all the resources in the collection are video clips.
 */
public val List<Link>.allAreVideo: Boolean get() = isNotEmpty() && all {
    it.mediaType?.isVideo ?: false
}

/**
 * Returns whether all the resources in the collection are HTML documents.
 */
public val List<Link>.allAreHtml: Boolean get() = isNotEmpty() && all {
    it.mediaType?.isHtml ?: false
}

/**
 * Returns whether all the resources in the collection are matching the given media type.
 */
public fun List<Link>.allMatchMediaType(mediaType: MediaType): Boolean = isNotEmpty() && all {
    mediaType.matches(it.mediaType)
}

/**
 * Returns whether all the resources in the collection are matching any of the given media types.
 */
public fun List<Link>.allMatchMediaTypes(mediaTypes: List<MediaType>): Boolean = isNotEmpty() && all {
    mediaTypes.any { mediaType -> mediaType.matches(it.mediaType) }
}

/**
 * Returns a list of `Link` after flattening the `children` and `alternates` links of the receiver.
 */
public fun List<Link>.flatten(): List<Link> {
    fun Link.flatten(): List<Link> {
        val children = children.flatten()
        val alternates = alternates.flatten()
        return listOf(this) + children.flatten() + alternates.flatten()
    }

    return flatMap { it.flatten() }
}
