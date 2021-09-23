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
import org.readium.r2.shared.extensions.toList
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.opds.*
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetchWithDecoder
import java.net.URL

enum class OPDSParserError {
    MissingTitle
}

data class MimeTypeParameters(
        var type: String,
        var parameters: MutableMap<String, String> = mutableMapOf()
)

object Namespaces {
    const val Opds = "http://opds-spec.org/2010/catalog"
    const val Dc = "http://purl.org/dc/elements/1.1/"
    const val Dcterms = "http://purl.org/dc/terms/"
    const val Atom = "http://www.w3.org/2005/Atom"
    const val Search = "http://a9.com/-/spec/opensearch/1.1/"
    const val Thread = "http://purl.org/syndication/thread/1.0"
}

class OPDS1Parser {
    companion object {

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
            ReplaceWith("OPDS1Parser.parseUrlString(url)"),
            DeprecationLevel.WARNING
        )
        fun parseURL(url: URL): Promise<ParseData, Exception> {
            return DefaultHttpClient().fetchPromise(HttpRequest(url.toString())) then {
                this.parse(xmlData = it.body, url = url)
            }
        }

        @Deprecated(
            "Use `parseRequest` or `parseUrlString` with coroutines instead",
            ReplaceWith("OPDS1Parser.parseUrlString(url)"),
            DeprecationLevel.WARNING
        )
        @Suppress("unused")
        fun parseURL(headers: MutableMap<String,String>, url: URL): Promise<ParseData, Exception> {
            return DefaultHttpClient().fetchPromise(HttpRequest(url = url.toString(), headers = headers)) then {
                this.parse(xmlData = it.body, url = url)
            }
        }

        fun parse(xmlData: ByteArray, url: URL): ParseData {
            val root = XmlParser().parse(xmlData.inputStream())
            return if (root.name == "feed")
                ParseData(parseFeed(root, url), null, 1)
            else
                ParseData(null, parseEntry(root, url), 1)
        }

        private fun parseFeed(root: ElementNode, url: URL): Feed {
            val feedTitle = root.getFirst("title", Namespaces.Atom)?.text
                    ?: throw Exception(OPDSParserError.MissingTitle.name)
            val feed = Feed(feedTitle, 1, url)
            val tmpDate = root.getFirst("updated", Namespaces.Atom)?.text
            feed.metadata.modified = tmpDate?.let { DateTime(it).toDate() }

            val totalResults = root.getFirst("TotalResults", Namespaces.Search)?.text
            totalResults?.let {
                feed.metadata.numberOfItems = totalResults.toString().toInt()
            }
            val itemsPerPage = root.getFirst("ItemsPerPage", Namespaces.Search)?.text
            itemsPerPage?.let {
                feed.metadata.itemsPerPage = itemsPerPage.toString().toInt()
            }

            // Parse entries
            for (entry in root.get("entry", Namespaces.Atom)) {
                var isNavigation = true
                var collectionLink: Link? = null
                val links = entry.get("link", Namespaces.Atom)
                for (link in links) {
                    val href = link.getAttr("href")
                    val rel = link.getAttr("rel")
                    if (rel != null) {
                        if (rel.startsWith("http://opds-spec.org/acquisition")) {
                            isNavigation = false
                        }
                        if (href != null && (rel == "collection" || rel == "http://opds-spec.org/group")) {
                            collectionLink = Link(
                                href = Href(href, baseHref = feed.href.toString()).percentEncodedString,
                                title = link.getAttr("title"),
                                rels = setOf("collection")
                            )
                        }
                    }
                }
                if ((!isNavigation)) {
                    val publication = parseEntry(entry, baseUrl = url)
                    if (publication != null) {
                        collectionLink?.let {
                            addPublicationInGroup(feed, publication, it)
                        } ?: run {
                            feed.publications.add(publication)
                        }
                    }
                } else {
                    val link = entry.getFirst("link", Namespaces.Atom)
                    val href = link?.getAttr("href")
                    if (href != null) {
                        val otherProperties = mutableMapOf<String, Any>()
                        val facetElementCount = link.getAttrNs("count", Namespaces.Thread)?.toInt()
                        if (facetElementCount != null) {
                            otherProperties["numberOfItems"] = facetElementCount
                        }

                        val newLink = Link(
                            href = Href(href, baseHref = feed.href.toString()).percentEncodedString,
                            type = link.getAttr("type"),
                            title = entry.getFirst("title", Namespaces.Atom)?.text,
                            rels = listOfNotNull(link.getAttr("rel")).toSet(),
                            properties = Properties(otherProperties = otherProperties)
                        )

                        collectionLink?.let {
                            addNavigationInGroup(feed, newLink, it)
                        } ?: run {
                            feed.navigation.add(newLink)
                        }
                    }

                }
            }
            // Parse links
            for (link in root.get("link", Namespaces.Atom)) {
                val hrefAttr = link.getAttr("href") ?: continue
                val href = Href(hrefAttr, baseHref = feed.href.toString()).percentEncodedString
                val title = link.getAttr("title")
                val type = link.getAttr("type")
                val rels = listOfNotNull(link.getAttr("rel")).toSet()

                val facetGroupName = link.getAttrNs("facetGroup", Namespaces.Opds)
                if (facetGroupName != null && rels.contains("http://opds-spec.org/facet")) {
                    val otherProperties = mutableMapOf<String, Any>()
                    val facetElementCount = link.getAttrNs("count", Namespaces.Thread)?.toInt()
                    if (facetElementCount != null) {
                        otherProperties["numberOfItems"] = facetElementCount
                    }
                    val newLink = Link(href = href, type = type, title = title, rels = rels, properties = Properties(otherProperties = otherProperties))
                    addFacet(feed, newLink, facetGroupName)
                } else {
                    feed.links.add(Link(href = href, type = type, title = title, rels = rels))
                }
            }
            return feed
        }

