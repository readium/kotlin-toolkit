/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.Link
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.streamer.parser.normalize
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class NavigationDocumentParser {

    var navigationDocumentPath: String = ""

    fun tableOfContent(xml: ByteArray) : MutableList<Link> {
        val tableOfContents = mutableListOf<Link>()
        val document = xml.inputStream()

        val xpath = "/xhtml:html/xhtml:body/xhtml:nav[@epub:type='toc']//xhtml:a" + "|/xhtml:html/xhtml:body/xhtml:nav[@epub:type='toc']//xhtml:span"
        val nodes = evaluateXpath(xpath, document)

        for (i in 0 until nodes.length) {
            nodes.item(i).attributes.getNamedItem("href")?.let {
                val link = Link()
                link.href = normalize(navigationDocumentPath, it.nodeValue)
                link.title = nodes.item(i).textContent
                tableOfContents.add(link)
            }
        }

        return tableOfContents
    }

    fun pageList(document: ElementNode) = nodeArray(document, "page-list")
    fun landmarks(document: ElementNode) = nodeArray(document, "landmarks")
    fun listOfIllustrations(document: ElementNode) = nodeArray(document, "loi")
    fun listOfTables(document: ElementNode) = nodeArray(document, "lot")
    fun listOfAudiofiles(document: ElementNode) = nodeArray(document, "loa")
    fun listOfVideos(document: ElementNode) = nodeArray(document, "lov")

    private fun nodeArray(document: ElementNode, navType: String): List<Link> {
        var body = document.getFirst("body", Namespaces.Xhtml)
        body?.getFirst("section", Namespaces.Xhtml)?.let { body = it }
        val navPoint = body?.get("nav", Namespaces.Xhtml)?.firstOrNull { it.getAttrNs("type", Namespaces.Ops) == navType }
        val olElement = navPoint?.getFirst("ol", Namespaces.Xhtml) ?: return emptyList()
        return nodeOl(olElement).children
    }

    private fun nodeOl(element: ElementNode): Link {
        val newOlNode = Link()
        val liElements = element.get("li", Namespaces.Xhtml)
        for (li in liElements) {
            val spanText = li.getFirst("span", Namespaces.Xhtml)?.name
            if (spanText != null && spanText.isNotEmpty()) {
                li.getFirst("ol", Namespaces.Xhtml)?.let {
                    newOlNode.children.add(nodeOl(it))
                }
            } else {
                val childLiNode = nodeLi(li)
                newOlNode.children.add(childLiNode)
            }
        }
        return newOlNode
    }

    private fun nodeLi(element: ElementNode): Link {
        val newLiNode = Link()
        val aNode = element.getFirst("a", Namespaces.Xhtml)!!
        val title = (aNode.getFirst("span", Namespaces.Xhtml))?.text ?: aNode.text ?: aNode.name
        newLiNode.href = normalize(navigationDocumentPath, aNode.getAttr("href"))
        newLiNode.title = title
        element.getFirst("ol", Namespaces.Xhtml)?.let { newLiNode.children.add(nodeOl(it)) }
        return newLiNode
    }

    private fun evaluateXpath(expression: String, doc: InputStream): NodeList {

        val dbFactory = DocumentBuilderFactory.newInstance()
        dbFactory.isNamespaceAware = true
        
        val docBuilder  = dbFactory.newDocumentBuilder()

        val document = docBuilder.parse(doc)

        val xPath = XPathFactory.newInstance().newXPath()
        xPath.namespaceContext = NameSpaceResolver()

        return xPath.evaluate(expression, document, XPathConstants.NODESET) as NodeList
    }
}

class NameSpaceResolver : NamespaceContext {
    override fun getNamespaceURI(p0: String?): String {
        return when (p0) {
            null -> throw IllegalArgumentException("No prefix provided!")
            "epub" -> Namespaces.Ops
            "xhtml" -> Namespaces.Xhtml
            else -> XMLConstants.DEFAULT_NS_PREFIX
        }
    }

    override fun getPrefix(p0: String?): String? {
        return null
    }

    override fun getPrefixes(p0: String?): MutableIterator<Any?>? {
        return null
    }
}

