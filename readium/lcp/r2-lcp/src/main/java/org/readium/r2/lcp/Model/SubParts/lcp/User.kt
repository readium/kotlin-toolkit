/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.Model.SubParts.lcp

import org.json.JSONObject

class User(json: JSONObject) {
    var id: String
    var email: String
    var name: String
    var encrypted = mutableListOf<String>()

    init {
        id = json.getString("id")
        email = json.getString("email")
        name = json.getString("name")
        val encryptedArray = json.getJSONArray("encrypted")
        for (i in 0..encryptedArray.length() - 1){
            encrypted.add(encryptedArray.getString(i))
        }
    }
}