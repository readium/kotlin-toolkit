package org.readium.r2.shared.opds

import java.util.*


public class OpdsMetadata {
    public var title: String
    public var numberOfItem: Int? = null
    public var itemsPerPage: Int? = null
    public var currentPage: Int? = null
    public var modified: Date? = null
    public var position: Int? = null
    public var rdfType: String? = null

    constructor(title: String) {
        this.title = title
    }
}
