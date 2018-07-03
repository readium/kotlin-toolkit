package org.readium.r2.opds

import com.github.kittinunf.fuel.Fuel
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.*
import org.readium.r2.shared.Collection
import org.readium.r2.shared.metadata.BelongsTo
import org.readium.r2.shared.opds.*
import java.net.URL

enum class OPDS2ParserError(v:String) {
    invalidJSON("OPDS 2 manifest is not valid JSON"),
    metadataNotFound("Metadata not found"),
    invalidMetadata("Invalid metadata"),
    invalidLink("Invalid link"),
    invalidIndirectAcquisition("Invalid indirect acquisition"),
    missingTitle("Missing title"),
    invalidFacet("Invalid facet"),
    invalidGroup("Invalid group"),
    invalidPublication("Invalid publication"),
    invalidContributor("Invalid contributor"),
    invalidCollection("Invalid collection"),
    invalidNavigation("Invalid navigation")
}

class OPDS2Parser {

    companion object {

        var feedUrl:URL? = null
        fun parseURL(url: URL) : Promise<ParseData, Exception> {
            return Fuel.get(url.toString(),null).promise() then {
                val (request, response, result) = it
                this.parse(result, url)
            }
        }

        fun parse(jsonData: ByteArray, url: URL) : ParseData {
            return if (isFeed(jsonData)) {
                ParseData(parseFeed(jsonData, url), null, 2)
            } else {
                ParseData(null, parsePublication(JSONObject(String(jsonData)), feedUrl), 2)
            }
        }

        fun isFeed(jsonData: ByteArray) =
                JSONObject(String(jsonData)).let {
                    (it.has("navigation")  ||
                        it.has("groups")  ||
                        it.has("publications")  ||
                        it.has("facets") )
            }

        fun parseFeed(jsonData: ByteArray, url: URL) : Feed {
            feedUrl = url
            val topLevelDict = JSONObject(String(jsonData))
            val metadataDict:JSONObject = topLevelDict.getJSONObject("metadata") ?: throw Exception(OPDS2ParserError.metadataNotFound.name)
            val title = metadataDict.getString("title") ?: throw Exception(OPDS2ParserError.missingTitle.name)
            val feed = Feed(title, 2)
            parseMetadata(opdsMetadata = feed.metadata, metadataDict = metadataDict)
            if(topLevelDict.has("@context")) {
                if (topLevelDict.get("@context") is JSONObject){
                    feed.context.add(topLevelDict.getString("@context"))
                } else if (topLevelDict.get("@context") is JSONArray) {
                    val array = topLevelDict.getJSONArray("@context")
                    for (i in 0..(array.length() - 1)) {
                        val string = array.getString(i)
                        feed.context.add(string)
                    }
                }
            }

            if(topLevelDict.has("links")) {
                topLevelDict.get("links")?.let {
                    val links = it as? JSONArray ?: throw Exception(OPDS2ParserError.invalidLink.name)
                    parseLinks(feed, links)
                }
            }

            if(topLevelDict.has("facets")) {
                topLevelDict.get("facets")?.let {
                    val facets = it as? JSONArray ?: throw Exception(OPDS2ParserError.invalidLink.name)
                    parseFacets(feed, facets)
                }
            }
            if(topLevelDict.has("publications")) {
                topLevelDict.get("publications")?.let {
                    val publications = it as? JSONArray ?: throw Exception(OPDS2ParserError.invalidLink.name)
                    parsePublications(feed, publications)
                }
            }
            if(topLevelDict.has("navigation")) {
                topLevelDict.get("navigation")?.let {
                    val navigation = it as? JSONArray ?: throw Exception(OPDS2ParserError.invalidLink.name)
                    parseNavigation(feed, navigation)
                }
            }
            if(topLevelDict.has("groups")) {
                topLevelDict.get("groups")?.let {
                    val groups = it as? JSONArray ?: throw Exception(OPDS2ParserError.invalidLink.name)
                    parseGroups(feed, groups)
                }
            }
            return feed
        }

        internal fun parseMetadata(opdsMetadata: OpdsMetadata, metadataDict: JSONObject) {
            if(metadataDict.has("title")) {
                metadataDict.get("title")?.let {
                    opdsMetadata.title = it.toString()
                }}
            if(metadataDict.has("numberOfItems")) {
                metadataDict.get("numberOfItems")?.let {
                    opdsMetadata.numberOfItems = it.toString().toInt()
                }}
            if(metadataDict.has("itemsPerPage")) {
                metadataDict.get("itemsPerPage")?.let {
                    opdsMetadata.itemsPerPage = it.toString().toInt()
                }}
            if(metadataDict.has("modified")) {
                metadataDict.get("modified")?.let {
                    opdsMetadata.modified = DateTime(it.toString()).toDate()
                }}
            if(metadataDict.has("@type")) {
                metadataDict.get("@type")?.let {
                    opdsMetadata.rdfType = it.toString()
                }}
            if(metadataDict.has("currentPage")) {
                metadataDict.get("currentPage")?.let {
                    opdsMetadata.currentPage = it.toString().toInt()
                }}
        }

        internal fun parseLink(linkDict: JSONObject) : Link {
            val link = Link()
            if(linkDict.has("title")) {
                link.title = linkDict.getString("title")
            }
            if(linkDict.has("href")) {
                link.href = getAbsolute(linkDict.getString("href")!!, feedUrl.toString())
            }
            if(linkDict.has("type")) {
                link.typeLink = linkDict.getString("type")
            }
            if(linkDict.has("rel")) {
                if (linkDict.get("rel") is String) {
                    link.rel.add(linkDict.getString("rel"))
                } else if (linkDict.get("rel") is JSONArray) {
                    val array = linkDict.getJSONArray("rel")
                    for (i in 0..(array.length() - 1)) {
                        val string = array.getString(i)
                        link.rel.add(string)
                    }
                }
            }
            if(linkDict.has("height")) {
                link.height = linkDict.getInt("height")
            }
            if(linkDict.has("width")) {
                link.width = linkDict.getInt("width")
            }
            if(linkDict.has("bitrate")) {
                link.bitrate = linkDict.getInt("bitrate")
            }
            if(linkDict.has("duration")) {
                link.duration = linkDict.getDouble("duration")
            }
            if(linkDict.has("properties")) {
                val properties = Properties()
                val propertiesDict = linkDict.getJSONObject("properties")
                if (propertiesDict.has("numberOfItems")) {
                    properties.numberOfItems = propertiesDict.getInt("numberOfItems")
                }
                if (propertiesDict.has("indirectAcquisition")) {
                    val acquisitions = propertiesDict.getJSONArray("indirectAcquisition") ?: throw Exception(OPDS2ParserError.invalidLink.name)
                    for (i in 0..(acquisitions.length() - 1)) {
                        val acquisition = acquisitions.getJSONObject(i)
                        val indirectAcquisition = parseIndirectAcquisition(indirectAcquisitionDict = acquisition)
                        properties.indirectAcquisition.add(indirectAcquisition)
                    }
                }
                if (propertiesDict.has("price")) {
                    val priceDict = propertiesDict.getJSONObject("price")
                    val currency = priceDict["currency"] as? String
                    val value = priceDict["value"] as? Double
                    if (priceDict == null || currency == null || value == null) {
                        throw Exception(OPDS2ParserError.invalidLink.name)
                    }
                    val price = Price(currency = currency, value = value)
                    properties.price = price
                }
            }
            if(linkDict.has("children")) {
                val childLinkDict = linkDict.getJSONObject("children") ?: throw Exception(OPDS2ParserError.invalidLink.name)
                val childLink = parseLink(linkDict = childLinkDict)
                link.children.add(childLink)
            }
            return link
        }


        internal fun parseFacets(feed: Feed, facets: JSONArray) {
            for (i in 0..(facets.length() - 1)) {
                val facetDict = facets.getJSONObject(i)
                val metadata = facetDict.getJSONObject("metadata") ?: throw Exception(OPDS2ParserError.invalidFacet.name)
                val title = metadata["title"] as? String ?: throw Exception(OPDS2ParserError.invalidFacet.name)
                val facet = Facet(title = title)
                parseMetadata(opdsMetadata = facet.metadata, metadataDict = metadata)
                if (facetDict.has("links")){
                    val links = facetDict.getJSONArray("links") ?: throw Exception(OPDS2ParserError.invalidFacet.name)
                    for (k in 0..(links.length() - 1)) {
                        val linkDict = links.getJSONObject(k)
                        val link = parseLink(linkDict = linkDict)
                        facet.links.add(link)
                    }
                }
                feed.facets.add(facet)
            }
        }

        internal fun parseLinks(feed: Feed, links: JSONArray) {
            for (i in 0..(links.length() - 1)) {
                val linkDict = links.getJSONObject(i)
                val link = parseLink(linkDict = linkDict)
                feed.links.add(link)
            }
        }

        internal fun parsePublications(feed: Feed, publications: JSONArray) {
            for (i in 0..(publications.length() - 1)) {
                val pubDict = publications.getJSONObject(i)
                val pub = parsePublication(pubDict, feedUrl)
                feed.publications.add(pub)
            }
        }

        internal fun parseNavigation(feed: Feed, navLinks:JSONArray) {
            for (i in 0..(navLinks.length() - 1)) {
                val navDict = navLinks.getJSONObject(i)
                val link = parseLink(linkDict = navDict)
                feed.navigation.add(link)
            }
        }

        internal fun parseGroups(feed: Feed, groups: JSONArray) {
            for (i in 0..(groups.length() - 1)) {
                val groupDict = groups.getJSONObject(i)
                val metadata = groupDict.getJSONObject("metadata") ?: throw Exception(OPDS2ParserError.invalidGroup.name)
                val title = metadata.getString("title") ?: throw Exception(OPDS2ParserError.invalidGroup.name)
                val group = Group(title = title)
                parseMetadata(opdsMetadata = group.metadata, metadataDict = metadata)

                if (groupDict.has("links")) {
                    val links = groupDict.getJSONArray("links") ?: throw Exception(OPDS2ParserError.invalidGroup.name)
                    for (j in 0..(links.length() - 1)) {
                        val linkDict = links.getJSONObject(j)
                        val link = parseLink(linkDict = linkDict)
                        group.links.add(link)
                    }
                }
                if (groupDict.has("navigation")) {
                    val links = groupDict.getJSONArray("navigation") ?: throw Exception(OPDS2ParserError.invalidGroup.name)
                    for (j in 0..(links.length() - 1)) {
                        val linkDict = links.getJSONObject(j)
                        val link = parseLink(linkDict = linkDict)
                        group.navigation.add(link)
                    }
                }
                if (groupDict.has("publications")) {
                    val publications = groupDict.getJSONArray("publications") ?: throw Exception(OPDS2ParserError.invalidGroup.name)
                    for (j in 0..(publications.length() - 1)) {
                        val pubDict = publications.getJSONObject(j)
                        val pub = parsePublication(pubDict, feedUrl)
                        group.publications.add(pub)
                    }
                }
                feed.groups.add(group)
            }
        }

    }
}