        private fun parseMimeType(mimeTypeString: String): MimeTypeParameters {
            val substrings = mimeTypeString.split(";")
            val type = substrings[0].replace("\\s".toRegex(), "")
            val params: MutableMap<String, String> = mutableMapOf()
            for (defn in substrings.drop(0)) {
                val halves = defn.split("=")
                val paramName = halves[0].replace("\\s".toRegex(), "")
                val paramValue = halves[1].replace("\\s".toRegex(), "")
                params[paramName] = paramValue
            }
            return MimeTypeParameters(type = type, parameters = params)
        }

        @Suppress("unused")
        suspend fun retrieveOpenSearchTemplate(feed: Feed): Try<String?, Exception> {

            var openSearchURL: URL? = null
            var selfMimeType: String? = null

            for (link in feed.links) {
                if (link.rels.contains("self")) {
                    if (link.type != null) {
                        selfMimeType = link.type
                    }
                } else if (link.rels.contains("search")) {
                    openSearchURL = URL(link.href)
                }
            }

            val unwrappedURL = openSearchURL?.let {
                return@let it
            }

            return DefaultHttpClient().fetchWithDecoder(HttpRequest(unwrappedURL.toString())) {

                val document = XmlParser().parse(it.body.inputStream())

                val urls = document.get("Url", Namespaces.Search)

                var typeAndProfileMatch: ElementNode? = null
                var typeMatch: ElementNode? = null

                selfMimeType?.let { s ->

                    val selfMimeParams = parseMimeType(mimeTypeString = s)
                    for (url in urls) {
                        val urlMimeType = url.getAttr("type") ?: continue
                        val otherMimeParams = parseMimeType(mimeTypeString = urlMimeType)
                        if (selfMimeParams.type == otherMimeParams.type) {
                            if (typeMatch == null) {
                                typeMatch = url
                            }
                            if (selfMimeParams.parameters["profile"] == otherMimeParams.parameters["profile"]) {
                                typeAndProfileMatch = url
                                break
                            }
                        }
                    }
                    val match = typeAndProfileMatch ?: (typeMatch ?: urls[0])
                    val template = match.getAttr("template")

                    template

                }
                null
            }

        }

        @Deprecated(
            "Use `retrieveOpenSearchTemplate` with coroutines instead",
            ReplaceWith("OPDS1Parser.retrieveOpenSearchTemplate(feed)"),
            DeprecationLevel.WARNING
        )
        @Suppress("unused")
        fun fetchOpenSearchTemplate(feed: Feed): Promise<String?, Exception> {

            var openSearchURL: URL? = null
            var selfMimeType: String? = null

            for (link in feed.links) {
                if (link.rels.contains("self")) {
                    if (link.type != null) {
                        selfMimeType = link.type
                    }
                } else if (link.rels.contains("search")) {
                    openSearchURL = URL(link.href)
                }
            }

            val unwrappedURL = openSearchURL?.let {
                return@let it
            }

            return DefaultHttpClient().fetchPromise(HttpRequest(unwrappedURL.toString())) then {

                val document = XmlParser().parse(it.body.inputStream())

                val urls = document.get("Url", Namespaces.Search)

                var typeAndProfileMatch: ElementNode? = null
                var typeMatch: ElementNode? = null

                selfMimeType?.let { s ->

                    val selfMimeParams = parseMimeType(mimeTypeString = s)
                    for (url in urls) {
                        val urlMimeType = url.getAttr("type") ?: continue
                        val otherMimeParams = parseMimeType(mimeTypeString = urlMimeType)
                        if (selfMimeParams.type == otherMimeParams.type) {
                            if (typeMatch == null) {
                                typeMatch = url
                            }
                            if (selfMimeParams.parameters["profile"] == otherMimeParams.parameters["profile"]) {
                                typeAndProfileMatch = url
                                break
                            }
                        }
                    }
                    val match = typeAndProfileMatch ?: (typeMatch ?: urls[0])
                    val template = match.getAttr("template")

                    template

                }
                null
            }

        }

