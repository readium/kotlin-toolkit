package org.readium.r2.shared

import org.json.JSONObject

interface JSONable{

    fun getJSON() : JSONObject

}