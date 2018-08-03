/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared


import org.joda.time.DateTime
import org.json.JSONObject
import java.net.URI

/**
 * Locator : That class is used to define a precise location in a Publication
 *
 * @param publicationId: String - Identifier of a Publication
 * @param spineIndex: Integer - Index at a spine element
 * @param created: DateTime - Date when the Locator has been created ( unique )
 * @param title: URI - Title of the spine element
 * @param locations: Location - Object that's used to locate the target
 *
 *
 * @var text: LocatorText? - If there's one, describe the Locator's context
 *
 */
class Locator(val publicationId: String, val spineIndex: Int, val created: DateTime, val title: URI, val locations: Location): JSONable {

    var text = LocatorText(null, null, null)

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("publicationId", publicationId)
        json.putOpt("spineIndex", spineIndex)
        json.putOpt("created", created)
        json.putOpt("title", title)
        json.putOpt("location", locations.toString())
        json.putOpt("text", text)
        return json
    }

    override fun toString(): String{
        return """{ "publicationId": "$publicationId", "spineIndex": "$spineIndex", "created": "$created", "title": "$title", "locations" : ${locations}  "text" : "${text}" """
    }

    fun setText(after: String? = null, before: String? = null, highlight: String? = null){
        if (after != null) {
            text.after = after
        }
        if (before != null) {
            text.before = before
        }
        if (highlight != null) {
            text.highlight = highlight
        }
    }

    inner class LocatorText(var after: String?, var before: String?, var highlight: String?): JSONable{

        override fun getJSON(): JSONObject {
            val json = JSONObject()
            json.putOpt("after", after)
            json.putOpt("before", before)
            json.putOpt("highlight", highlight)
            return json
        }

        override fun toString(): String{
            var jsonString =  """{"""
            after.let { jsonString += """ "after": "$after" """ }
            before.let { jsonString += """, "before": "$before" """ }
            highlight.let { jsonString += """, "highlight": "$highlight" """ }
            jsonString += """}"""
            return jsonString
        }
    }
}

/**
 * Location : Class that contain the different variables needed to localize a particular position
 *
 * @param pubId: String - Identifier of a Publication
 * @param cfi: String? - String formatted to designed a particular place in an EPUB
 * @param css: String? - Css selector
 * @param progression: Float - A percentage ( between 0 and 1 ) of the progression in a Publication
 * @param position: integer - Index of a segment in the resource.
 *
 */
class Location(val pubId: String, val cfi: String?, val css: String?, val progression: Float, val position: Integer): JSONable{

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("pubId", pubId)
        if (cfi != null) {
            json.putOpt("cfi", cfi)
        }
        if (css != null) {
            json.putOpt("css", css)
        }
        json.putOpt("progression", progression)
        json.putOpt("position", position)
        return json
    }

    override fun toString(): String {
        var jsonString = """{"""
        pubId.let { jsonString += """ "id": "$pubId" """ }
        cfi.let { jsonString += """, "cfi": "$cfi" """ }
        css.let { jsonString += """, "css": "$css" """ }
        progression.let { jsonString += """, "progression": "$progression" """ }
        position.let { jsonString += """, "position": "$position" """ }
        jsonString += """}"""
        return jsonString
    }
}