package org.readium.r2.shared.opds

import org.readium.r2.shared.Link


data class Facet(val title: String) {
    var metadata: OpdsMetadata
    var links = mutableListOf<Link>()

    init {
        this.metadata = OpdsMetadata(title = title)
    }

}
