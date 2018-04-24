package org.readium.r2.opds

import com.github.kittinunf.fuel.Fuel
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import org.joda.time.DateTime
import org.readium.r2.shared.*
import org.readium.r2.shared.XmlParser.Node
import org.readium.r2.shared.XmlParser.XmlParser
import org.readium.r2.shared.opds.*
import java.net.URL

enum class OPDSParserError(v:String) {
    missingTitle("The title is missing from the feed."),
    documentNotFound("Document is not found")
}
enum class OPDSParserOpenSearchHelperError(v:String) {
    searchLinkNotFound("Search link not found in feed"),
    searchDocumentIsInvalid("OpenSearch document is invalid")
}

data class MimeTypeParameters(
        var type: String,
        var parameters:MutableMap<String, String> = mutableMapOf()
)

class OPDSParser {
    companion object {

        fun parseURL(url: URL) : Promise<Feed, Exception> {
            return Fuel.get(url.toString(),null).promise() then {
                val (request, response, result) = it
                this.parse(xmlData = result)
            }
        }

        fun parse(xmlData: ByteArray) : Feed {
            val document = XmlParser()
            document.parseXml(xmlData.inputStream())
            val root = document.root()
            val title = root.getFirst("title")?.text ?: throw Exception(OPDSParserError.missingTitle.name)
            val feed = Feed(title.toString())
            val tmpDate = root.getFirst("updated")?.text
            tmpDate?.let {
                val date = DateTime(it).toDate()
                feed.metadata.modified = date
            }

            val totalResults = root.getFirst("TotalResults")?.text
            totalResults?.let {
                feed.metadata.numberOfItems = totalResults.toString().toInt()
            }
            val itemsPerPage = root.getFirst("ItemsPerPage")?.text
            itemsPerPage?.let {
                feed.metadata.itemsPerPage = itemsPerPage.toString().toInt()
            }
            val entries = root.get("entry") ?: return feed
            for (entry in entries) {
                var isNavigation = true
                val collectionLink = Link()
                val links = entry.get("link")
                links?.let {
                    for (link in links) {
                        val rel = link.attributes["rel"]
                        rel?.let {
                            if (rel.contains("http://opds-spec.org/acquisition")) {
                                isNavigation = false
                            }
                            if (rel == "collection" || rel == "http://opds-spec.org/group") {
                                collectionLink.rel.add("collection")
                                collectionLink.href = link.attributes["href"]
                                collectionLink.title = link.attributes["title"]
                            }
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
                    val entryTitle = entry.get("title")
                    if (entryTitle != null) {
                        newLink.title = entryTitle.toString()
                    }
                    val link = entry.getFirst("link")
                    link?.let {
                        val rel = link.attributes["rel"]
                        if (rel != null) {
                            newLink.rel.add(rel)
                        }
                        val facetElementCountStr = link.attributes["thr:count"]
                        facetElementCountStr?.let {
                            val facetElementCount = it.toInt()
                            newLink.properties.numberOfItems = facetElementCount
                        }
                        newLink.typeLink = link.attributes["type"]
                        newLink.href = link.attributes["href"]
                        if (collectionLink.href != null) {
                            addNavigationInGroup(feed, newLink, collectionLink)
                        } else {
                            feed.navigation.add(newLink)
                        }
                    }

                }
            }
            val links = root.get("link")
            if (links != null) {
                for (link in links) {
                    val newLink = Link()
                    newLink.href = link.attributes["href"]
                    newLink.title = link.attributes["title"]
                    newLink.typeLink = link.attributes["type"]
                    val rel = link.attributes["rel"]
                    if (rel != null) {
                        newLink.rel.add(rel)
                    }
                    val facetGroupName = link.attributes["opds:facetGroup"]
                    if (facetGroupName != null && newLink.rel.contains("http://opds-spec.org/facet")) {
                        val facetElementCountStr = link.attributes["thr:count"]
                        val facetElementCount = facetElementCountStr.toString().toInt()
                        if (facetElementCountStr != null && facetElementCount != null) {
                            newLink.properties.numberOfItems = facetElementCount
                        }
                        addFacet(feed, newLink, facetGroupName)
                    } else {
                        feed.links.add(newLink)
                    }
                }
            }
            return feed
        }

        public fun parseEntry(xmlData: ByteArray) : Publication {
            val document = XmlParser()
            document.parseXml(xmlData.inputStream())
            val root = document.root()
            return parseEntry(entry = root)
        }

        fun parseMimeType(mimeTypeString: String) : MimeTypeParameters {
            val substrings = mimeTypeString.split(";")
            val type = substrings[0].replace("\\s".toRegex(), "")
            val params:MutableMap<String, String> = mutableMapOf()
            for (defn in substrings.drop(0)) {
                val halves = defn.split("=")
                val paramName = halves[0].replace("\\s".toRegex(), "")
                val paramValue = halves[1].replace("\\s".toRegex(), "")
                params[paramName] = paramValue
            }
            return MimeTypeParameters(type = type, parameters = params)
        }

        public fun fetchOpenSearchTemplate(feed: Feed) : Promise<String?, Exception> {

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

            return Fuel.get(unwrappedURL.toString(),null).promise() then {
                val (request, response, result) = it

                val document = XmlParser()
                document.parseXml(result.inputStream())

                val urls = document.root().get("Url")

                var typeAndProfileMatch: Node? = null
                var typeMatch: Node? = null

                selfMimeType?.let {

                    val selfMimeParams = parseMimeType(mimeTypeString = it)
                    urls?.let {
                        for (url in urls) {
                            val urlMimeType = url.attributes["type"]
                            if (urlMimeType == null) {
                                continue
                            }
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
                        val template = match.attributes["template"]

                        template
                    }
                }
                null
            }

        }

        internal fun parseEntry(entry: Node) : Publication {
            val publication = Publication()
            val metadata = Metadata()
            publication.metadata = metadata
            val entryTitle = entry.getFirst("title")
            entryTitle?.let {
                if (metadata.multilangTitle == null) {
                    metadata.multilangTitle = MultilangString()
                }
                metadata.multilangTitle?.singleString = entryTitle.text
            }
            var identifier = entry.getFirst("identifier")
            identifier?.let {
                metadata.identifier = it.text.toString()
            }?: run {
                identifier = entry.getFirst("id")
                identifier?.let {
                    metadata.identifier = it.text.toString()
                }
            }
            val languages = entry.get("dcterms:language")
            languages?.let {
                metadata.languages = languages.map({ it.text.toString() }).toMutableList()
            }
            val tmpDate = entry.getFirst("updated")
            tmpDate?.let {
                val date = DateTime(tmpDate.text).toDate()
                metadata.modified = date
            }
            val published = entry.getFirst("published")
            published?.let {
                metadata.publicationDate = published.text
            }
            val rights = entry.get("rights")
            rights?.let {
                metadata.rights = rights.map({ it.text }).joinToString(" ")
            }
            val publisher = entry.get("dcterms:publisher")
            publisher?.let {
                val contributor = Contributor()
                contributor.multilangName.singleString = publisher.toString()
                metadata.publishers.add(contributor)
            }
            val categories = entry.get("category")
            categories?.let {
                for (category in categories) {
                    val subject = Subject()
                    subject.code = category.attributes["term"]
                    subject.name = category.attributes["label"]
                    subject.scheme = category.attributes["scheme"]
                    metadata.subjects.add(subject)
                }
            }
            val authors = entry.get("author")
            authors?.let {
                for (author in authors) {
                    val contributor = Contributor()
                    val uri = author.get("uri")
                    if (uri != null) {
                        val link = Link()
                        link.href = uri.toString()
                        contributor.links.add(link)
                    }
                    contributor.multilangName.singleString = author.get("name").toString()
                    metadata.authors.add(contributor)
                }
            }
            val content = entry.getFirst("content")
            content?.let {
                metadata.description = content.text
            } ?: run {
                val summary = entry.getFirst("summary")
                summary?.let {
                    metadata.description = summary.text
                }
            }
            val links = entry.get("link")
            links?.let {
                for (link in links) {
                    val newLink = Link()
                    newLink.href = link.attributes["href"]
                    newLink.title = link.attributes["title"]
                    newLink.typeLink = link.attributes["type"]
                    val rel = link.attributes["rel"]
                    rel?.let {
                        newLink.rel.add(rel)
                    }
                    val indirectAcquisitions = link.get("opds:indirectAcquisition")
                    indirectAcquisitions?.let {
                        if (!indirectAcquisitions.isEmpty()) {
                            newLink.properties.indirectAcquisition = parseIndirectAcquisition(indirectAcquisitions.toMutableList())
                        }
                    }
                    val price = link.getFirst("opds:price")
                    var priceDouble:Double? = null
                    var currency:String? = null
                    price?.let {
                        priceDouble = price.toString().toDouble()
                        currency = price.attributes["currencyCode"]
                        val newPrice = Price(currency = currency!!, value = priceDouble!!)
                        newLink.properties.price = newPrice
                    }
                    rel?.let {
                        if (rel == "collection" || rel == "http://opds-spec.org/group") {} else if (rel == "http://opds-spec.org/image" || rel == "http://opds-spec.org/image-thumbnail") {
                            publication.images.add(newLink)
                        } else {
                            publication.links.add(newLink)
                        }
                    }
                }
            }
            return publication
        }

        internal fun addFacet(feed: Feed, link: Link, title: String) {
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

        internal fun addPublicationInGroup(feed: Feed, publication: Publication, collectionLink: Link) {
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

        internal fun addNavigationInGroup(feed: Feed, link: Link, collectionLink: Link) {
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

        internal fun parseIndirectAcquisition(children: MutableList<Node>) : MutableList<IndirectAcquisition> {
            val ret = mutableListOf<IndirectAcquisition>()
            for (child in children) {
                val typeAcquisition = child.attributes["type"]
                if (typeAcquisition != null) {
                    val newIndAcq = IndirectAcquisition(typeAcquisition = typeAcquisition)
                    val grandChildren = child.get("opds:indirectAcquisition")?.toMutableList()
                    if (grandChildren != null) {
                        newIndAcq.child = parseIndirectAcquisition(grandChildren)
                    }
                    ret.add(newIndAcq)
                }
            }
            return ret
        }
    }
}
