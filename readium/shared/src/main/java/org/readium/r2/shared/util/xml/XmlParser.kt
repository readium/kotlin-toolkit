/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.xml

import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.xml.XMLConstants
import org.readium.r2.shared.InternalReadiumApi
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

/** XML Parser with support for namespaces, mixed content and lang inheritance
 *
 * [isNamespaceAware] behaves as defined in XmlPullParser specification.
 * If [isCaseSensitive] is false, attribute and tag names are lowercased during the parsing
 */
@InternalReadiumApi
public class XmlParser(
    private val isNamespaceAware: Boolean = true,
    private val isCaseSensitive: Boolean = true,
) {

    private val parser: XmlPullParser = XmlPullParserFactory.newInstance().let {
        it.isNamespaceAware = isNamespaceAware
        it.newPullParser()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    public fun parse(stream: InputStream): ElementNode {
        parser.setInput(stream, null) // let the parser try to determine input encoding

        val stack = Stack<Triple<MutableList<Node>, AttributeMap, String>>()
        // stack contains children, attributes, and lang
        stack.push(Triple(mutableListOf(), mutableMapOf(), ""))
        var text = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    maybeAddText(text, stack.peek().first)
                    text = ""
                    val attributes = buildAttributeMap(parser)
                    val langAttr =
                        if (isNamespaceAware) {
                            attributes[XMLConstants.XML_NS_URI]?.get("lang")
                        } else {
                            attributes[""]?.get("xml:lang")
                        }
                    stack.push(Triple(mutableListOf(), attributes, langAttr ?: stack.peek().third))
                }
                XmlPullParser.END_TAG -> {
                    val (children, attributes, lang) = stack.pop()
                    maybeAddText(text, children)
                    text = ""
                    val element = buildElement(attributes, children, lang)
                    stack.peek().first.add(element)
                }
                XmlPullParser.TEXT, XmlPullParser.ENTITY_REF -> {
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

    private fun maybeAddText(text: String, children: MutableList<Node>) {
        if (text.isNotEmpty()) {
            children.add(TextNode(text))
        }
    }

    private fun buildElement(attributes: AttributeMap, children: MutableList<Node>, lang: String): ElementNode {
        val rawName = parser.name
        val name = if (isCaseSensitive) rawName else rawName.lowercase(Locale.getDefault())
        return ElementNode(name, parser.namespace, lang, attributes, children)
    }

    private fun buildAttribute(index: Int): Attribute {
        with(parser) {
            val rawName = getAttributeName(index)
            val name = if (isCaseSensitive) rawName else rawName.lowercase(Locale.getDefault())
            return Attribute(name, getAttributeNamespace(index), getAttributeValue(index))
        }
    }

    private fun buildAttributeMap(parser: XmlPullParser): AttributeMap {
        val attributes = (0 until parser.attributeCount).map { buildAttribute(it) }
        val namespaces = attributes.map(Attribute::namespace).distinct()
        return namespaces.associateWith { ns ->
            attributes.filter { it.namespace == ns }.associate { Pair(it.name, it.value) }
        }
    }
}

@InternalReadiumApi
public data class Attribute(val name: String, val namespace: String, val value: String)

@InternalReadiumApi
public typealias AttributeMap = Map<String, Map<String, String>>

@InternalReadiumApi
public sealed class Node

/** Container for text in the XML tree */
@InternalReadiumApi
public data class TextNode(val text: String) : Node()

/** Represents a node with children in the XML tree */
@InternalReadiumApi
public data class ElementNode(
    val name: String,
    val namespace: String = "",
    val lang: String = "",
    val attributes: AttributeMap = emptyMap(),
    val children: List<Node> = listOf(),
) : Node() {

    /** Text of the first child if it is a [TextNode], or null otherwise */
    val text: String?
        get() = (children.firstOrNull() as? TextNode)?.text

    /** Return the [id] attribute as specified in [getAttr] with fallback to XML namespace */
    val id: String?
        get() = getAttr("id") ?: getAttrNs("id", XMLConstants.XML_NS_URI)

    /** Return the value of an attribute picked in the same namespace as this [ElementNode],
     * fallback to no namespace and at last to null. */
    public fun getAttr(name: String): String? = getAttrNs(name, namespace) ?: getAttrNs(name, "")

    /** Return the value of an attribute picked in a specific namespace or null if it does not exist */
    public fun getAttrNs(name: String, namespace: String): String? = attributes[namespace]?.get(
        name
    )

    /** Return a list of all ElementNode children */
    public fun getAll(): List<ElementNode> = children.filterIsInstance<ElementNode>()

    /** Return a list of [ElementNode] children with the given name and namespace */
    public fun get(name: String, namespace: String): List<ElementNode> =
        getAll().filter { it.name == name && it.namespace == namespace }

    /** Return the first [ElementNode] child with the given name and namespace, or null if there is none */
    public fun getFirst(name: String, namespace: String): ElementNode? = get(name, namespace).firstOrNull()

    /** Recursively collect all descendent [ElementNode] with the given name and namespace into a list */
    public fun collect(name: String, namespace: String): List<ElementNode> {
        val founded: MutableList<ElementNode> = mutableListOf()
        for (c in getAll()) {
            if (c.name == name && c.namespace == namespace) founded.add(c)
            founded.addAll(c.collect(name, namespace))
        }
        return founded
    }

    /** Recursively collect and concatenate all descendent [TextNode] in depth-first order */
    public fun collectText(): String {
        val text = StringBuilder()
        for (c in children) {
            when (c) {
                is TextNode -> text.append(c.text)
                is ElementNode -> text.append(c.collectText())
            }
        }
        return text.toString()
    }
}
