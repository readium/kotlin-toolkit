package org.readium.r2.shared

import java.io.Serializable

class MetadataItem : Serializable{

    var property: String? = null
    var value: String? = null
    var children: MutableList<MetadataItem> = mutableListOf()
}