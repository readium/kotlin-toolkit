package org.readium.r2.streamer.XmlParser

class Node (val name: String) {

    var children: MutableList<Node> = mutableListOf()
    var properties: MutableMap<String, String> = mutableMapOf()
    var text: String? = ""

    fun get(name: String) = try {
        children.filter{it.name == name}
    } catch(e: Exception) { null }

    fun getFirst(name: String) = try {
        children.first{it.name == name}
    } catch(e: Exception) { null }

}