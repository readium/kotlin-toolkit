/*
 * Module: r2-opds-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)

package org.readium.r2.opds

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.opds.Facet
import org.readium.r2.shared.opds.Feed
import org.readium.r2.shared.opds.Group
import org.readium.r2.shared.opds.OpdsMetadata
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.normalizeHrefsToBase
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetchWithDecoder
import org.readium.r2.shared.util.json.optString
import org.readium.r2.shared.util.json.stringOrNull
import org.readium.r2.shared.util.json.toJsonObjectOrNull

public enum class OPDS2ParserError {
    MetadataNotFound,
    InvalidLink,
    MissingTitle,
    InvalidFacet,
    InvalidGroup,
}

public class OPDS2Parser {

    public companion object {

        public suspend fun parseUrlString(
            url: String,
            client: HttpClient = DefaultHttpClient(),
        ): Try<ParseData, Exception> =
            AbsoluteUrl(url)
                ?.let { parseRequest(HttpRequest(it), client) }
                ?: run { Try.failure(Exception("Not an absolute URL.")) }

        public suspend fun parseRequest(
            request: HttpRequest,
            client: HttpClient = DefaultHttpClient(),
        ): Try<ParseData, Exception> {
            return client.fetchWithDecoder(request) {
                this.parse(it.body, request.url)
            }.mapFailure { ErrorException(it) }
        }

        public fun parse(jsonData: ByteArray, url: Url): ParseData {
            val json = jsonData.decodeToString().toJsonObjectOrNull()
                ?: throw Exception("Invalid JSON")
            return if (isFeed(json)) {
                ParseData(parseFeed(json, url), null, 2)
            } else {
                ParseData(
                    null,
                    parsePublication(json, url),
                    2
                )
            }
        }

        private fun isFeed(json: JsonObject) =
            "navigation" in json ||
                "groups" in json ||
                "publications" in json ||
                "facets" in json

        private fun parseFeed(topLevelDict: JsonObject, url: Url): Feed {
            val metadataDict = topLevelDict["metadata"] as? JsonObject
                ?: throw Exception(OPDS2ParserError.MetadataNotFound.name)
            // Like org.json's getString, any non-null value is coerced to a string.
            val title = ("title" in metadataDict)
                .takeIf { it }
                ?.let { metadataDict.optString("title") }
                ?: throw Exception(OPDS2ParserError.MissingTitle.name)
            val feed = Feed.Builder(title, 2, url)
            parseFeedMetadata(opdsMetadata = feed.metadata, metadataDict = metadataDict)
            when (val context = topLevelDict["@context"]) {
                is JsonObject -> feed.context.add(topLevelDict.optString("@context"))
                is JsonArray -> {
                    for (element in context) {
                        element.stringOrNull?.let { feed.context.add(it) }
                    }
                }
                else -> {}
            }

            if ("links" in topLevelDict) {
                val links = topLevelDict["links"] as? JsonArray
                    ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                parseLinks(feed, links)
            }

            if ("facets" in topLevelDict) {
                val facets = topLevelDict["facets"] as? JsonArray
                    ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                parseFacets(feed, facets)
            }
            if ("publications" in topLevelDict) {
                val publications = topLevelDict["publications"] as? JsonArray
                    ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                parsePublications(feed, publications)
            }
            if ("navigation" in topLevelDict) {
                val navigation = topLevelDict["navigation"] as? JsonArray
                    ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                parseNavigation(feed, navigation)
            }
            if ("groups" in topLevelDict) {
                val groups = topLevelDict["groups"] as? JsonArray
                    ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                parseGroups(feed, groups)
            }
            return feed.build()
        }

        private fun parseFeedMetadata(opdsMetadata: OpdsMetadata.Builder, metadataDict: JsonObject) {
            if ("title" in metadataDict) {
                opdsMetadata.title = metadataDict.optString("title")
            }
            if ("numberOfItems" in metadataDict) {
                opdsMetadata.numberOfItems = metadataDict.optString("numberOfItems").toInt()
            }
            if ("itemsPerPage" in metadataDict) {
                opdsMetadata.itemsPerPage = metadataDict.optString("itemsPerPage").toInt()
            }
            if ("modified" in metadataDict) {
                opdsMetadata.modified = metadataDict.optString("modified").toInstant()
            }
            if ("@type" in metadataDict) {
                opdsMetadata.rdfType = metadataDict.optString("@type")
            }
            if ("currentPage" in metadataDict) {
                opdsMetadata.currentPage = metadataDict.optString("currentPage").toInt()
            }
        }

        private fun parseFacets(feed: Feed.Builder, facets: JsonArray) {
            for (facetDict in facets.filterIsInstance<JsonObject>()) {
                val metadata = facetDict["metadata"] as? JsonObject
                    ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                val title = metadata["title"]?.stringOrNull
                    ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                val facet = Facet.Builder(title = title)
                parseFeedMetadata(opdsMetadata = facet.metadata, metadataDict = metadata)
                if ("links" in facetDict) {
                    val links = facetDict["links"] as? JsonArray
                        ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                    for (linkDict in links.filterIsInstance<JsonObject>()) {
                        parseLink(linkDict, feed.href)?.let {
                            facet.links.add(it)
                        }
                    }
                }
                feed.facets.add(facet)
            }
        }

        private fun parseLinks(feed: Feed.Builder, links: JsonArray) {
            for (linkDict in links.filterIsInstance<JsonObject>()) {
                parseLink(linkDict, feed.href)?.let {
                    feed.links.add(it)
                }
            }
        }

        private fun parsePublications(feed: Feed.Builder, publications: JsonArray) {
            for (pubDict in publications.filterIsInstance<JsonObject>()) {
                parsePublication(pubDict, feed.href)?.let {
                    feed.publications.add(it)
                }
            }
        }

        private fun parseNavigation(feed: Feed.Builder, navLinks: JsonArray) {
            for (navDict in navLinks.filterIsInstance<JsonObject>()) {
                parseLink(navDict, feed.href)?.let { link ->
                    feed.navigation.add(link)
                }
            }
        }

        private fun parseGroups(feed: Feed.Builder, groups: JsonArray) {
            for (groupDict in groups.filterIsInstance<JsonObject>()) {
                val metadata = groupDict["metadata"] as? JsonObject
                    ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                // Like org.json's getString, any non-null value is coerced to a string.
                val title = ("title" in metadata)
                    .takeIf { it }
                    ?.let { metadata.optString("title") }
                    ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                val group = Group.Builder(title = title)
                parseFeedMetadata(opdsMetadata = group.metadata, metadataDict = metadata)

                if ("links" in groupDict) {
                    val links = groupDict["links"] as? JsonArray
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (linkDict in links.filterIsInstance<JsonObject>()) {
                        parseLink(linkDict, feed.href)?.let { link ->
                            group.links.add(link)
                        }
                    }
                }
                if ("navigation" in groupDict) {
                    val links = groupDict["navigation"] as? JsonArray
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (linkDict in links.filterIsInstance<JsonObject>()) {
                        parseLink(linkDict, feed.href)?.let { link ->
                            group.navigation.add(link)
                        }
                    }
                }
                if ("publications" in groupDict) {
                    val publications = groupDict["publications"] as? JsonArray
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (pubDict in publications.filterIsInstance<JsonObject>()) {
                        parsePublication(pubDict, feed.href)?.let {
                            group.publications.add(it)
                        }
                    }
                }
                feed.groups.add(group)
            }
        }

        private fun parsePublication(json: JsonObject, baseUrl: Url): Publication? =
            Manifest.fromJSON(json)
                // Self link takes precedence over the given `baseUrl`.
                ?.let { it.normalizeHrefsToBase(it.linkWithRel("self")?.href?.resolve() ?: baseUrl) }
                ?.let { Publication(it) }

        private fun parseLink(json: JsonObject, baseUrl: Url): Link? =
            Link.fromJSON(json)
                ?.normalizeHrefsToBase(baseUrl)
    }
}
