package org.readium.r2.streamer.Parser.EpubParserSubClasses

import org.readium.r2.shared.Link
import org.readium.r2.streamer.XmlParser.XmlParser
import org.readium.r2.streamer.XmlParser.Node
import org.readium.r2.streamer.Parser.normalize

class NavigationDocumentParser {

    var navigationDocumentPath: String = ""

    fun tableOfContent(document: XmlParser) = nodeArray(document, "toc")
    fun pageList(document: XmlParser) = nodeArray(document, "page-list")
    fun landmarks(document: XmlParser) = nodeArray(document, "landmarks")
    fun listOfIllustrations(document: XmlParser) = nodeArray(document, "loi")
    fun listOfTables(document: XmlParser) = nodeArray(document, "lot")
    fun listOfAudiofiles(document: XmlParser) = nodeArray(document, "loa")
    fun listOfVideos(document: XmlParser) = nodeArray(document, "lov")

    private fun nodeArray(document: XmlParser, navType: String) : List<Link> {
        var body = document.root().getFirst("body")
        body?.getFirst("section")?.let { body = it }
        val navPoint = body?.get("nav")?.firstOrNull{ it.properties["epub:type"] == navType }
        val olElement = navPoint?.getFirst("ol") ?: return emptyList()
        return nodeOl(olElement).children
    }

    private fun nodeOl(element: Node) : Link {
        val newOlNode = Link()
        val liElements = element.get("li") ?: return newOlNode
        for (li in liElements){
            val spanText = li.getFirst("span")?.name
            if (spanText != null && !spanText.isEmpty()){
                li.getFirst("ol")?.let {
                    newOlNode.children.add(nodeOl(it))
                }
            } else{
                val childLiNode = nodeLi(li)
                newOlNode.children.add(childLiNode)
            }
        }
        return newOlNode
    }

    private fun nodeLi(element: Node) : Link {
        val newLiNode = Link()
        val aNode = element.getFirst("a")!!
        val title = (aNode.getFirst("span"))?.name ?: aNode.name
        newLiNode.href = normalize(navigationDocumentPath, aNode.properties["href"])
        newLiNode.title = title
        element.getFirst("ol")?.let { newLiNode.children.add(nodeOl(it)) }
        return newLiNode
    }

}