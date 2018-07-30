/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared


import org.joda.time.DateTime
import org.json.JSONObject
import java.net.URI

class Locator(val publicationId: String, val spineIndex: Integer, val created: DateTime, val title: URI, val locations: Location, val text: LocatorText?): JSONable {

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("publicationId", publicationId)
        json.putOpt("spineIndex", spineIndex)
        json.putOpt("created", created)
        json.putOpt("title", title)
        json.putOpt("location", locations.toJson())
        if (text != null) {
            json.putOpt("text", text)
        }
        return json
    }

    fun toJsonString(): String{
        var jsonString = """{ "publicationId": "$publicationId", "spineIndex": "$spineIndex", "created": "$created", "title": "$title", "locations" : ${locations.toJson()} """
        if (text != null) { jsonString += """, "text" : "${text.toJson()}"""" }
        jsonString += """ }"""
        return jsonString
    }
}

class Location(val id: String, val cfi: String, val css: String, val progression: Double, val position: Integer): JSONable{

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("id", id)
        json.putOpt("cfi", cfi)
        json.putOpt("css", css)
        json.putOpt("progression", progression)
        json.putOpt("position", position)
        return json
    }

    fun toJson(): String {
        var jsonString = """{"""
        id.let { jsonString += """ "id": "$id" """ }
        cfi.let { jsonString += """, "cfi": "$cfi" """ }
        css.let { jsonString += """, "css": "$css" """ }
        progression.let { jsonString += """ "p": "$id" """ }
        id.let { jsonString += """ "id": "$id" """ }
        jsonString += """}"""
    }
}

class LocatorText(val after: String?, val before: String?, val highlight: String?): JSONable{

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("after", after)
        json.putOpt("before", before)
        json.putOpt("highlight", highlight)
        return json
    }

    fun toJson(): String{
        var jsonString =  """{"""
        after.let { jsonString += """ "after": "$after" """ }
        before.let { jsonString += """, "before": "$before" """ }
        highlight.let { jsonString += """, "highlight": "$highlight" """ }
        jsonString += """}"""
    }
}
/*
 override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("title", title)
        json.putOpt("type", typeLink)
        json.putOpt("href", href)
        if (rel.isNotEmpty())
            json.put("rel", getStringArray(rel))
        json.putOpt("properties", properties)
        if (height != 0)
            json.putOpt("height", height)
        if (width != 0)
            json.putOpt("width", width)
        json.putOpt("duration", duration)
        return json
    }
 */