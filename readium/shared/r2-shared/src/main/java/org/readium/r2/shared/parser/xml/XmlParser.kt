/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.parser.xml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.util.Stack
import javax.xml.XMLConstants

class XmlParser (val isNamespaceAware: Boolean = true, val isCaseSensitive: Boolean = true) {
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(stream: InputStream) : ElementNode {
        val parser = buildParser(isNamespaceAware)
        parser.setInput(stream, null) // let the parser try to determine input encoding

        val stack = Stack<Triple<MutableList<Node>, AttributeMap, String>>()
        stack.push(Triple(mutableListOf(), mutableMapOf(), ""))
        var text = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    maybeAddText(text, stack.peek().first)
                    text = ""
                    val attributes =  buildAttributeMap(parser)
                    val langAttr = if (isNamespaceAware) attributes[XMLConstants.XML_NS_URI]?.get("lang")
                    else attributes[""]?.get("xml:lang")
                    stack.push(Triple(mutableListOf(), attributes, langAttr ?: stack.peek().third))
                }
                XmlPullParser.END_TAG -> {
                    val (children, attributes, lang) = stack.pop()
                    maybeAddText(text, children)
                    text = ""
                    val element = buildElement(parser, attributes, children, lang)
                    stack.peek().first.add(element)
                }
                XmlPullParser.TEXT -> {
                    text += parser.text
                }
            }
            parser.nextToken()
        }

        stream.close()
        assert(stack.size == 1)
        val children = stack.peek().first
        val roots = children.filterIsInstance<ElementNode>()
        if (roots.size == 1) {
            return roots.first()
        } else {
            throw XmlPullParserException("No unique root element found")
        }
    }

    private fun buildParser(isNamespaceAware: Boolean) : XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = isNamespaceAware
        return factory.newPullParser()
    }

    private fun maybeAddText(text: String, children: MutableList<Node>) {
        if (text.isNotEmpty()) {
           children.add(TextNode(text))
        }
    }

    private fun buildElement(parser: XmlPullParser, attributes:AttributeMap, children: MutableList<Node>, lang: String) : ElementNode {
        val rawName = parser.name
        val name = if (isCaseSensitive) rawName else rawName.toLowerCase()
        val node = ElementNode(name, parser.namespace,  lang, attributes, children)
        return node
    }

    private fun buildAttribute(parser: XmlPullParser, index: Int) : Attribute {
        with(parser) {
            val rawName = getAttributeName(index)
            val name = if (isCaseSensitive) rawName else rawName.toLowerCase()
            return Attribute(name,
                    getAttributeNamespace(index),
                    getAttributeValue(index))
        }
    }

    private fun buildAttributeMap(parser: XmlPullParser) : AttributeMap {
        val attributes = (0 until parser.attributeCount).map { buildAttribute(parser, it) }
        val namespaces = attributes.map(Attribute::namespace).distinct()
        return namespaces.associateWith { ns -> attributes
                .filter{ it.namespace == ns }.associate { Pair(it.name, it.value) } }
    }
}

open class Node

data class TextNode(val text: String) : Node()

data class Attribute(val name: String, val namespace: String, val value:String)

typealias AttributeMap = Map<String,Map<String,String>>

data class ElementNode(
        val name: String,
        val namespace: String = "",
        val lang: String = "",
        val attributes: AttributeMap = mapOf(),
        val children: List<Node> = listOf()) : Node() {

    // Text of the first child, if it is a TextNode, otherwise null
    val text: String?
        get() = (children.firstOrNull() as? TextNode)?.text

    // Id with fallback to XML namespace
    val id: String?
        get() = getAttr("id") ?: getAttrNs("id", XMLConstants.XML_NS_URI)

    // Get attribute in the same namespace as this ElementNode or in no namespace
    fun getAttr(name: String) = getAttrNs(name, namespace) ?: getAttrNs(name, "")

    // Get attribute in a specific namespace
    fun getAttrNs(name: String, namespace: String) = attributes[namespace]?.get(name)

    // Get all ElementNode children
    fun getAll() = children.filterIsInstance<ElementNode>()

    // Get ElementNode children with specific name and namespace
    fun get(name: String, namespace: String) = getAll().filter { it.name == name && it.namespace == namespace}

    fun getFirst(name: String, namespace: String) = get(name, namespace).firstOrNull()

    fun collect(name: String, namespace: String) : List<ElementNode> {
        val founded: MutableList<ElementNode> = mutableListOf()
        for (c in getAll()) {
            if (c.name == name && c.namespace == namespace) founded.add(c)
            founded.addAll(c.collect(name, namespace))
        }
        return founded
    }

    fun collectText() : String {
        val text = StringBuilder()
        for (c in children) {
            when(c) {
                is TextNode -> text.append(c.text)
                is ElementNode -> text.append(c.collectText())
            }
        }
        return text.toString()
    }
}