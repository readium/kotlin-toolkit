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
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.log

/**
 * Function used to recursively transform the href of a [Link] when parsing its JSON
 * representation.
 */
typealias LinkHrefNormalizer = (String) -> String

/**
 * Default href normalizer for [Link], doing nothing.
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
 */
@Parcelize
data class Link(
    val href: String,
    val type: String? = null,
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

    companion object {

        fun fromJSON(json: JSONObject?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): Link? =
            fromJSON(json, normalizeHref, null)

        /**
         * Creates an [Link] from its RWPM JSON representation.
         * It's [href] and its children's recursively will be normalized using the provided
         * [normalizeHref] closure.
         * If the link can't be parsed, a warning will be logged with [warnings].
         */
        internal fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger<JsonWarning>?
        ): Link? {
            val href = json?.optNullableString("href")
            if (href == null) {
                warnings?.log(Link::class.java, "[href] is required", json)
                return null
            }

            return Link(
                href = normalizeHref(href),
                type = json.optNullableString("type"),
                templated = json.optBoolean("templated", false),
                title = json.optNullableString("title"),
                rels = json.optStringsFromArrayOrSingle("rel").toSet(),
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

        fun fromJSONArray(json: JSONArray?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): List<Link> =
            fromJSONArray(json, normalizeHref, null)

        /**
         * Creates a list of [Link] from its RWPM JSON representation.
         * It's [href] and its children's recursively will be normalized using the provided
         * [normalizeHref] closure.
         * If a link can't be parsed, a warning will be logged with [warnings].
         */
        internal fun fromJSONArray(
            json: JSONArray?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger<JsonWarning>?
        ): List<Link> {
            return json.parseObjects { fromJSON(it as? JSONObject, normalizeHref, warnings) }
        }

    }

    @Deprecated("Use [type] instead", ReplaceWith("type"))
    val typeLink: String?
        get() = type

    @Deprecated("Use [rels] instead.", ReplaceWith("rels"))
    val rel: List<String>
        get() = rels.toList()

}

/**
 * Returns the first [Link] with the given [href], or [null] if not found.
 */
fun List<Link>.indexOfFirstWithHref(href: String): Int? =
    indexOfFirst { it.href == href }
        .takeUnless { it == -1 }
