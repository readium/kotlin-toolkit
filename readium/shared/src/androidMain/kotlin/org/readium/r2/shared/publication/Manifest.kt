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

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.logging.ConsoleWarningLogger
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Holds the metadata of a Readium publication, as described in the Readium Web Publication Manifest.
 */
public data class Manifest(
    val context: List<String> = emptyList(),
    val metadata: Metadata,
    val links: List<Link> = emptyList(),
    val readingOrder: List<Link> = emptyList(),
    val resources: List<Link> = emptyList(),
    val tableOfContents: List<Link> = emptyList(),
    val subcollections: Map<String, List<PublicationCollection>> = emptyMap(),
) : JSONable {

    /**
     * Returns whether this manifest conforms to the given Readium Web Publication Profile.
     */
    public fun conformsTo(profile: Publication.Profile): Boolean {
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
    @OptIn(DelicateReadiumApi::class)
    public fun linkWithHref(href: Url): Link? {
        fun List<Link>.deepLinkWithHref(href: Url): Link? {
            for (l in this) {
                if (l.url().normalize() == href) {
                    return l
                } else {
                    l.alternates.deepLinkWithHref(href)?.let { return it }
                    l.children.deepLinkWithHref(href)?.let { return it }
                }
            }
            return null
        }

        fun find(href: Url): Link? {
            return readingOrder.deepLinkWithHref(href)
                ?: resources.deepLinkWithHref(href)
                ?: links.deepLinkWithHref(href)
        }

        val normalizedHref = href.normalize()
        return find(normalizedHref)
            ?: find(normalizedHref.removeFragment().removeQuery())
    }

    /**
     * Finds the first [Link] with the given relation in the manifest's links.
     */
    public fun linkWithRel(rel: String): Link? =
        readingOrder.firstWithRel(rel)
            ?: resources.firstWithRel(rel)
            ?: links.firstWithRel(rel)

    /**
     * Finds all [Link]s having the given [rel] in the manifest's links.
     */
    public fun linksWithRel(rel: String): List<Link> =
        (readingOrder + resources + links).filterByRel(rel)

    /**
     * Creates a new [Locator] object from a [Link] to a resource of this manifest.
     *
     * Returns null if the resource is not found in this manifest.
     */
    public fun locatorFromLink(link: Link): Locator? {
        var url = link.url()
        val fragment = url.fragment
        url = url.removeFragment()

        val resourceLink = linkWithHref(url) ?: return null
        val mediaType = resourceLink.mediaType ?: return null

        return Locator(
            href = url,
            mediaType = mediaType,
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
    override fun toJSON(): JSONObject = JSONObject().apply {
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

    public companion object {

        /**
         * Parses a [Manifest] from its RWPM JSON representation.
         *
         * If the publication can't be parsed, a warning will be logged with [warnings].
         * https://readium.org/webpub-manifest/
         * https://readium.org/webpub-manifest/schema/publication.schema.json
         */
        public fun fromJSON(
            json: JSONObject?,
            warnings: WarningLogger? = ConsoleWarningLogger(),
        ): Manifest? {
            json ?: return null

            val context = json.optStringsFromArrayOrSingle("@context", remove = true)

            val metadata = Metadata.fromJSON(
                json.remove("metadata") as? JSONObject,
                warnings
            )
            if (metadata == null) {
                warnings?.log(Manifest::class.java, "[metadata] is required", json)
                return null
            }

            val links = Link.fromJSONArray(
                json.remove("links") as? JSONArray,
                warnings
            )

            // [readingOrder] used to be [spine], so we parse [spine] as a fallback.
            val readingOrderJSON = (json.remove("readingOrder") ?: json.remove("spine")) as? JSONArray
            val readingOrder = Link.fromJSONArray(
                readingOrderJSON,
                warnings
            )
                .filter { it.mediaType != null }

            val resources = Link.fromJSONArray(
                json.remove("resources") as? JSONArray,
                warnings
            )
                .filter { it.mediaType != null }

            val tableOfContents = Link.fromJSONArray(
                json.remove("toc") as? JSONArray,
                warnings
            )

            // Parses subcollections from the remaining JSON properties.
            val subcollections = PublicationCollection.collectionsFromJSON(
                json,
                warnings
            )

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
