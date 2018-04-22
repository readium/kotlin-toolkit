package org.readium.r2.shared.opds

import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication


public class Feed {
    public var metadata: OpdsMetadata
    public var links = mutableListOf<Link>()
    public var facets = mutableListOf<Facet>()
    public var groups = mutableListOf<Group>()
    public var publications = mutableListOf<Publication>()
    public var navigation = mutableListOf<Link>()
    public var context = mutableListOf<String>()

    public constructor(title: String) {
        this.metadata = OpdsMetadata(title = title)
    }

    internal fun getSearchLinkHref() : String? {
        val searchLink = links.firstOrNull { it.rel.contains("search") }
        return searchLink?.href
    }
}
