/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import org.json.JSONObject
import java.io.Serializable

class Subject : JSONable, Serializable{

    private val TAG = this::class.java.simpleName

    var name: String? = null
    //  The WebPubManifest elements
    var sortAs: String? = null
    //  Epub 3.1 "scheme" (opf:authority)
    var scheme: String? = null
    //  Epub 3.1 "code" (opf:term)
    var code: String? = null

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("name", name)
        json.putOpt("sortAs", sortAs)
        json.putOpt("scheme", scheme)
        json.putOpt("code", code)
        return json
    }

}