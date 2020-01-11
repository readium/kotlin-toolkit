/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import org.json.JSONObject
import java.io.Serializable

/**
 * Locator model - https://github.com/readium/architecture/tree/master/locators
 *
 * @val href: String -  The href of the resource the locator points at.
 * @val type: String - The media type of the resource that the Locator Object points to.
 * @val title: String - The title of the chapter or section which is more relevant in the context of this locator.
 * @val location: Location - One or more alternative expressions of the location.
 * @val text: LocatorText? - Textual context of the locator.
 */

open class Locator(var href: String? = null,
                   var type: String? = null,
                   var title: String? = null,
                   var locations: Locations? = null,
                   var text: LocatorText? = null) : JSONable, Serializable {


    companion object {
        fun fromJSON(json: JSONObject): Locator {

            val locator = Locator()
            if (json.has("href")) {
                locator.href = json.getString("href")
            }
            if (json.has("type")) {
                locator.type = json.getString("type")
            }
            if (json.has("title")) {
                locator.title = json.getString("title")
            }
            if (json.has("locations")) {
                locator.locations = Locations.fromJSON(JSONObject(json.getString("locations")))
            }
            if (json.has("text")) {
                locator.text = LocatorText.fromJSON(JSONObject(json.getString("text")))
            }

            return locator
        }
    }

    override fun toJSON(): JSONObject {
        val json = JSONObject()

        href.let {
            json.putOpt("href", href)
        }
        type.let {
            json.putOpt("type", type)
        }
        title.let {
            json.putOpt("title", title)
        }
        locations?.let {
            json.putOpt("locations", it.toJSON())
        }
        text?.let {
            json.putOpt("text", it.toJSON())
        }
        return json
    }

}

class LocatorText(var after: String? = null,
                  var before: String? = null,
                  var highlight: String? = null)
    : JSONable, Serializable {

    companion object {
        fun fromJSON(json: JSONObject): LocatorText {

            val location = LocatorText()
            if (json.has("before")) {
                location.before = json.getString("before")
            }
            if (json.has("highlight")) {
                location.highlight = json.getString("highlight")
            }
            if (json.has("after")) {
                location.after = json.getString("after")
            }

            return location
        }
    }

    override fun toJSON(): JSONObject {
        val json = JSONObject()

        before?.let {
            json.putOpt("before", before)
        }
        highlight?.let {
            json.putOpt("highlight", highlight)
        }
        after?.let {
            json.putOpt("after", after)
        }

        return json
    }

}

/**
 * Location : Class that contain the different variables needed to localize a particular position
 *
 * @var fragment: Long? - Contains one or more fragment in the resource referenced by the Locator Object.
 * @var progression: Double - Progression in the resource expressed as a percentage.
 * @var position: Long - An index in the publication.
 *
 */
class Locations(var fragment: String? = null,        // 1 = fragment identifier (toc, page lists, landmarks)
                var progression: Double? = null,     // 2 = bookmarks
                var position: Long? = null,           // 3 = goto page
                var cssSelector: String? = null,
                var partialCfi: String? = null,
                var domRange: DomRange? = null
) : JSONable, Serializable {

    companion object {
        fun fromJSON(json: JSONObject): Locations {

            val location = Locations()
            if (json.has("fragment")) {
                location.fragment = json.getString("fragment")
            }
            if (json.has("progression")) {
                location.progression = json.getDouble("progression")
            }
            if (json.has("position")) {
                location.position = json.getLong("position")
            }
            if (json.has("cssSelector")) {
                location.cssSelector = json.getString("cssSelector")
            }
            if (json.has("partialCfi")) {
                location.partialCfi = json.getString("partialCfi")
            }
            if (json.has("domRange")) {
                location.domRange = DomRange.fromJSON(JSONObject(json.getString("domRange")))
            }
            return location
        }

        fun isEmpty(locations: Locations): Boolean {
            if (locations.fragment == null && locations.position == null && locations.progression == null && locations.cssSelector == null && locations.partialCfi == null && locations.domRange == null) {
                return true
            }
            return false
        }

    }

    override fun toJSON(): JSONObject {
        val json = JSONObject()

        fragment?.let {
            json.putOpt("fragment", fragment)
        }
        progression?.let {
            json.putOpt("progression", progression)
        }
        position?.let {
            json.putOpt("position", position)
        }
        cssSelector?.let {
            json.putOpt("cssSelector", cssSelector)
        }
        partialCfi?.let {
            json.putOpt("partialCfi", partialCfi)
        }
        domRange?.let {
            json.putOpt("domRange", it.toJSON())
        }

        return json
    }
}

class Range(var cssSelector: String? = null,
            var textNodeIndex: Long? = null,
            var offset: Long? = null
) : JSONable, Serializable {

    companion object {
        fun fromJSON(json: JSONObject): Range {

            val range = Range()
            if (json.has("cssSelector")) {
                range.cssSelector = json.getString("cssSelector")
            }
            if (json.has("textNodeIndex")) {
                range.textNodeIndex = json.getLong("textNodeIndex")
            }
            if (json.has("offset")) {
                range.offset = json.getLong("offset")
            }
            return range
        }

        fun isEmpty(locations: Range): Boolean {
            if (locations.cssSelector == null && locations.textNodeIndex == null && locations.offset == null) {
                return true
            }
            return false
        }

    }

    override fun toJSON(): JSONObject {
        val json = JSONObject()

        cssSelector?.let {
            json.putOpt("cssSelector", cssSelector)
        }
        textNodeIndex?.let {
            json.putOpt("textNodeIndex", textNodeIndex)
        }
        offset?.let {
            json.putOpt("offset", offset)
        }

        return json
    }
}


class DomRange(var start: Range? = null,
               var end: Range? = null
) : JSONable, Serializable {

    companion object {
        fun fromJSON(json: JSONObject): DomRange {

            val domRange = DomRange()

            if (json.has("start")) {
                domRange.start = Range.fromJSON(JSONObject(json.getString("start")))
            }
            if (json.has("end")) {
                domRange.end = Range.fromJSON(JSONObject(json.getString("end")))
            }

            return domRange
        }

        fun isEmpty(locations: DomRange): Boolean {
            if (locations.start == null && locations.end == null) {
                return true
            }
            return false
        }

    }

    override fun toJSON(): JSONObject {
        val json = JSONObject()

        start?.let {
            json.putOpt("start", it.toJSON())
        }
        end?.let {
            json.putOpt("end", it.toJSON())
        }

        return json
    }

}
