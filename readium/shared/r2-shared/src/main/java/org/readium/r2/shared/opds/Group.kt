package org.readium.r2.shared.opds

import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication


data class Group(val title: String) {
     var metadata: OpdsMetadata
     var links = mutableListOf<Link>()
     var publications = mutableListOf<Publication>()
     var navigation = mutableListOf<Link>()

    init {
        this.metadata = OpdsMetadata(title = title)
    }

}
