package org.readium.r2.streamer.Server

class Ressources {
    val resources: MutableMap<String, String> = mutableMapOf()

    fun add(key: String, body: String){
        resources.put(key, body)
    }

    fun get(key: String) : String {
        return resources[key] ?: ""
    }
}