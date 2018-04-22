package org.readium.r2.shared.opds

import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication


public class Group {
    public var metadata: OpdsMetadata
    public var links = mutableListOf<Link>()
    public var publications = mutableListOf<Publication>()
    public var navigation = mutableListOf<Link>()

    public constructor(title: String) {
        this.metadata = OpdsMetadata(title = title)
    }
}
