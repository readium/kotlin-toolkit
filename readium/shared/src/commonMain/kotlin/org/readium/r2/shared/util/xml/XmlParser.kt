/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.xml

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlStreaming
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.getOrElse

/** The `xml` namespace, e.g. of `xml:lang` attributes. */
internal const val XML_NS_URI: String = "http://www.w3.org/XML/1998/namespace"

/** The namespace of `xmlns` attributes. */
private const val XMLNS_ATTRIBUTE_NS_URI: String = "http://www.w3.org/2000/xmlns/"

/**
 * Thrown when the input cannot be parsed as an XML document.
 */
@InternalReadiumApi
public class XmlParserException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** XML Parser with support for namespaces, mixed content and lang inheritance
 *
 * [isNamespaceAware] behaves as defined in the XmlPullParser specification.
 * If [isCaseSensitive] is false, attribute and tag names are lowercased during the parsing
 */
@InternalReadiumApi
public class XmlParser(
    private val isNamespaceAware: Boolean = true,
    private val isCaseSensitive: Boolean = true,
) {

    /**
     * Parses the given XML [string] into an [ElementNode] tree.
     */
    @Throws(XmlParserException::class)
    public fun parse(string: String): ElementNode {
        val reader = try {
            xmlStreaming.newGenericReader(string)
        } catch (e: XmlException) {
            throw XmlParserException("Failed to open the XML document.", e)
        }
        try {
            return parse(reader)
        } finally {
            reader.close()
        }
    }

    /**
     * Parses the given XML [bytes] into an [ElementNode] tree.
     *
     * The character encoding is detected from the byte order mark or the XML declaration,
     * defaulting to UTF-8.
     */
    @Throws(XmlParserException::class)
    public fun parse(bytes: ByteArray): ElementNode =
        parse(bytes.decodeXmlString())

    /**
     * Parses the content of the given [readable] into an [ElementNode] tree.
     *
     * @throws XmlParserException if the content is not a valid XML document.
     * @throws ReadException if the content cannot be read.
     */
    public suspend fun parse(readable: Readable): ElementNode =
        parse(readable.read().getOrElse { throw ReadException(it) })

    private fun parse(reader: XmlReader): ElementNode {
        // Each frame contains children, attributes and lang of the element being parsed.
        val stack = ArrayDeque<Triple<MutableList<Node>, AttributeMap, String>>()
        stack.addLast(Triple(mutableListOf(), emptyMap(), ""))
        val text = StringBuilder()

        try {
            while (reader.hasNext()) {
                when (reader.next()) {
                    EventType.START_ELEMENT -> {
                        maybeAddText(text, stack.last().first)
                        val attributes = buildAttributeMap(reader)
                        val langAttr =
                            if (isNamespaceAware) {
                                attributes[XML_NS_URI]?.get("lang")
                            } else {
                                attributes[""]?.get("xml:lang")
                            }
                        stack.addLast(
                            Triple(mutableListOf(), attributes, langAttr ?: stack.last().third)
                        )
                    }
                    EventType.END_ELEMENT -> {
                        val (children, attributes, lang) = stack.removeLast()
                        maybeAddText(text, children)
                        val element = buildElement(reader, attributes, children, lang)
                        stack.last().first.add(element)
                    }
                    EventType.TEXT,
                    EventType.CDSECT,
                    EventType.IGNORABLE_WHITESPACE,
                    EventType.ENTITY_REF,
                    -> {
                        text.append(reader.text)
                    }
                    else -> {}
                }
            }
        } catch (e: XmlException) {
            throw XmlParserException("Failed to parse the XML document.", e)
        }

        if (stack.size != 1) {
            throw XmlParserException("Malformed XML document: unbalanced elements.")
        }
        val roots = stack.last().first.filterIsInstance<ElementNode>()
        return roots.singleOrNull()
            ?: throw XmlParserException("No unique root element found")
    }

    private fun maybeAddText(text: StringBuilder, children: MutableList<Node>) {
        if (text.isNotEmpty()) {
            children.add(TextNode(text.toString()))
            text.clear()
        }
    }

    private fun buildElement(
        reader: XmlReader,
        attributes: AttributeMap,
        children: MutableList<Node>,
        lang: String,
    ): ElementNode {
        val rawName =
            if (isNamespaceAware) {
                reader.localName
            } else {
                qualifiedName(reader.prefix, reader.localName)
            }
        val name = if (isCaseSensitive) rawName else rawName.lowercase()
        val namespace = if (isNamespaceAware) reader.namespaceURI else ""
        return ElementNode(name, namespace, lang, attributes, children)
    }

    private fun buildAttributeMap(reader: XmlReader): AttributeMap {
        val attributes = mutableListOf<Attribute>()

        for (i in 0 until reader.attributeCount) {
            val namespace = reader.getAttributeNamespace(i)
            val prefix = reader.getAttributePrefix(i)
            val localName = reader.getAttributeLocalName(i)
            val value = reader.getAttributeValue(i)

            val isNamespaceDeclaration = namespace == XMLNS_ATTRIBUTE_NS_URI ||
                (prefix.isEmpty() && localName == "xmlns")

            if (isNamespaceDeclaration) {
                if (!isNamespaceAware) {
                    val rawName = if (prefix.isEmpty()) localName else "$prefix:$localName"
                    attributes.add(Attribute(rawName, "", value))
                }
            } else if (isNamespaceAware) {
                attributes.add(Attribute(localName, namespace, value))
            } else {
                attributes.add(Attribute(qualifiedName(prefix, localName), "", value))
            }
        }

        if (!isNamespaceAware) {
            // The generic reader reports the namespace declarations separately from the
            // attributes; reconstruct the raw `xmlns`/`xmlns:*` attributes from them.
            for (namespace in reader.namespaceDecls) {
                val rawName = if (namespace.prefix.isEmpty()) {
                    "xmlns"
                } else {
                    "xmlns:${namespace.prefix}"
                }
                if (attributes.none { it.name == rawName }) {
                    attributes.add(Attribute(rawName, "", namespace.namespaceURI))
                }
            }
        }

        val normalized =
            if (isCaseSensitive) {
                attributes
            } else {
                attributes.map { it.copy(name = it.name.lowercase()) }
            }

        return normalized
            .map(Attribute::namespace).distinct()
            .associateWith { ns ->
                normalized.filter { it.namespace == ns }.associate { Pair(it.name, it.value) }
            }
    }

    private fun qualifiedName(prefix: String, localName: String): String =
        if (prefix.isEmpty()) localName else "$prefix:$localName"
}

