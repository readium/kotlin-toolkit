package org.readium.r2.shared

import org.json.JSONObject
import java.io.Serializable

class Contributor : JSONable, Serializable {

    var multilangName:MultilangString = MultilangString()

    var name: String? = null
        get() = multilangName.singleString

    var sortAs: String? = null
    var roles: MutableList<String> = mutableListOf()

    override fun getJSON() : JSONObject{
        val obj = JSONObject()
        obj.put("name", name)
        if (roles.isNotEmpty())
            obj.put("roles", getStringArray(roles))
        obj.put("sortAs", sortAs)
        return obj
    }

}