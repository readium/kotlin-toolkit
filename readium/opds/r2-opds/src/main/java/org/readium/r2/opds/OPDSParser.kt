package org.readium.r2.opds

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.joda.time.DateTime
import org.readium.r2.shared.*
import org.readium.r2.shared.opds.*
import org.readium.r2.shared.XmlParser.Node
import org.readium.r2.shared.XmlParser.XmlParser
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

public class OPDSParser {
    companion object {

        public fun parseURL(url: URL) : Promise<Feed, Exception> {
            return task {

                //TODO incomplete
                Feed("")
            }
        }

        public fun parse(xmlData: ByteArray) : Feed {
            val document = XmlParser()
            document.parseXml(xmlData.inputStream())
            val root = document.root()
            val title = root.get("title") ?: throw Exception(OPDSParserError.missingTitle.name)
            val feed = Feed(title.toString())
            val tmpDate = root.get("updated")
            val date = DateTime(tmpDate).toDate()
            if (tmpDate != null && date != null) {
                feed.metadata.modified = date
            }
            val totalResults = root.get("TotalResults")
            if (totalResults != null) {
                feed.metadata.numberOfItem = totalResults.toString().toInt()
            }
            val itemsPerPage = root.get("ItemsPerPage")
            if (itemsPerPage != null) {
                feed.metadata.itemsPerPage = itemsPerPage.toString().toInt()
            }
            val entries = root.get("entry") ?: return feed
            for (entry in entries) {
                var isNavigation = true
                val collectionLink = Link()
                val links = entry.get("link")
                if (links != null) {
                    for (link in links) {
                        val rel = link.attributes["rel"]
                        if (rel != null) {
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
                        val facetElementCount = facetElementCountStr.toString().toInt()
                        if (facetElementCountStr != null && facetElementCount != null) {
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

        public fun fetchOpenSearchTemplate(feed: Feed) : Promise<String, Exception> {

            //TODO incomplete
            return task { "" }
        }

        internal fun parseEntry(entry: Node) : Publication {
            val publication = Publication()
            val metadata = Metadata()
            publication.metadata = metadata
            val entryTitle = entry.get("title")
            if (entryTitle != null) {
                if (metadata.multilangTitle == null) {
                    metadata.multilangTitle = MultilangString()
                }
                metadata.multilangTitle?.singleString = entryTitle.toString()
            }
            val identifier = entry.get("identifier")
            if (identifier != null) {
                metadata.identifier = identifier.toString()
            } else {
                val id = entry.get("id")
                if (id != null) {
                    metadata.identifier = id.toString()
                }
            }
            val languages = entry.get("dcterms:language")
            if (languages != null) {
                metadata.languages = languages.map({ it.toString() }).toMutableList()
            }
            val tmpDate = entry.get("updated")
            if (tmpDate != null) {
                val date = DateTime(tmpDate.toString()).toDate()
                if (date != null) {
                    metadata.modified = date.toString()
                }
            }
            val published = entry.get("published")
            if (published != null) {
                metadata.publicationDate = published.toString()
            }
            val rights = entry.get("rights")
            if (rights != null) {
                metadata.rights = rights.map({ it }).joinToString(" ")
            }
            val publisher = entry.get("dcterms:publisher")
            if (publisher != null) {
                val contributor = Contributor()
                contributor.multilangName.singleString = publisher.toString()
                metadata.publishers.add(contributor)
            }
            val categories = entry.get("category")
            if (categories != null) {
                for (category in categories) {
                    val subject = Subject()
                    subject.code = category.attributes["term"]
                    subject.name = category.attributes["label"]
                    subject.scheme = category.attributes["scheme"]
                    metadata.subjects.add(subject)
                }
            }
            val authors = entry.get("author")
            if (authors != null) {
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
            val content = entry.get("content")
            if (content != null) {
                metadata.description = content.toString()
            } else {
                val summary = entry.get("summary")
                if (summary != null) {
                    metadata.description = summary.toString()
                }
            }
            val links = entry.get("link")
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
                    val indirectAcquisitions = link.get("opds:indirectAcquisition")
                    if (indirectAcquisitions != null && !indirectAcquisitions.isEmpty()) {
                        newLink.properties.indirectAcquisition = parseIndirectAcquisition(indirectAcquisitions.toMutableList())
                    }
                    val price:Node = link.getFirst("opds:price")!!
                    val priceDouble = price.toString().toDouble()
                    val currency = price.attributes["currencyCode"]
                    if (price != null && priceDouble != null && currency != null) {
                        val newPrice = Price(currency = currency, value = priceDouble)
                        newLink.properties.price = newPrice
                    }
//                    val rel = link.attributes["rel"]
                    if (rel != null) {
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
