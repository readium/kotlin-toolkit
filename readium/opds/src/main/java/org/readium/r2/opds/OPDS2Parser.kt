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

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
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
import org.readium.r2.shared.util.Instant
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetchWithDecoder

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
            return if (isFeed(jsonData)) {
                ParseData(parseFeed(jsonData, url), null, 2)
            } else {
                ParseData(
                    null,
                    parsePublication(
                        JSONObject(String(jsonData)),
                        url
                    ),
                    2
                )
            }
        }

        private fun isFeed(jsonData: ByteArray) =
            JSONObject(String(jsonData)).let {
                (
                    it.has("navigation") ||
                        it.has("groups") ||
                        it.has("publications") ||
                        it.has("facets")
                    )
            }

        private fun parseFeed(jsonData: ByteArray, url: Url): Feed {
            val topLevelDict = JSONObject(String(jsonData))
            val metadataDict: JSONObject = topLevelDict.getJSONObject("metadata")
                ?: throw Exception(OPDS2ParserError.MetadataNotFound.name)
            val title = metadataDict.getString("title")
                ?: throw Exception(OPDS2ParserError.MissingTitle.name)
            val feed = Feed.Builder(title, 2, url)
            parseFeedMetadata(opdsMetadata = feed.metadata, metadataDict = metadataDict)
            if (topLevelDict.has("@context")) {
                if (topLevelDict.get("@context") is JSONObject) {
                    feed.context.add(topLevelDict.getString("@context"))
                } else if (topLevelDict.get("@context") is JSONArray) {
                    val array = topLevelDict.getJSONArray("@context")
                    for (i in 0 until array.length()) {
                        val string = array.getString(i)
                        feed.context.add(string)
                    }
                }
            }

            if (topLevelDict.has("links")) {
                topLevelDict.get("links").let {
                    val links = it as? JSONArray
                        ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                    parseLinks(feed, links)
                }
            }

            if (topLevelDict.has("facets")) {
                topLevelDict.get("facets").let {
                    val facets = it as? JSONArray
                        ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                    parseFacets(feed, facets)
                }
            }
            if (topLevelDict.has("publications")) {
                topLevelDict.get("publications").let {
                    val publications = it as? JSONArray
                        ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                    parsePublications(feed, publications)
                }
            }
            if (topLevelDict.has("navigation")) {
                topLevelDict.get("navigation").let {
                    val navigation = it as? JSONArray
                        ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                    parseNavigation(feed, navigation)
                }
            }
            if (topLevelDict.has("groups")) {
                topLevelDict.get("groups").let {
                    val groups = it as? JSONArray
                        ?: throw Exception(OPDS2ParserError.InvalidLink.name)
                    parseGroups(feed, groups)
                }
            }
            return feed.build()
        }

        private fun parseFeedMetadata(opdsMetadata: OpdsMetadata.Builder, metadataDict: JSONObject) {
            if (metadataDict.has("title")) {
                metadataDict.get("title").let {
                    opdsMetadata.title = it.toString()
                }
            }
            if (metadataDict.has("numberOfItems")) {
                metadataDict.get("numberOfItems").let {
                    opdsMetadata.numberOfItems = it.toString().toInt()
                }
            }
            if (metadataDict.has("itemsPerPage")) {
                metadataDict.get("itemsPerPage").let {
                    opdsMetadata.itemsPerPage = it.toString().toInt()
                }
            }
            if (metadataDict.has("modified")) {
                metadataDict.get("modified").let {
                    opdsMetadata.modified = Instant.parse(it.toString())
                }
            }
            if (metadataDict.has("@type")) {
                metadataDict.get("@type").let {
                    opdsMetadata.rdfType = it.toString()
                }
            }
            if (metadataDict.has("currentPage")) {
                metadataDict.get("currentPage").let {
                    opdsMetadata.currentPage = it.toString().toInt()
                }
            }
        }

        private fun parseFacets(feed: Feed.Builder, facets: JSONArray) {
            for (i in 0 until facets.length()) {
                val facetDict = facets.getJSONObject(i)
                val metadata = facetDict.getJSONObject("metadata")
                    ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                val title = metadata["title"] as? String
                    ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                val facet = Facet.Builder(title = title)
                parseFeedMetadata(opdsMetadata = facet.metadata, metadataDict = metadata)
                if (facetDict.has("links")) {
                    val links = facetDict.getJSONArray("links")
                        ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                    for (k in 0 until links.length()) {
                        val linkDict = links.getJSONObject(k)
                        parseLink(linkDict, feed.href)?.let {
                            facet.links.add(it)
                        }
                    }
                }
                feed.facets.add(facet)
            }
        }

        private fun parseLinks(feed: Feed.Builder, links: JSONArray) {
            for (i in 0 until links.length()) {
                val linkDict = links.getJSONObject(i)
                parseLink(linkDict, feed.href)?.let {
                    feed.links.add(it)
                }
            }
        }

        private fun parsePublications(feed: Feed.Builder, publications: JSONArray) {
            for (i in 0 until publications.length()) {
                val pubDict = publications.getJSONObject(i)
                parsePublication(pubDict, feed.href)?.let {
                    feed.publications.add(it)
                }
            }
        }

        private fun parseNavigation(feed: Feed.Builder, navLinks: JSONArray) {
            for (i in 0 until navLinks.length()) {
                val navDict = navLinks.getJSONObject(i)
                parseLink(navDict, feed.href)?.let { link ->
                    feed.navigation.add(link)
                }
            }
        }

        private fun parseGroups(feed: Feed.Builder, groups: JSONArray) {
            for (i in 0 until groups.length()) {
                val groupDict = groups.getJSONObject(i)
                val metadata = groupDict.getJSONObject("metadata")
                    ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                val title = metadata.getString("title")
                    ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                val group = Group.Builder(title = title)
                parseFeedMetadata(opdsMetadata = group.metadata, metadataDict = metadata)

                if (groupDict.has("links")) {
                    val links = groupDict.getJSONArray("links")
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (j in 0 until links.length()) {
                        val linkDict = links.getJSONObject(j)
                        parseLink(linkDict, feed.href)?.let { link ->
                            group.links.add(link)
                        }
                    }
                }
                if (groupDict.has("navigation")) {
                    val links = groupDict.getJSONArray("navigation")
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (j in 0 until links.length()) {
                        val linkDict = links.getJSONObject(j)
                        parseLink(linkDict, feed.href)?.let { link ->
                            group.navigation.add(link)
                        }
                    }
                }
                if (groupDict.has("publications")) {
                    val publications = groupDict.getJSONArray("publications")
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (j in 0 until publications.length()) {
                        val pubDict = publications.getJSONObject(j)
                        parsePublication(pubDict, feed.href)?.let {
                            group.publications.add(it)
                        }
                    }
                }
                feed.groups.add(group)
            }
        }

        private fun parsePublication(json: JSONObject, baseUrl: Url): Publication? =
            Manifest.fromJSON(json)
                // Self link takes precedence over the given `baseUrl`.
                ?.let { it.normalizeHrefsToBase(it.linkWithRel("self")?.href?.resolve() ?: baseUrl) }
                ?.let { Publication(it) }

        private fun parseLink(json: JSONObject, baseUrl: Url): Link? =
            Link.fromJSON(json)
                ?.normalizeHrefsToBase(baseUrl)
    }
}
