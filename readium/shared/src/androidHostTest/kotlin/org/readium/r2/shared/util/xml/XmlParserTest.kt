/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.xml

import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.InternalReadiumApi
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParserException

val metadatav3 = """
    <package xmlns="http://www.idpf.org/2007/opf" version="3.0" xml:lang="en" unique-identifier="pub-id">
        <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title id="title">Moby-Dick</dc:title>
            <meta refines="#title" property="title-type">main</meta>
            <meta refines="#title" property="alternate-script" xml:lang="fr">Moby Dick</meta>
            <dc:creator id="creator">Herman Melville</dc:creator>
        </metadata>
    </package>
    """

val metadatav2 = """
    <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="pub-id">
        <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
            <dc:title>La Maison Tellier</dc:title>
            <dc:creator opf:role="aut">Guy de Maupassant</dc:creator>
            <dc:identifier id="pub-id">urn:uuid:8A768A9F-5559-3BAA-84E4-D39A4D249D51</dc:identifier>
        </metadata>
    </package>    
"""

private fun parseXmlString(string: String, namespaceAware: Boolean = true): ElementNode {
    val parser = XmlParser(namespaceAware)
    val stream = ByteArrayInputStream(string.toByteArray(Charsets.UTF_8))
    return parser.parse(stream)
}

@RunWith(RobolectricTestRunner::class)
class XmlParserTest {
    @Test
    fun testNotNamespaceAwareV3() {
        val doc = parseXmlString(metadatav3, false)
        val expectedTitle = ElementNode(
            "dc:title",
            "",
            "en",
            mapOf("" to mapOf("id" to "title")),
            listOf(TextNode("Moby-Dick"))
        )
        val expectedTitleType = ElementNode(
            "meta",
            "",
            "en",
            mapOf("" to mapOf("refines" to "#title", "property" to "title-type")),
            listOf(TextNode("main"))
        )
        val expectedAltScript = ElementNode(
            "meta",
            "",
            "fr",
            mapOf(
                "" to mapOf(
                    "refines" to "#title",
                    "property" to "alternate-script",
                    "xml:lang" to "fr"
                )
            ),
            listOf(TextNode("Moby Dick"))
        )
        val expectedCreator = ElementNode(
            "dc:creator",
            "",
            "en",
            mapOf("" to mapOf("id" to "creator")),
            listOf(TextNode("Herman Melville"))
        )
        val expectedMetadata = ElementNode(
            "metadata",
            "",
            "en",
            mapOf("" to mapOf("xmlns:dc" to "http://purl.org/dc/elements/1.1/")),
            listOf(expectedTitle, expectedTitleType, expectedAltScript, expectedCreator)
        )
        assertEquals(doc.name, "package")
        assertEquals(doc.namespace, "")
        assertEquals(doc.getAttr("version"), "3.0")
        assertEquals(doc.getAttr("xml:lang"), "en")
        assertEquals(doc.lang, "en")
        val metadata = doc.getFirst("metadata", "") as ElementNode
        assertEquals(metadata.name, "metadata")
        assertEquals(metadata.namespace, "")
        assertEquals(metadata.attributes, expectedMetadata.attributes)
        assertEquals(metadata.lang, expectedMetadata.lang)
        assertEquals(metadata.children.filterIsInstance<ElementNode>(), expectedMetadata.children)
    }

    @Test
    fun testNamespaceAwareV3() {
        val doc = parseXmlString(metadatav3, true)
        val expectedTitle = ElementNode(
            "title",
            "http://purl.org/dc/elements/1.1/",
            "en",
            mapOf("" to mapOf("id" to "title")),
            listOf(TextNode("Moby-Dick"))
        )
        val expectedTitleType = ElementNode(
            "meta",
            "http://www.idpf.org/2007/opf",
            "en",
            mapOf("" to mapOf("refines" to "#title", "property" to "title-type")),
            listOf(TextNode("main"))
        )
        val expectedAltScript = ElementNode(
            "meta",
            "http://www.idpf.org/2007/opf",
            "fr",
            mapOf(
                "" to mapOf("refines" to "#title", "property" to "alternate-script"),
                XMLConstants.XML_NS_URI to mapOf("lang" to "fr")
            ),
            listOf(TextNode("Moby Dick"))
        )
        val expectedCreator = ElementNode(
            "creator",
            "http://purl.org/dc/elements/1.1/",
            "en",
            mapOf("" to mapOf("id" to "creator")),
            listOf(TextNode("Herman Melville"))
        )
        val expectedMetadata = ElementNode(
            "metadata",
            "http://www.idpf.org/2007/opf",
            "en",
            mapOf(),
            listOf(expectedTitle, expectedTitleType, expectedAltScript, expectedCreator)
        )
        assertEquals("package", doc.name)
        assertEquals("http://www.idpf.org/2007/opf", doc.namespace)
        assertEquals("3.0", doc.getAttr("version"))

        val metadata = doc.getFirst("metadata", "http://www.idpf.org/2007/opf") as ElementNode
        assertEquals("metadata", metadata.name)
        assertEquals("http://www.idpf.org/2007/opf", metadata.namespace)
        assertEquals(expectedMetadata.attributes, metadata.attributes)
        assertEquals(expectedMetadata.children, metadata.children.filterIsInstance<ElementNode>())
    }

