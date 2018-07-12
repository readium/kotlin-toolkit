/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.shared.opds

import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import java.io.Serializable


data class Group(val title: String): Serializable {
     var metadata: OpdsMetadata
     var links = mutableListOf<Link>()
     var publications = mutableListOf<Publication>()
     var navigation = mutableListOf<Link>()

    init {
        this.metadata = OpdsMetadata(title = title)
    }

}
