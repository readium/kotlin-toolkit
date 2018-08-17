/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import com.google.gson.Gson
import org.joda.time.DateTime
import org.json.JSONObject

/**
 * Locator : That class is used to define a precise location in a Publication
 *
 * @var publicationId: String - Identifier of a Publication
 * @var spineIndex: Long - Index at a spine element
 * @var spineHref: String? - ( Optional ) String reference to the spine element
 * @var title: String - Title of the spine element
 *
 * @var location: Location - List of objects used to locate the target
 * @var timestamp: DateTime - Date when the Locator has been timestamp
 *
 * @var text: LocatorText? - ( Optional ) Describe the Locator's context
 *
 */
open class Locator(val publicationId: String,
                   val spineIndex: Long,
                   val spineHref: String?,
                   val title: String,
                   val location: Location? = null): JSONable {

    var timestamp = DateTime.now().toDate().time
    var text = LocatorText(null, null, null)

    fun toJson(): String{
        return Gson().toJson(this)
    }

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("href", spineHref)
        json.putOpt("title", title)
        json.putOpt("timestamp", timestamp)
        json.putOpt("location", location.toString())
        json.putOpt("text", text)
        return json
    }

    override fun toString(): String{
        return """{ "href": "$spineHref", "title": "$title", "timestamp": "$timestamp", "locations" : $location  "text" : "$text" """
    }

    fun setText(before: String? = null, highlight: String? = null, after: String? = null){
        text.before = before
        text.highlight = highlight
        text.after = after
    }

    inner class LocatorText(var before: String?, var highlight: String?, var after: String?): JSONable{

        fun toJson(): String{
            return Gson().toJson(this)
        }

        override fun getJSON(): JSONObject {
            val json = JSONObject()
            json.putOpt("before", before)
            json.putOpt("highlight", highlight)
            json.putOpt("after", after)
            return json
        }

        override fun toString(): String{
            var jsonString =  """{"""
            if (before != null) {
                before.let { jsonString += """ "before": "$before" """ }
                if (highlight != null) {
                    jsonString += ""","""
                }
            }
            if (highlight != null) {
                highlight.let { jsonString += """ "highlight": "$highlight" """ }
                if (after != null) {
                    jsonString += ""","""
                }
            }
            if (after != null) {
                after.let { jsonString += """ "after": "$after" """ }
            }
            jsonString += """}"""
            return jsonString
        }
    }
}

/**
 * Location : Class that contain the different variables needed to localize a particular position
 *
 * @var id: Long? - Identifier of a specific fragment in the publication
 * @var cfi: String? - String formatted to designed a particular place in an Publication
 * @var cssSelector: String? - Css selector
 * @var xpath: String? - An xpath in the resource
 * @var progression: Double - A percentage ( between 0 and 1 ) of the progression in a Publication
 * @var position: Long - Index of a segment in the resource
 *
 */
class Location(val id: Long?, val cfi: String?, val cssSelector: String?, val xpath: String?, val progression: Double, val position: Long): JSONable{


    fun toJson(): String{
        return Gson().toJson(this)
    }

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("id", id)
        if (cfi != null) {
            json.putOpt("cfi", cfi)
        }
        if (cssSelector != null) {
            json.putOpt("cssSelector", cssSelector)
        }
        if (xpath != null) {
            json.putOpt("xpath", xpath)
        }
        json.putOpt("progression", progression)
        json.putOpt("position", position)
        return json
    }

    override fun toString(): String {
        var jsonString = """{"""
        if (id != null) {
            id.let { jsonString += """ "id": "$id" ,""" }
        }
        if (cfi != null) {
            cfi.let { jsonString += """ "cfi": "$cfi" ,""" }
        }
        if (cssSelector != null) {
            cssSelector.let { jsonString += """ "cssSelector": "$cssSelector" ,""" }
        }
        if (xpath != null) {
            xpath.let { jsonString += """ "xpath": "$xpath" ,""" }
        }
        progression.let { jsonString += """ "progression": "$progression" ,""" }
        position.let { jsonString += """ "position": "$position" """ }
        jsonString += """}"""
        return jsonString
    }
}