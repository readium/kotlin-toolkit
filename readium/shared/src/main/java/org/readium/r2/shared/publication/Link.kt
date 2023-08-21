/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.extensions.optPositiveDouble
import org.readium.r2.shared.extensions.optPositiveInt
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.parseObjects
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.URITemplate
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import org.readium.r2.shared.util.mediatype.DefaultMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.sniff

/**
 * Function used to recursively transform the href of a [Link] when parsing its JSON
 * representation.
 */

public typealias LinkHrefNormalizer = (String) -> String

/**
 * Default href normalizer for [Link], doing nothing.
 */
public val LinkHrefNormalizerIdentity: LinkHrefNormalizer = { it }

/**
 * Link Object for the Readium Web Publication Manifest.
 * https://readium.org/webpub-manifest/schema/link.schema.json
 *
 * @param href URI or URI template of the linked resource.
 * @param mediaType Media type of the linked resource.
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
 */
@Parcelize
public data class Link(
    val href: String,
    val mediaType: MediaType? = null,
    val templated: Boolean = false,
    val title: String? = null,
    val rels: Set<String> = setOf(),
    val properties: Properties = Properties(),
    val height: Int? = null,
    val width: Int? = null,
    val bitrate: Double? = null,
    val duration: Double? = null,
    val languages: List<String> = listOf(),
    val alternates: List<Link> = listOf(),
    val children: List<Link> = listOf()
) : JSONable, Parcelable {

    /**
     * List of URI template parameter keys, if the [Link] is templated.
     */
    @IgnoredOnParcel
    val templateParameters: List<String> by lazy {
        if (!templated) {
            emptyList()
        } else {
            URITemplate(href).parameters
        }
    }

    /**
     * Expands the HREF by replacing URI template variables by the given parameters.
     *
     * See RFC 6570 on URI template.
     */
    public fun expandTemplate(parameters: Map<String, String>): Link =
        copy(href = URITemplate(href).expand(parameters), templated = false)

    /**
     * Computes an absolute URL to the link, relative to the given [baseUrl].
     *
     * If the link's [href] is already absolute, the [baseUrl] is ignored.
     */
    public fun toUrl(baseUrl: String?): String? {
        val href = href.removePrefix("/")
        if (href.isBlank()) {
            return null
        }

        return Href(href, baseHref = baseUrl ?: "/").percentEncodedString
    }

    /**
     * Serializes a [Link] to its RWPM JSON representation.
     */
    override fun toJSON(): JSONObject = JSONObject().apply {
        put("href", href)
        put("type", mediaType?.toString())
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

    /**
     * Makes a copy of this [Link] after merging in the given additional other [properties].
     */
    public fun addProperties(properties: Map<String, Any>): Link =
        copy(properties = this.properties.add(properties))

    public companion object {

        /**
         * Creates an [Link] from its RWPM JSON representation.
         * It's [href] and its children's recursively will be normalized using the provided
         * [normalizeHref] closure.
         * If the link can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(
            json: JSONObject?,
            mediaTypeSniffer: MediaTypeSniffer = DefaultMediaTypeSniffer(),
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): Link? {
            val href = json?.optNullableString("href")
            if (href == null) {
                warnings?.log(Link::class.java, "[href] is required", json)
                return null
            }

            return Link(
                href = normalizeHref(href),
                mediaType = json.optNullableString("type")
                    ?.let { MediaType(it) }
                    ?.let { mediaTypeSniffer.sniff(it) },
                templated = json.optBoolean("templated", false),
                title = json.optNullableString("title"),
                rels = json.optStringsFromArrayOrSingle("rel").toSet(),
                properties = Properties.fromJSON(json.optJSONObject("properties")),
                height = json.optPositiveInt("height"),
                width = json.optPositiveInt("width"),
                bitrate = json.optPositiveDouble("bitrate"),
                duration = json.optPositiveDouble("duration"),
                languages = json.optStringsFromArrayOrSingle("language"),
                alternates = fromJSONArray(
                    json.optJSONArray("alternate"),
                    mediaTypeSniffer,
                    normalizeHref
                ),
                children = fromJSONArray(
                    json.optJSONArray("children"),
                    mediaTypeSniffer,
                    normalizeHref
                )
            )
        }

        /**
         * Creates a list of [Link] from its RWPM JSON representation.
         * It's [href] and its children's recursively will be normalized using the provided
         * [normalizeHref] closure.
         * If a link can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSONArray(
            json: JSONArray?,
            mediaTypeSniffer: MediaTypeSniffer = DefaultMediaTypeSniffer(),
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): List<Link> {
            return json.parseObjects {
                fromJSON(
                    it as? JSONObject,
                    mediaTypeSniffer,
                    normalizeHref,
                    warnings
                )
            }
        }
    }

    @Deprecated(
        "Use [mediaType.toString()] instead",
        ReplaceWith("mediaType.toString()"),
        level = DeprecationLevel.ERROR
    )
    val type: String? get() = throw NotImplementedError()

    @Deprecated("Use [type] instead", ReplaceWith("type"), level = DeprecationLevel.ERROR)
    val typeLink: String? get() = throw NotImplementedError()

    @Deprecated("Use [rels] instead.", ReplaceWith("rels"), level = DeprecationLevel.ERROR)
    val rel: List<String>
        get() = rels.toList()
}

/**
 * Returns the first [Link] with the given [href], or null if not found.
 */
public fun List<Link>.indexOfFirstWithHref(href: String): Int? =
    indexOfFirst { it.href == href }
        .takeUnless { it == -1 }

/**
 * Finds the first link matching the given HREF.
 */
public fun List<Link>.firstWithHref(href: String): Link? = firstOrNull { it.href == href }

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
