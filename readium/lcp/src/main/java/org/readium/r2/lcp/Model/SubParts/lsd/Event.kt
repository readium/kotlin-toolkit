package org.readium.r2.lcp.Model.SubParts.lsd

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
            date = Date(json.getString("timestamp"))
            type = json.getString("type")
            id = json.getString("id")
        } catch (e: Exception) {
            throw Exception(LcpParsingError().errorDescription(LcpParsingErrors.json))
        }
    }

    fun parseEvents(key: String) : List<Event> {
        val jsonEvents = json.getJSONArray(key)
        val events = mutableListOf<Event>()
        for (i in 0..jsonEvents.length() - 1) {
            val event = Event(JSONObject(jsonEvents[i].toString()))
            events.add(event)
        }
        return events
    }
}
