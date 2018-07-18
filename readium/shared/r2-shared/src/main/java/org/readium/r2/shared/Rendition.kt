/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import org.json.JSONObject
import java.io.Serializable

enum class RenditionLayout(val ini: String) : Serializable{
    Reflowable("reflowable"),
    Fixed("fixed")
}

enum class RenditionFlow(val ini: String) : Serializable{
    Paginated("paginated"),
    Continuous("continuous"),
    Document("document"),
    Fixed("Fixed")
}

enum class RenditionOrientation(val ini: String) : Serializable{
    Auto("auto"),
    Landscape("landscape"),
    Portrait("portrait")
}

enum class RenditionSpread(val ini: String) : Serializable{
    Auto("auto"),
    Landscape("landscape"),
    Portrait("portrait"),
    Both("both"),
    None("none")
}

class Rendition : Serializable {
    var flow: RenditionFlow? = null
    var spread: RenditionSpread? = null
    var layout: RenditionLayout? = null
    var viewport: String? = null
    var orientation: RenditionOrientation? = null

    fun isEmpty() : Boolean {
        return (layout == null
                && flow == null
                && spread == null
                && viewport == null
                && orientation == null)
    }

    fun getJSON() : JSONObject{
        val obj = JSONObject()
        obj.putOpt("flow", flow?.toString())
        obj.putOpt("spread", spread?.toString())
        obj.putOpt("layout", layout?.toString())
        obj.putOpt("viewport", viewport)
        obj.putOpt("orientation", orientation?.toString())
        return obj
    }

}