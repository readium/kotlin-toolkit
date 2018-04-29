package org.readium.r2.shared.opds

import java.io.Serializable
import java.util.*


data class OpdsMetadata(var title: String): Serializable {
    var numberOfItems: Int? = null
    var itemsPerPage: Int? = null
    var currentPage: Int? = null
    var modified: Date? = null
    var position: Int? = null
    var rdfType: String? = null
}
