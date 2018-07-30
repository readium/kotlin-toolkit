/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared


import org.joda.time.DateTime
import java.net.URI

class Locator(val publicationId: String, val spineIndex: Integer, val created: DateTime, val title: URI, val locations: MutableList<Location>, val text: LocatorText) {
    fun toJson(): String{
        var jsonString = """{ "publicationId": "$publicationId", "spineIndex": "$spineIndex", "created": "$created", "title": "$title", "locations" : [ """
        if (locations.size > 1) {
            locations.forEach { jsonString = jsonString + it.toJson() + """, """ }
        } else {
            locations.firstOrNull().let { jsonString += """ "${it!!.toJson()}" """ }
        }
        jsonString += """ ], "text" : "${text.toJson()}" }"""
        return jsonString
    }
}

class Location(val id: String, val cfi: String, val css: String, val progression: Double, val position: Integer){
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

class LocatorText(val after: String?, val before: String?, val highlight: String?){
    fun toJson(): String{
        var jsonString =  """{"""
        after.let { jsonString += """ "after": "$after" """ }
        before.let { jsonString += """, "before": "$before" """ }
        highlight.let { jsonString += """, "highlight": "$highlight" """ }
        jsonString += """}"""
    }
}