    @Test
    fun testNamespaceAwareV2() {
        val doc = parseXmlString(metadatav2, true)
        val expectedTitle = ElementNode(
            "title",
            "http://purl.org/dc/elements/1.1/",
            "",
            mapOf(),
            listOf(TextNode("La Maison Tellier"))
        )
        val expectedCreator = ElementNode(
            "creator",
            "http://purl.org/dc/elements/1.1/",
            "",
            mapOf("http://www.idpf.org/2007/opf" to mapOf("role" to "aut")),
            listOf(TextNode("Guy de Maupassant"))
        )
        val expectedIdentifier = ElementNode(
            "identifier",
            "http://purl.org/dc/elements/1.1/",
            "",
            mapOf("" to mapOf("id" to "pub-id")),
            listOf(TextNode("urn:uuid:8A768A9F-5559-3BAA-84E4-D39A4D249D51"))
        )
        val expectedMetadata = ElementNode(
            "metadata",
            "http://www.idpf.org/2007/opf",
            "",
            mapOf(),
            listOf(expectedTitle, expectedCreator, expectedIdentifier)
        )
        assertEquals("package", doc.name)
        assertEquals("http://www.idpf.org/2007/opf", doc.namespace)
        assertEquals("2.0", doc.getAttr("version"))

        val metadata = doc.getFirst("metadata", "http://www.idpf.org/2007/opf") as ElementNode
        assertEquals("metadata", metadata.name)
        assertEquals("http://www.idpf.org/2007/opf", metadata.namespace)
        assertEquals(expectedMetadata.attributes, metadata.attributes)
        assertEquals(expectedMetadata.children, metadata.children.filterIsInstance<ElementNode>())
    }

    @Test(expected = XmlPullParserException::class)
    fun `An input with multiple roots raises an exception`() {
        parseXmlString(metadatav2 + metadatav3)
    }

    @Test(expected = XmlPullParserException::class)
    fun `An input with no root raises an exception`() {
        parseXmlString("   \n    \n")
    }

    @Test
    fun `CDATA parsed rightly`() {
        val doc = parseXmlString(
            """
            <text>
                pre text <![CDATA["Some text like <, >, & are safe here"]]> post text
            </text>
            """.trimIndent()
        )

        val cdata = doc.children.first() as TextNode

        assertEquals("pre text \"Some text like <, >, & are safe here\" post text", cdata.text.trim())
    }
}

@RunWith(RobolectricTestRunner::class)
class ElementNodeTest {
    @Test
    fun testCollectText() {
        val doc = parseXmlString(
            """    
            <html>
                <body>
                    <p>Premier paragraphe</p>
                    <section>
                        <p> Un plus long
                            paragraphe </p>
                    </section>
                    <article>
                        <h1> Ici un titre </h1>
                    </article>
                </body>
            </html>
            
        """
        )
        val text = doc.collectText().replace("\\s+".toRegex(), " ").trim()
        assertEquals("Premier paragraphe Un plus long paragraphe Ici un titre", text)
    }

    @Test
    fun testCollect() {
        val doc = parseXmlString(
            """    
            <html>
                <body>
                    <section>
                        <nav></nav>
                    </section>
                    <article>
                        <section>
                            <nav></nav>
                        </section>
                    </article>
                    <nav></nav>
                </body>
            </html>
            
        """
        )
        val navNode = ElementNode("nav", "")
        assertEquals(List(3) { _ -> navNode }, doc.collect("nav", ""))
    }
}
