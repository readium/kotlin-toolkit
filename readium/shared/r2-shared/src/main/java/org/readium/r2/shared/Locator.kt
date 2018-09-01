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



/**
 * Locator model
 *
 * @var bookId: Long? - Book index in the database
 * @val publicationID: String -  Publication identifier
 * @val resourceIndex: Long - Index to the spine element of the book
 * @val resourceHref: String -  Reference to the spine element
 * @val resourceTitle: String - Title to the spine element of the book
 * @val location: Location - Location in the spine element
 * @val creationDate: Long - Datetime when the bookmark has been created
 * @var id: Long? - ID of the bookmark in database
 *
 * @fun toString(): String - Return a String description of the Locator
 */

class Locator(val bookID: Long,
              val publicationID: String,
              val resourceIndex: Long,
              val resourceHref: String,
              val resourceTitle: String,
              val location: Location,
              var creationDate: Long = DateTime().toDate().time,
              var id: Long? = null) {

    //TODO update this
//    override fun toString(): String {
//        return "Locator id : ${this.id}, book id : ${this.bookID}, resource href selected ${this.resourceHref}, progression saved ${this.location.progression} and created the ${this.creationDate}."
//    }

}


///**
// * Locator : That class is used to define a precise location in a Publication
// *
// * @var publicationId: String - Identifier of a Publication
// * @var spineIndex: Long - Index of a spine element
// * @var resourceHref: String? - ( Optional ) String reference to the spine element
// * @var title: String - Title of the spine element
// *
// * @var location: Location - List of objects used to locate the target
// * @var created: DateTime - Date when the Locator has been created
// *
// * @var text: LocatorText? - ( Optional ) Describe the Locator's context
// *
// */
//open class Locator(val publicationId: String,
//                   val spineIndex: Long?,
//                   open val resourceHref: String,
//                   val title: String,
//                   val location: Location? = null): JSONable {
//
//    var created = DateTime.now().toDate().time
//    var text = LocatorText(null, null, null)
//
//    fun toJson(): String{
//        return Gson().toJson(this)
//    }
//
//    override fun toJSON(): JSONObject {
//        val json = JSONObject()
//        json.putOpt("href", resourceHref)
//        json.putOpt("title", title)
//        json.putOpt("created", created)
//        json.putOpt("location", location.toString())
//        json.putOpt("text", text)
//        return json
//    }
//
//    override fun toString(): String{
//        return """{ "href": "$resourceHref", "title": "$title", "created": "$created", "locations" : $location  "text" : "$text" """
//    }
//
//    fun setText(before: String? = null, highlight: String? = null, after: String? = null){
//        text.before = before
//        text.highlight = highlight
//        text.after = after
//    }
//
//    inner class LocatorText(var before: String?, var highlight: String?, var after: String?): JSONable{
//
//        fun toJson(): String{
//            return Gson().toJson(this)
//        }
//
//        override fun toJSON(): JSONObject {
//            val json = JSONObject()
//            json.putOpt("before", before)
//            json.putOpt("highlight", highlight)
//            json.putOpt("after", after)
//            return json
//        }
//
//        override fun toString(): String{
//            var jsonString =  """{"""
//            if (before != null) {
//                before.let { jsonString += """ "before": "$before" """ }
//                if (highlight != null) {
//                    jsonString += ""","""
//                }
//            }
//            if (highlight != null) {
//                highlight.let { jsonString += """ "highlight": "$highlight" """ }
//                if (after != null) {
//                    jsonString += ""","""
//                }
//            }
//            if (after != null) {
//                after.let { jsonString += """ "after": "$after" """ }
//            }
//            jsonString += """}"""
//            return jsonString
//        }
//    }
//}

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
class Location(var id: Long? = null,
               var cfi: String? = null,
               var cssSelector: String? = null,
               var xpath: String? = null,
               var progression: Double? = null,
               var position: Long? = null) : JSONable {

    companion object {
        fun fromJSON(json: JSONObject): Location {

            val location = Location()
            if (json.has("id")) {
                location.id = json.getLong("id")
            }
            if (json.has("cfi")) {
                location.cfi = json.getString("cfi")
            }
            if (json.has("cssSelector")) {
                location.cssSelector = json.getString("cssSelector")
            }
            if (json.has("xpath")) {
                location.xpath = json.getString("xpath")
            }
            if (json.has("progression")) {
                location.progression = json.getDouble("progression")
            }
            if (json.has("position")) {
                location.position = json.getLong("position")
            }

            return location
        }
    }

    override fun toJSON(): JSONObject {
        val json = JSONObject()

        id?.let {
            json.putOpt("id", id)
        }
        cfi?.let {
            json.putOpt("cfi", cfi)
        }
        cssSelector?.let {
            json.putOpt("cssSelector", cssSelector)
        }
        xpath?.let {
            json.putOpt("xpath", xpath)
        }
        progression?.let {
            json.putOpt("progression", progression)
        }
        position?.let {
            json.putOpt("position", position)
        }

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