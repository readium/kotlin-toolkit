/*
 * Module: r2-opds-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.opds

import com.github.kittinunf.fuel.Fuel
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import org.joda.time.DateTime
import org.readium.r2.shared.*
import org.readium.r2.shared.opds.*
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import java.net.URL

enum class OPDSParserError(var v: String) {
    MissingTitle("The title is missing from the feed."),
    DocumentNotFound("Document is not found")
}

enum class OPDSParserOpenSearchHelperError(var v: String) {
    SearchLinkNotFound("Search link not found in feed"),
    SearchDocumentIsInvalid("OpenSearch document is invalid")
}

data class MimeTypeParameters(
        var type: String,
        var parameters: MutableMap<String, String> = mutableMapOf()
)

object Namespaces {
    val Opds = "http://opds-spec.org/2010/catalog"
    val Dc = "http://purl.org/dc/elements/1.1/"
    val Dcterms = "http://purl.org/dc/terms/"
    val Atom = "http://www.w3.org/2005/Atom"
    val Search = "http://a9.com/-/spec/opensearch/1.1/"
    val Thread = "http://purl.org/syndication/thread/1.0"
}

class OPDS1Parser {
    companion object {

        private lateinit var feed: Feed

        fun parseURL(url: URL): Promise<ParseData, Exception> {
            return Fuel.get(url.toString(), null).promise() then {
                val (_, _, result) = it
                this.parse(xmlData = result, url = url)
            }
        }

        fun parseURL(headers: MutableMap<String,String>,url: URL): Promise<ParseData, Exception> {
            return Fuel.get(url.toString(), null).header(headers).promise() then {
                val (_, _, result) = it
                this.parse(xmlData = result, url = url)
            }
        }

        fun parse(xmlData: ByteArray, url: URL): ParseData {
            val root = XmlParser().parse(xmlData.inputStream())
            return if (root.name == "feed")
                ParseData(parseFeed(xmlData, url), null, 1)
            else
                ParseData(null, parseEntry(xmlData), 1)
        }

        private fun parseFeed(xmlData: ByteArray, url: URL): Feed {
            val root = XmlParser().parse(xmlData.inputStream())
            val title = root.getFirst("title", Namespaces.Atom)?.text
                    ?: throw Exception(OPDSParserError.MissingTitle.name)
            feed = Feed(title, 1, url)
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
                val collectionLink = Link()
                val links = entry.get("link", Namespaces.Opds)
                for (link in links) {
                    val rel = link.getAttr("rel")
                    rel?.let {
                        if (rel.contains("http://opds-spec.org/acquisition")) {
                            isNavigation = false
                        }
                        if (rel == "collection" || rel == "http://opds-spec.org/group") {
                            collectionLink.rel.add("collection")
                            collectionLink.href = getAbsolute(link.getAttr("href")!!, feed.href.toString())
                            collectionLink.title = link.getAttr("title")
                        }
                    }
                }
                if ((!isNavigation)) {
                    val publication = parseEntry(entry)
                    if (collectionLink.href != null) {
                        addPublicationInGroup(feed, publication, collectionLink)
                    } else {
                        feed.publications.add(publication)
                    }
                } else {
                    val newLink = Link()
                    val entryTitle = entry.getFirst("title", Namespaces.Atom)
                    entryTitle?.let {
                        newLink.title = entryTitle.text
                    }

                    val link = entry.getFirst("link", Namespaces.Atom)
                    link?.let {
                        val rel = link.getAttr("rel")
                        if (rel != null) {
                            newLink.rel.add(rel)
                        }
                        val facetElementCountStr = link.getAttrNs("count", Namespaces.Thread)
                        facetElementCountStr?.let { s ->
                            val facetElementCount = s.toInt()
                            newLink.properties.numberOfItems = facetElementCount
                        }
                        newLink.typeLink = link.getAttr("type")
                        newLink.href = getAbsolute(link.getAttr("href")!!, feed.href.toString())

                        if (collectionLink.href != null) {
                            addNavigationInGroup(feed, newLink, collectionLink)
                        } else {
                            feed.navigation.add(newLink)
                        }
                    }

                }
            }
            // Parse links
            for (link in root.get("link", Namespaces.Atom)) {
                val newLink = Link()
                newLink.href = getAbsolute(link.getAttr("href")!!, feed.href.toString())
                newLink.title = link.getAttr("title")
                newLink.typeLink = link.getAttr("type")
                val rel = link.getAttr("rel")
                if (rel != null) {
                    newLink.rel.add(rel)
                }
                val facetGroupName = link.getAttrNs("facetGroup", Namespaces.Opds)
                if (facetGroupName != null && newLink.rel.contains("http://opds-spec.org/facet")) {
                    val facetElementCountStr = link.getAttrNs("count", Namespaces.Thread)
                    facetElementCountStr?.let { s ->
                        val facetElementCount = s.toInt()
                        newLink.properties.numberOfItems = facetElementCount
                    }
                    addFacet(feed, newLink, facetGroupName)
                } else {
                    feed.links.add(newLink)
                }
            }
            return feed
        }

        private fun parseEntry(xmlData: ByteArray): Publication {
            val document = XmlParser().parse(xmlData.inputStream())
            return parseEntry(entry = document)
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

        fun fetchOpenSearchTemplate(feed: Feed): Promise<String?, Exception> {

            var openSearchURL: URL? = null
            var selfMimeType: String? = null

            for (link in feed.links) {
                if (link.rel[0] == "self") {
                    link.typeLink?.let {
                        selfMimeType = it
                    }
                } else if (link.rel[0] == "search") {
                    link.href?.let {
                        openSearchURL = URL(it)
                    }
                }
            }
            val unwrappedURL = openSearchURL?.let {
                return@let it
            }

            return Fuel.get(unwrappedURL.toString(), null).promise() then {
                val (_, _, result) = it

                val document = XmlParser().parse(result.inputStream())

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

        private fun parseEntry(entry: ElementNode): Publication {
            val publication = Publication()
            val metadata = Metadata()
            publication.metadata = metadata
            val entryTitle = entry.getFirst("title", Namespaces.Atom)
            entryTitle?.let {
                if (metadata.multilanguageTitle == null) {
                    metadata.multilanguageTitle = MultilanguageString()
                }
                metadata.multilanguageTitle?.singleString = entryTitle.text
            }
            var identifier = entry.getFirst("identifier", Namespaces.Dc)
            identifier?.let {
                metadata.identifier = it.text.toString()
            } ?: run {
                identifier = entry.getFirst("id", Namespaces.Atom)
                identifier?.let {
                    metadata.identifier = it.text.toString()
                }
            }
            val languages = entry.get("language", Namespaces.Dcterms)
            metadata.languages = languages.map { it.text.toString() }.toMutableList()
            val tmpDate = entry.getFirst("updated", Namespaces.Atom)
            tmpDate?.let {
                val date = DateTime(tmpDate.text).toDate()
                metadata.modified = date
            }
            val published = entry.getFirst("published", Namespaces.Atom)
            published?.let {
                metadata.publicationDate = published.text
            }
            val rights = entry.get("rights", Namespaces.Atom)
            metadata.rights = rights.map { it.text }.joinToString(" ")
            val publishers = entry.get("publisher", Namespaces.Dcterms)
            for(publisher in publishers) {
                val contributor = Contributor()
                contributor.multilanguageName.singleString = publisher.text
                metadata.publishers.add(contributor)
            }
            val categories = entry.get("category", Namespaces.Atom)
            for (category in categories) {
                val subject = Subject()
                subject.code = category.getAttr("term")
                subject.name = category.getAttr("label")
                subject.scheme = category.getAttr("scheme")
                metadata.subjects.add(subject)
            }
            val authors = entry.get("author", Namespaces.Atom)
            for (author in authors) {
                val contributor = Contributor()
                val uri = author.getFirst("uri", Namespaces.Atom)
                if (uri != null) {
                    val link = Link()
                    link.href = uri.toString()
                    contributor.links.add(link)
                }
                contributor.multilanguageName.singleString = author.getFirst("name", Namespaces.Atom)?.text
                metadata.authors.add(contributor)
            }
            val content = entry.getFirst("content", Namespaces.Atom)
            content?.let {
                metadata.description = content.text
            } ?: run {
                val summary = entry.getFirst("summary", Namespaces.Atom)
                summary?.let {
                    metadata.description = summary.text
                }
            }
            val links = entry.get("link", Namespaces.Atom)

            for (link in links) {
                val newLink = Link()
                newLink.href = getAbsolute(link.getAttr("href")!!, feed.href.toString())
                newLink.title = link.getAttr("title")
                newLink.typeLink = link.getAttr("type")
                val rel = link.getAttr("rel")
                rel?.let {
                    newLink.rel.add(rel)
                }
                val indirectAcquisitions = link.get("indirectAcquisition", Namespaces.Opds)
                if (indirectAcquisitions.isNotEmpty()) {
                    newLink.properties.indirectAcquisition = parseIndirectAcquisition(indirectAcquisitions.toMutableList())
                }
                val price = link.getFirst("price", Namespaces.Opds)
                var priceDouble: Double?
                var currency: String?
                price?.let {
                    priceDouble = price.text.toString().toDouble()
                    currency = price.getAttr("currencyCode")
                    if (currency == null) {
                        currency = price.getAttr("currencycode")
                    }
                    val newPrice = Price(currency = currency!!, value = priceDouble!!)
                    newLink.properties.price = newPrice
                }
                rel?.let {
                    if (rel != "collection" && rel != "http://opds-spec.org/group") {
                        if (rel == "http://opds-spec.org/image" || rel == "http://opds-spec.org/image-thumbnail") {
                            publication.images.add(newLink)
                        } else {
                            publication.links.add(newLink)
                        }
                    }
                }
            }
            return publication
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
                val newGroup = Group(title = title)
                val selfLink = Link()
                selfLink.href = collectionLink.href
                selfLink.title = collectionLink.title
                selfLink.rel.add("self")
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
                val newGroup = Group(title = title)
                val selfLink = Link()
                selfLink.href = collectionLink.href
                selfLink.title = collectionLink.title
                selfLink.rel.add("self")
                newGroup.links.add(selfLink)
                newGroup.navigation.add(link)
                feed.groups.add(newGroup)
            }
        }

        private fun parseIndirectAcquisition(children: MutableList<ElementNode>): MutableList<IndirectAcquisition> {
            val ret = mutableListOf<IndirectAcquisition>()
            for (child in children) {
                val typeAcquisition = child.getAttr("type")
                if (typeAcquisition != null) {
                    val newIndAcq = IndirectAcquisition(typeAcquisition = typeAcquisition)
                    val grandChildren = child.get("indirectAcquisition", Namespaces.Opds).toMutableList()
                    if (grandChildren.isNotEmpty()) {
                        newIndAcq.child = parseIndirectAcquisition(grandChildren)
                    }
                    ret.add(newIndAcq)
                }
            }
            return ret
        }
    }
}