        private fun parseEntry(entry: ElementNode, baseUrl: URL): Publication? {
            // A title is mandatory
            val title = entry.getFirst("title", Namespaces.Atom)?.text
                ?: return null

            var links = entry.get("link", Namespaces.Atom)
                .mapNotNull { element ->
                    val href = element.getAttr("href")
                    val rel = element.getAttr("rel")
                    if (href == null || rel == "collection" || rel == "http://opds-spec.org/group") {
                        return@mapNotNull null
                    }

                    val properties = mutableMapOf<String, Any>()
                    val acquisitions = Acquisition.fromXML(element)
                    if (acquisitions.isNotEmpty()) {
                        properties["indirectAcquisition"] = acquisitions.toJSON().toList()
                    }

                    element.getFirst("price", Namespaces.Opds)?.let {
                        val value = it.text?.toDouble()
                        val currency = it.getAttr("currencyCode")
                            ?: it.getAttr("currencycode")
                        if (value != null && currency != null) {
                            properties["price"] = Price(currency = currency, value = value)
                                .toJSON().toMap()
                        }

                    }

                    Link(
                        href = Href(href, baseHref = baseUrl.toString()).percentEncodedString,
                        type = element.getAttr("type"),
                        title = element.getAttr("title"),
                        rels = listOfNotNull(rel).toSet(),
                        properties = Properties(otherProperties = properties)
                    )
                }

            val images = links.filter {
                it.rels.contains("http://opds-spec.org/image") || it.rels.contains("http://opds-spec.org/image-thumbnail")
            }

            links = links - images

            val manifest = Manifest(
                metadata = Metadata(
                    identifier = entry.getFirst("identifier", Namespaces.Dc)?.text
                        ?: entry.getFirst("id", Namespaces.Atom)?.text,

                    localizedTitle = LocalizedString(title),

                    modified = entry.getFirst("updated", Namespaces.Atom)
                        ?.let { DateTime(it.text).toDate() },

                    published = entry.getFirst("published", Namespaces.Atom)
                        ?. let { DateTime(it.text).toDate() },

                    languages = entry.get("language", Namespaces.Dcterms)
                        .mapNotNull { it.text },

                    publishers = entry.get("publisher", Namespaces.Dcterms)
                        .mapNotNull {
                            it.text?.let { name -> Contributor(localizedName = LocalizedString(name)) }
                        },

                    subjects = entry.get("category", Namespaces.Atom)
                        .mapNotNull { element ->
                            element.getAttr("label")?.let { name ->
                                Subject(
                                    localizedName = LocalizedString(name),
                                    scheme = element.getAttr("scheme"),
                                    code = element.getAttr("term")
                                )
                            }
                        },

                    authors = entry.get("author", Namespaces.Atom)
                        .mapNotNull { element ->
                            element.getFirst("name", Namespaces.Atom)?.text?.let { name ->
                                Contributor(
                                    localizedName = LocalizedString(name),
                                    links = listOfNotNull(
                                        element.getFirst("uri", Namespaces.Atom)?.text
                                            ?.let { Link(href = it) }
                                    )
                                )
                            }
                        },

                    description = entry.getFirst("content", Namespaces.Atom)?.text
                        ?: entry.getFirst("summary", Namespaces.Atom)?.text
                ),

                links = links,
                subcollections = mapOf(
                    "images" to listOfNotNull(
                        images.takeIf { it.isNotEmpty() }
                        ?.let { PublicationCollection(links = it) }
                    )
                ).filterValues { it.isNotEmpty() }
            )
            return Publication(manifest)
        }

        private fun addFacet(feed: Feed, link: Link, title: String) {
            for (facet in feed.facets) {
                if (facet.metadata.title == title) {
                    facet.links.add(link)
                    return
                }
            }
            val newFacet = Facet(title = title)
            newFacet.links.add(link)
            feed.facets.add(newFacet)
        }

        private fun addPublicationInGroup(feed: Feed, publication: Publication, collectionLink: Link) {
            for (group in feed.groups) {
                for (l in group.links) {
                    if (l.href == collectionLink.href) {
                        group.publications.add(publication)
                        return
                    }
                }
            }
            val title = collectionLink.title
            if (title != null) {
                val selfLink = collectionLink.copy(
                    rels = collectionLink.rels + "self"
                )
                val newGroup = Group(title = title)
                newGroup.links.add(selfLink)
                newGroup.publications.add(publication)
                feed.groups.add(newGroup)
            }
        }

        private fun addNavigationInGroup(feed: Feed, link: Link, collectionLink: Link) {
            for (group in feed.groups) {
                for (l in group.links) {
                    if (l.href == collectionLink.href) {
                        group.navigation.add(link)
                        return
                    }
                }
            }
            val title = collectionLink.title
            if (title != null) {
                val selfLink = collectionLink.copy(
                    rels = collectionLink.rels + "self"
                )
                val newGroup = Group(title = title)
                newGroup.links.add(selfLink)
                newGroup.navigation.add(link)
                feed.groups.add(newGroup)
            }
        }

        private fun Acquisition.Companion.fromXML(element: ElementNode): List<Acquisition> =
            element.get("indirectAcquisition", Namespaces.Opds).mapNotNull { child ->
                val type = child.getAttr("type")
                    ?: return@mapNotNull null

                Acquisition(
                    type = type,
                    children = fromXML(child)
                )
            }

    }

}