/**
 * Decodes the bytes of an XML document into a string, sniffing the character encoding from the
 * byte order mark or the XML declaration. Defaults to UTF-8.
 */
private fun ByteArray.decodeXmlString(): String {
    // Byte order marks.
    if (size >= 2) {
        if (this[0] == 0xFE.toByte() && this[1] == 0xFF.toByte()) {
            return decodeUtf16(offset = 2, bigEndian = true)
        }
        if (this[0] == 0xFF.toByte() && this[1] == 0xFE.toByte()) {
            return decodeUtf16(offset = 2, bigEndian = false)
        }
    }
    if (size >= 3 && this[0] == 0xEF.toByte() && this[1] == 0xBB.toByte() && this[2] == 0xBF.toByte()) {
        return decodeToString(startIndex = 3)
    }

    // UTF-16 without BOM, detected from the pattern of the `<` opening the XML declaration.
    if (size >= 2) {
        if (this[0] == 0.toByte() && this[1] == '<'.code.toByte()) {
            return decodeUtf16(offset = 0, bigEndian = true)
        }
        if (this[0] == '<'.code.toByte() && this[1] == 0.toByte()) {
            return decodeUtf16(offset = 0, bigEndian = false)
        }
    }

    // Encoding sniffed from the XML declaration. UTF-16 is not checked here: a UTF-16 document
    // is always caught earlier by its BOM or its `<` byte pattern.
    return when (sniffDeclaredEncoding()?.lowercase()) {
        "iso-8859-1", "latin1", "us-ascii", "ascii" -> buildString(size) {
            for (byte in this@decodeXmlString) {
                append((byte.toInt() and 0xFF).toChar())
            }
        }
        // UTF-8 is the default, and unknown encodings fall back to it leniently.
        else -> decodeToString()
    }
}

private fun ByteArray.sniffDeclaredEncoding(): String? {
    // The XML declaration, if any, is in the ASCII range at the very beginning of the document.
    val prolog = copyOfRange(0, size.coerceAtMost(200))
        .map { (it.toInt() and 0xFF).toChar() }
        .joinToString("")
        .substringAfter("<?xml", "")
        .substringBefore("?>")

    return Regex("""encoding\s*=\s*["']([^"']+)["']""")
        .find(prolog)
        ?.groupValues?.get(1)
}

private fun ByteArray.decodeUtf16(offset: Int, bigEndian: Boolean): String {
    val chars = CharArray((size - offset) / 2)
    for (i in chars.indices) {
        val high: Int
        val low: Int
        if (bigEndian) {
            high = this[offset + 2 * i].toInt() and 0xFF
            low = this[offset + 2 * i + 1].toInt() and 0xFF
        } else {
            high = this[offset + 2 * i + 1].toInt() and 0xFF
            low = this[offset + 2 * i].toInt() and 0xFF
        }
        chars[i] = ((high shl 8) or low).toChar()
    }
    return chars.concatToString()
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
        get() = getAttr("id") ?: getAttrNs("id", XML_NS_URI)

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
