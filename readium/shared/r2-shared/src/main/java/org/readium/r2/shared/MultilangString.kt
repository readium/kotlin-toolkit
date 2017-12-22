package org.readium.r2.shared

import java.io.Serializable

class MultilangString : Serializable{

    var singleString: String? = null
    var multiString: Map<String, String> = mapOf()

}