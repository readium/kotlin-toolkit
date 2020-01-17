/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.parser.xml.parser.org.readium.r2.shared.parser.xml

import org.junit.Test
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.TextNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayInputStream

val metadatav3 = """
    <package xmlns="http://www.idpf.org/2007/opf" version="3.0" xml:lang="en" unique-identifier="pub-id">
        <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title id="title">Moby-Dick</dc:title>
            <meta refines="#title" property="title-type">main</meta>
            <dc:creator id="creator">Herman Melville</dc:creator>
        </metadata>
    </package>
    """

val metadatav2 = """
    <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="BookId">
        <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
            <dc:title>La Maison Tellier</dc:title>
            <dc:creator opf:role="aut">Guy de Maupassant</dc:creator>
            <dc:identifier id="BookId">urn:uuid:8A768A9F-5559-3BAA-84E4-D39A4D249D51</dc:identifier>
        </metadata>
    </package>    
"""

class XmlParserTest {
    @Test
    fun testNotNamespaceAwareV3() {
        val parser = XmlParser(false)
        val stream = ByteArrayInputStream(metadatav3.toByteArray(Charsets.UTF_8))
        val doc = parser.parse(stream)
        val expectedTitle = ElementNode(
                "dc:title",
                "",
                mapOf("" to mapOf("id" to "title")),
                listOf(TextNode("Moby-Dick"))
        )
        val expectedMeta = ElementNode(
                "meta",
                "",
                mapOf("" to mapOf("refines" to "#title", "property" to "title-type")),
                listOf(TextNode("main"))
        )
        val expectedCreator = ElementNode(
                "dc:creator",
                "",
                mapOf("" to mapOf("id" to "creator")),
                listOf(TextNode("Herman Melville"))
        )
        val expectedMetadata = ElementNode(
                "metadata",
                "",
                mapOf("" to mapOf("xmlns:dc" to "http://purl.org/dc/elements/1.1/")),
                listOf(expectedTitle, expectedMeta, expectedCreator)
        )
        assert(doc.name == "package")
        assert(doc.namespace == "")
        assert(doc.getAttr("version") == "3.0")
        val metadata = doc.getFirst("metadata", "") as ElementNode
        assert(metadata.name == "metadata")
        assert(metadata.namespace == "")
        assert(metadata.attributes == expectedMetadata.attributes)
        assert(metadata.children.filterIsInstance<ElementNode>() == expectedMetadata.children)
    }

    @Test
    fun testNamespaceAwareV3() {
        val parser = XmlParser(true)
        val stream = ByteArrayInputStream(metadatav3.toByteArray(Charsets.UTF_8))
        val doc = parser.parse(stream)
        val expectedTitle = ElementNode(
                "title",
                "http://purl.org/dc/elements/1.1/",
                mapOf("" to mapOf("id" to "title")),
                listOf(TextNode("Moby-Dick"))
        )
        val expectedMeta = ElementNode(
                "meta",
            "http://www.idpf.org/2007/opf",
                    mapOf("" to mapOf("refines" to "#title", "property" to "title-type")),
                    listOf(TextNode("main"))
        )
        val expectedCreator = ElementNode(
                "creator",
                "http://purl.org/dc/elements/1.1/",
                mapOf("" to mapOf("id" to "creator")),
                listOf(TextNode("Herman Melville"))
        )
        val expectedMetadata = ElementNode(
                "metadata",
                "http://www.idpf.org/2007/opf",
                mapOf(),
                listOf(expectedTitle, expectedMeta, expectedCreator)
        )
        assert(doc.name == "package")
        assert(doc.namespace == "http://www.idpf.org/2007/opf")
        assert(doc.getAttr("version") == "3.0")
        val metadata = doc.getFirst("metadata", "http://www.idpf.org/2007/opf") as ElementNode
        assert(metadata.name == "metadata")
        assert(metadata.namespace == "http://www.idpf.org/2007/opf")
        assert(metadata.attributes == expectedMetadata.attributes)
        assert(metadata.children.filterIsInstance<ElementNode>() == expectedMetadata.children)
    }

    @Test
    fun testNamespaceAwareV2() {
        val parser = XmlParser(true)
        val stream = ByteArrayInputStream(metadatav2.toByteArray(Charsets.UTF_8))
        val doc = parser.parse(stream)
        val expectedTitle = ElementNode(
                "title",
                "http://purl.org/dc/elements/1.1/",
                mapOf(),
                listOf(TextNode("La Maison Tellier"))
        )
        val expectedCreator = ElementNode(
                "creator",
                "http://purl.org/dc/elements/1.1/",
                mapOf("http://www.idpf.org/2007/opf" to mapOf("role" to "aut")),
                listOf(TextNode("Guy de Maupassant"))
        )
        val expectedIdentifier = ElementNode(
                "identifier",
                "http://purl.org/dc/elements/1.1/",
                mapOf("" to mapOf("id" to "BookId")),
                listOf(TextNode("urn:uuid:8A768A9F-5559-3BAA-84E4-D39A4D249D51"))
        )
        val expectedMetadata = ElementNode(
                "metadata",
                "http://www.idpf.org/2007/opf",
                mapOf(),
                listOf(expectedTitle, expectedCreator, expectedIdentifier)
        )
        assert(doc.name == "package")
        assert(doc.namespace == "http://www.idpf.org/2007/opf")
        assert(doc.getAttr("version") == "2.0")
        val metadata = doc.getFirst("metadata", "http://www.idpf.org/2007/opf") as ElementNode
        assert(metadata.name == "metadata")
        assert(metadata.namespace == "http://www.idpf.org/2007/opf")
        assert(metadata.attributes == expectedMetadata.attributes)
        assert(metadata.children.filterIsInstance<ElementNode>() == expectedMetadata.children)
    }

    @Test(expected= XmlPullParserException::class)
    fun testMultipleRoots() {
        val parser = XmlParser()
        val multipleRoots = metadatav2 + metadatav3
        val stream = ByteArrayInputStream(multipleRoots.toByteArray(Charsets.UTF_8))
        val doc = parser.parse(stream)
    }

    @Test(expected= XmlPullParserException::class)
    fun testNoRoot() {
        val parser = XmlParser()
        val noRoot = "   \n    \n"
        val stream = ByteArrayInputStream(noRoot.toByteArray(Charsets.UTF_8))
        val doc = parser.parse(stream)
    }
}