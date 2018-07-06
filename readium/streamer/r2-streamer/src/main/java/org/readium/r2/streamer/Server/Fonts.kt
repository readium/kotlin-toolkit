package org.readium.r2.streamer.Server

import java.io.File

class Fonts {
    val fonts: MutableMap<String, File> = mutableMapOf()

    fun add(key: String, body: File){
        fonts.put(key, body)
    }

    fun get(key: String) : File {
        return fonts[key]!!
    }
}