// TODO: extensions
/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.model.components.lcp

import org.json.JSONObject

data class User(val json: JSONObject) {
    val id: String?
    val email: String?
    val name: String?
    var extensions: JSONObject
    var encrypted = mutableListOf<String>()

    init {
        id = if (json.has("id")) json.getString("id") else  null
        email = if (json.has("email")) json.getString("email") else  null
        name = if (json.has("name")) json.getString("name") else  null

        if (json.has("encrypted")) {
            val encryptedArray = json.getJSONArray("encrypted")
            for (i in 0 until encryptedArray.length()){
                encrypted.add(encryptedArray.getString(i))
            }
        }

//        json.remove("id")
//        json.remove("email")
//        json.remove("name")
//        json.remove("encrypted")

        extensions = json

    }
}
