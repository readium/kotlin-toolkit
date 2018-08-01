/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.model.sub.lsd

import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.lcp.LcpParsingError
import org.readium.r2.lcp.LcpParsingErrors
import java.util.*

class Event(val json: JSONObject) {

    var type: String
    var name: String
    var id: String
    var date: Date

    init {
        try {
            name = json.getString("name")
            date = DateTime(json.getString("timestamp")).toDate()
            type = json.getString("type")
            id = json.getString("id")
        } catch (e: Exception) {
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }

}
fun parseEvents(json: JSONArray) : List<Event> {
//    val jsonEvents = json.getJSONArray(key)
    val events = mutableListOf<Event>()
    for (i in 0..json.length() - 1) {
        val event = Event(JSONObject(json[i].toString()))
        events.add(event)
    }
    return events
}
