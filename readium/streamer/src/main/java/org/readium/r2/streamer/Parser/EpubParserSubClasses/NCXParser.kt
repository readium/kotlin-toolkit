package org.readium.r2.streamer.Parser.EpubParserSubClasses

import org.readium.r2.shared.Link
import org.readium.r2.shared.TocElement
import org.readium.r2.streamer.XmlParser.XmlParser
import org.readium.r2.streamer.XmlParser.Node
import org.readium.r2.streamer.Parser.normalize

class NCXParser{

    lateinit var ncxDocumentPath: String

    fun tableOfContents(document: XmlParser) : List<Link> {
        val navMapElement = document.root().getFirst("navMap")!!
        return nodeArray(navMapElement, "navPoint")
    }

    fun pageList(document: XmlParser) : List<Link> {
        val pageListElement = document.root().getFirst("pageList")
        return nodeArray(pageListElement, "pageTarget")
    }

    private fun nodeArray(element: Node?, type: String) : List<Link> {
        // The "to be returned" node array.
        val newNodeArray: MutableList<Link> = mutableListOf()

        // Find the elements of `type` in the XML element.
        val elements = element?.get(type) ?: return emptyList()
        // For each element create a new node of type `type`.
        for (newNode in elements.map{node(it, type)})
            newNodeArray.plusAssign(newNode)
        return newNodeArray
    }

    private fun node(element: Node, type: String) : Link{
        val newNode = Link()
        newNode.href = normalize(ncxDocumentPath, element.getFirst("content")?.properties?.get("src"))
        newNode.title = element.getFirst("navLabel")!!.getFirst("text")!!.text
        element.get("navPoint")?.let {
            for (childNode in it){
                newNode.children.plusAssign(node(childNode, type))
            }
        }
        return newNode
    }

}