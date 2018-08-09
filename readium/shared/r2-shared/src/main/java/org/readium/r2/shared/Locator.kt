/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
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
 * @var publicationId: String - Identifier of a Publication
 * @var spineIndex: Long - Index at a spine element
 * @var spineHref: String? - ( Optional ) String reference to the spine element
 * @var timestamp: DateTime - Date when the Locator has been timestamp
 * @var title: URI - Title of the spine element
 * @var locations: Location - Object that's used to locate the target
 *
 *
 * @var text: LocatorText? - ( Optional ) Describe the Locator's context
 *
 */
open class Locator(val publicationId: String,
                   val spineIndex: Long,
                   val spineHref: String?,
                   val title: String,
                   location: Location? = null): JSONable {

    val locations = mutableListOf<Location>()
    var timestamp = DateTime.now().toDate().time
    var text = LocatorText(null, null, null)

    init {
        if(location != null){
            locations.add(location)
        }
    }

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("publicationId", publicationId)
        json.putOpt("spineIndex", spineIndex)
        json.putOpt("spineHref", spineHref)
        json.putOpt("timestamp", timestamp)
        json.putOpt("title", title)
        json.putOpt("location", locations.toString())
        json.putOpt("text", text)
        return json
    }

    override fun toString(): String{
        return """{ "publicationId": "$publicationId", "spineIndex": "$spineIndex",  "spineHref": "$spineHref", "timestamp": "$timestamp", "title": "$title", "locations" : ${locations}  "text" : "${text}" """
    }

    fun setText(before: String? = null, highlight: String? = null, after: String? = null){
        text.before = before
        text.highlight = highlight
        text.after = after
    }

    inner class LocatorText(var before: String?, var highlight: String?, var after: String?): JSONable{

        override fun getJSON(): JSONObject {
            val json = JSONObject()
            json.putOpt("before", before)
            json.putOpt("highlight", highlight)
            json.putOpt("after", after)
            return json
        }

        override fun toString(): String{
            var jsonString =  """{"""
            before.let { jsonString += """ "before": "$before" """ }
            highlight.let { jsonString += """, "highlight": "$highlight" """ }
            after.let { jsonString += """, "after": "$after" """ }
            jsonString += """}"""
            return jsonString
        }
    }
}

/**
 * Location : Class that contain the different variables needed to localize a particular position
 *
 * @var pubId: String - Identifier of a Publication
 * @var cfi: String? - String formatted to designed a particular place in an EPUB
 * @var cssSelector: String? - Css selector
 * @var xpath: String? - An xpath in the resource
 * @var progression: Float - A percentage ( between 0 and 1 ) of the progression in a Publication
 * @var position: integer - Index of a segment in the resource.
 *
 */
class Location(val pubId: String, val cfi: String?, val cssSelector: String?, val xpath: String?, val progression: Double, val position: Long): JSONable{

    override fun getJSON(): JSONObject {
        val json = JSONObject()
        json.putOpt("pubId", pubId)
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
        pubId.let { jsonString += """ "id": "$pubId" """ }
        cfi.let { jsonString += """, "cfi": "$cfi" """ }
        cssSelector.let { jsonString += """, "cssSelector": "$cssSelector" """ }
        xpath.let { jsonString += """, "xpath": "$xpath" """ }
        progression.let { jsonString += """, "progression": "$progression" """ }
        position.let { jsonString += """, "position": "$position" """ }
        jsonString += """}"""
        return jsonString
    }
}