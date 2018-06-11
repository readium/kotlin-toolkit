package org.readium.r2.shared

data class Collection(var name: String) {
    var sortAs: String? = null
     var identifier: String? = null
     var position: Double? = null
     var links:MutableList<Link> = mutableListOf()

}
