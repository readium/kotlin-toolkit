/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import android.net.UrlQuerySanitizer
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.net.URL

class Contributor : JSONable, Serializable {

    private val TAG = this::class.java.simpleName

    var multilangName:MultilangString = MultilangString()
    var sortAs: String? = null
    var roles: MutableList<String> = mutableListOf()
    var links: MutableList<Link> = mutableListOf()
    var identifier: String? = null

    var name: String? = null
        get() = multilangName.singleString

    override fun getJSON() : JSONObject{
        val obj = JSONObject()
        obj.put("name", name)
        if (roles.isNotEmpty()) {
            obj.put("roles", getStringArray(roles))
        }
        obj.put("sortAs", sortAs)
        return obj
    }

}

fun parseContributors(contributors: Any) : List<Contributor> {
    val result: MutableList<Contributor> = mutableListOf()
    if (contributors is String) {
        val c = Contributor()
        c.multilangName.singleString = contributors
        result.add(c)
    } else  if (contributors is JSONObject){
        val c = parseContributor(contributors)
        result.add(c)
    } else if (contributors is JSONArray) {
        for (i in 0..(contributors.length() - 1)) {
            val obj = contributors.getJSONObject(i)
            val c = parseContributor(obj)
            result.add(c)
        }
    }
    return result
}

fun parseContributor(cDict: JSONObject) : Contributor {
    val c = Contributor()

    if (cDict.has("name")){
        if (cDict.get("name") is String) {
            c.multilangName.singleString = cDict.getString("name")
        } else if (cDict.get("name") is JSONObject) {
            val array = cDict.getJSONObject("name")
            c.multilangName.multiString = array as MutableMap<String, String>
        }

    }
    if (cDict.has("identifier")){
        c.identifier = cDict.getString("identifier")
    }
    if (cDict.has("sort_as")){
        c.sortAs = cDict.getString("sort_as")
    }
    if (cDict.has("role")){
        c.roles.add(cDict.getString("role"))
    }
    if (cDict.has("links")){
        val linkDict = cDict.getJSONObject("links")
        c.links.add(parseLink(linkDict))
    }
    return c
}
