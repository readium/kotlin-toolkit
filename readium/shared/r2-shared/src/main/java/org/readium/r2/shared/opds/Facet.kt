package org.readium.r2.shared.opds

import org.readium.r2.shared.Link


public class Facet {
    public var metadata: OpdsMetadata
    public var links = mutableListOf<Link>()

    public constructor(title: String) {
        this.metadata = OpdsMetadata(title = title)
    }
}
