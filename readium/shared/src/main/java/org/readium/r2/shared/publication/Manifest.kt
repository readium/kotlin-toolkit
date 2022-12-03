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
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.removeLastComponent
import org.readium.r2.shared.extensions.toUrlOrNull
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Holds the metadata of a Readium publication, as described in the Readium Web Publication Manifest.
 */
@Parcelize
data class Manifest(
    val context: List<String> = emptyList(),
    val metadata: Metadata,
    // FIXME: Currently Readium requires to set the [Link] with [rel] "self" when adding it to the
    //     server. So we need to keep [links] as a mutable property.
    var links: List<Link> = emptyList(),
    val readingOrder: List<Link> = emptyList(),
    val resources: List<Link> = emptyList(),
    val tableOfContents: List<Link> = emptyList(),
    val subcollections: Map<String, List<PublicationCollection>> = emptyMap()

) : JSONable, Parcelable {

    /**
     * Returns whether this manifest conforms to the given Readium Web Publication Profile.
     */
    fun conformsTo(profile: Publication.Profile): Boolean {
        if (readingOrder.isEmpty()) {
            return false
        }

        return when (profile) {
            Publication.Profile.AUDIOBOOK -> readingOrder.allAreAudio
            Publication.Profile.DIVINA -> readingOrder.allAreBitmap
            Publication.Profile.EPUB ->
                // EPUB needs to be explicitly indicated in `conformsTo`, otherwise
                // it could be a regular Web Publication.
                readingOrder.allAreHtml && metadata.conformsTo.contains(Publication.Profile.EPUB)
            Publication.Profile.PDF -> readingOrder.allMatchMediaType(MediaType.PDF)
            else -> metadata.conformsTo.contains(profile)
        }
    }

    /**
     * Finds the first [Link] with the given HREF in the manifest's links.
     *
     * Searches through (in order) [readingOrder], [resources] and [links] recursively following
     * alternate and children links.
     *
     * If there's no match, tries again after removing any query parameter and anchor from the
     * given [href].
     */
    fun linkWithHref(href: String): Link? {
        fun List<Link>.deepLinkWithHref(href: String): Link? {
            for (l in this) {
                if (l.href == href)
                    return l
                else {
                    l.alternates.deepLinkWithHref(href)?.let { return it }
                    l.children.deepLinkWithHref(href)?.let { return it }
                }
            }
            return null
        }

        fun find(href: String): Link? {
            return readingOrder.deepLinkWithHref(href)
                ?: resources.deepLinkWithHref(href)
                ?: links.deepLinkWithHref(href)
        }

        return find(href)
            ?: find(href.takeWhile { it !in "#?" })
    }

    /**
     * Finds the first [Link] with the given relation in the manifest's links.
     */
    fun linkWithRel(rel: String): Link? =
        readingOrder.firstWithRel(rel)
            ?: resources.firstWithRel(rel)
            ?: links.firstWithRel(rel)

    /**
     * Finds all [Link]s having the given [rel] in the manifest's links.
     */
    fun linksWithRel(rel: String): List<Link> =
        (readingOrder + resources + links).filterByRel(rel)

    /**
     * Creates a new [Locator] object from a [Link] to a resource of this manifest.
     *
     * Returns null if the resource is not found in this manifest.
     */
    fun locatorFromLink(link: Link): Locator? {
        val components = link.href.split("#", limit = 2)
        val href = components.firstOrNull() ?: link.href
        val resourceLink = linkWithHref(href) ?: return null
        val type = resourceLink.type ?: return null
        val fragment = components.getOrNull(1)

        return Locator(
            href = href,
            type = type,
            title = resourceLink.title ?: link.title,
            locations = Locator.Locations(
                fragments = listOfNotNull(fragment),
                progression = if (fragment == null) 0.0 else null
            )
        )
    }

    /**
     * Serializes a [Publication] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        putIfNotEmpty("@context", context)
        put("metadata", metadata.toJSON())
        put("links", links.toJSON())
        put("readingOrder", readingOrder.toJSON())
        putIfNotEmpty("resources", resources)
        putIfNotEmpty("toc", tableOfContents)
        subcollections.appendToJSONObject(this)
    }

    /**
     * Returns the RWPM JSON representation for this manifest, as a string.
     */
    override fun toString(): String = toJSON().toString().replace("\\/", "/")

    companion object {

        /**
         * Parses a [Manifest] from its RWPM JSON representation.
         *
         * If the publication can't be parsed, a warning will be logged with [warnings].
         * https://readium.org/webpub-manifest/
         * https://readium.org/webpub-manifest/schema/publication.schema.json
         */
        fun fromJSON(
            json: JSONObject?,
            packaged: Boolean = false,
            warnings: WarningLogger? = null
        ): Manifest? {
            json ?: return null

            val baseUrl =
                if (packaged)
                    "/"
                else
                    Link.fromJSONArray(json.optJSONArray("links"), warnings = warnings)
                        .firstWithRel("self")
                        ?.href
                        ?.toUrlOrNull()
                        ?.removeLastComponent()
                        ?.toString()
                        ?: "/"

            val normalizeHref = { href: String -> Href(href, baseUrl).string }

            val context = json.optStringsFromArrayOrSingle("@context", remove = true)

            val metadata = Metadata.fromJSON(json.remove("metadata") as? JSONObject, normalizeHref, warnings)
            if (metadata == null) {
                warnings?.log(Manifest::class.java, "[metadata] is required", json)
                return null
            }

            val links = Link.fromJSONArray(json.remove("links") as? JSONArray, normalizeHref, warnings)
                .map { if (!packaged || "self" !in it.rels) it else it.copy(rels = it.rels - "self" + "alternate") }

            // [readingOrder] used to be [spine], so we parse [spine] as a fallback.
            val readingOrderJSON = (json.remove("readingOrder") ?: json.remove("spine")) as? JSONArray
            val readingOrder = Link.fromJSONArray(readingOrderJSON, normalizeHref, warnings)
                .filter { it.type != null }

            val resources = Link.fromJSONArray(json.remove("resources") as? JSONArray, normalizeHref, warnings)
                .filter { it.type != null }

            val tableOfContents = Link.fromJSONArray(json.remove("toc") as? JSONArray, normalizeHref, warnings)

            // Parses subcollections from the remaining JSON properties.
            val subcollections = PublicationCollection.collectionsFromJSON(json, normalizeHref, warnings)

            return Manifest(
                context = context,
                metadata = metadata,
                links = links,
                readingOrder = readingOrder,
                resources = resources,
                tableOfContents = tableOfContents,
                subcollections = subcollections
            )
        }
    }
}
