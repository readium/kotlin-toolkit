package org.readium.r2.shared.opds

import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import java.io.Serializable


data class Feed(val title: String, val type: Int) : Serializable {
    var metadata: OpdsMetadata
    var links:MutableList<Link> = mutableListOf()
    var facets:MutableList<Facet> = mutableListOf()
    var groups:MutableList<Group> = mutableListOf()
    var publications:MutableList<Publication> = mutableListOf()
    var navigation:MutableList<Link> = mutableListOf()
    var context:MutableList<String> = mutableListOf()

    init {
        this.metadata = OpdsMetadata(title = title)
    }

    internal fun getSearchLinkHref() : String? {
        val searchLink = links.firstOrNull { it.rel.contains("search") }
        return searchLink?.href
    }
}

data class ParseData(val feed: Feed?, val publication: Publication?, val type: Int) : Serializable
