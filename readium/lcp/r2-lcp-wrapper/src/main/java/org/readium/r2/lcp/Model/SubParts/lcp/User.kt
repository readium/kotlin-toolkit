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
            encrypted.add(encrypted[i])
        }
    }
}