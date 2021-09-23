/*
 * Module: r2-opds-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.opds

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.extensions.removeLastComponent
import org.readium.r2.shared.opds.*
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetchWithDecoder
import java.net.URL

enum class OPDS2ParserError {
    MetadataNotFound,
    InvalidLink,
    MissingTitle,
    InvalidFacet,
    InvalidGroup,
}

class OPDS2Parser {

    companion object {

        private lateinit var feed: Feed

        suspend fun parseUrlString(url: String, client: HttpClient = DefaultHttpClient()): Try<ParseData, Exception> {
            return client.fetchWithDecoder(HttpRequest(url)) {
                this.parse(it.body, URL(url))
            }
        }

        suspend fun parseRequest(request: HttpRequest, client: HttpClient = DefaultHttpClient()): Try<ParseData, Exception> {
            return client.fetchWithDecoder(request) {
                this.parse(it.body, URL(request.url))
            }
        }

        @Deprecated(
            "Use `parseRequest` or `parseUrlString` with coroutines instead",
            ReplaceWith("OPDS2Parser.parseUrlString(url)"),
            DeprecationLevel.WARNING
        )
        fun parseURL(url: URL): Promise<ParseData, Exception> {
            return DefaultHttpClient().fetchPromise(HttpRequest(url.toString())) then {
                this.parse(it.body, url)
            }
        }

        @Deprecated(
            "Use `parseRequest` or `parseUrlString` with coroutines instead",
            ReplaceWith("OPDS2Parser.parseUrlString(url)"),
            DeprecationLevel.WARNING
        )
        @Suppress("unused")
        fun parseURL(headers: MutableMap<String,String>, url: URL): Promise<ParseData, Exception> {
            return DefaultHttpClient().fetchPromise(HttpRequest(url = url.toString(), headers = headers)) then {
                this.parse(it.body, url)
            }
        }

        fun parse(jsonData: ByteArray, url: URL): ParseData {
            return if (isFeed(jsonData)) {
                ParseData(parseFeed(jsonData, url), null, 2)
            } else {
                ParseData(null, Manifest.fromJSON(JSONObject(String(jsonData)))?.let { Publication(it) }, 2)
            }
        }

        private fun isFeed(jsonData: ByteArray) =
                JSONObject(String(jsonData)).let {
                    (it.has("navigation") ||
                            it.has("groups") ||
                            it.has("publications") ||
                            it.has("facets"))
                }

        private fun parseFeed(jsonData: ByteArray, url: URL): Feed {
            val topLevelDict = JSONObject(String(jsonData))
            val metadataDict: JSONObject = topLevelDict.getJSONObject("metadata")
                    ?: throw Exception(OPDS2ParserError.MetadataNotFound.name)
            val title = metadataDict.getString("title")
                    ?: throw Exception(OPDS2ParserError.MissingTitle.name)
            feed = Feed(title, 2, url)
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
            return feed
        }

        private fun parseFeedMetadata(opdsMetadata: OpdsMetadata, metadataDict: JSONObject) {
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
                    opdsMetadata.modified = DateTime(it.toString()).toDate()
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

        private fun parseFacets(feed: Feed, facets: JSONArray) {
            for (i in 0 until facets.length()) {
                val facetDict = facets.getJSONObject(i)
                val metadata = facetDict.getJSONObject("metadata")
                        ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                val title = metadata["title"] as? String
                        ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                val facet = Facet(title = title)
                parseFeedMetadata(opdsMetadata = facet.metadata, metadataDict = metadata)
                if (facetDict.has("links")) {
                    val links = facetDict.getJSONArray("links")
                            ?: throw Exception(OPDS2ParserError.InvalidFacet.name)
                    for (k in 0 until links.length()) {
                        val linkDict = links.getJSONObject(k)
                        parseLink(feed, linkDict)?.let {
                            facet.links.add(it)
                        }
                    }
                }
                feed.facets.add(facet)
            }
        }

        private fun parseLinks(feed: Feed, links: JSONArray) {
            for (i in 0 until links.length()) {
                val linkDict = links.getJSONObject(i)
                parseLink(feed, linkDict)?.let {
                    feed.links.add(it)
                }
            }
        }

        private fun parsePublications(feed: Feed, publications: JSONArray) {
            for (i in 0 until publications.length()) {
                val pubDict = publications.getJSONObject(i)
                Manifest.fromJSON(pubDict)?.let { manifest ->
                    feed.publications.add(Publication(manifest))
                }
            }
        }

        private fun parseNavigation(feed: Feed, navLinks: JSONArray) {
            for (i in 0 until navLinks.length()) {
                val navDict = navLinks.getJSONObject(i)
                parseLink(feed, navDict)?.let { link ->
                    feed.navigation.add(link)
                }
            }
        }

        private fun parseGroups(feed: Feed, groups: JSONArray) {
            for (i in 0 until groups.length()) {
                val groupDict = groups.getJSONObject(i)
                val metadata = groupDict.getJSONObject("metadata")
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                val title = metadata.getString("title")
                        ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                val group = Group(title = title)
                parseFeedMetadata(opdsMetadata = group.metadata, metadataDict = metadata)

                if (groupDict.has("links")) {
                    val links = groupDict.getJSONArray("links")
                            ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (j in 0 until links.length()) {
                        val linkDict = links.getJSONObject(j)
                        parseLink(feed, linkDict)?.let { link ->
                            group.links.add(link)
                        }
                    }
                }
                if (groupDict.has("navigation")) {
                    val links = groupDict.getJSONArray("navigation")
                            ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (j in 0 until links.length()) {
                        val linkDict = links.getJSONObject(j)
                        parseLink(feed, linkDict)?.let { link ->
                            group.navigation.add(link)
                        }
                    }
                }
                if (groupDict.has("publications")) {
                    val publications = groupDict.getJSONArray("publications")
                            ?: throw Exception(OPDS2ParserError.InvalidGroup.name)
                    for (j in 0 until publications.length()) {
                        val pubDict = publications.getJSONObject(j)
                        Manifest.fromJSON(pubDict)?.let { manifest ->
                            group.publications.add(Publication(manifest))
                        }
                    }
                }
                feed.groups.add(group)
            }
        }

        private fun parseLink(feed: Feed, json: JSONObject): Link? {
            val baseUrl = feed.href.removeLastComponent()
            return Link.fromJSON(json, normalizeHref = { Href(it, baseUrl.toString()).string })
        }

    }
}


