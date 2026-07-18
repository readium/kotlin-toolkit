/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParsePrefixesTest {
    @Test
    fun `A single prefix is rightly parsed`() {
        val prefixes = parsePrefixes("foaf: http://xmlns.com/foaf/spec/")
        assertEquals(
            mapOf("foaf" to "http://xmlns.com/foaf/spec/"),
            prefixes
        )
    }

    @Test
    fun `Space between prefixes and iris can be ommited`() {
        val prefixes = parsePrefixes(
            "foaf: http://xmlns.com/foaf/spec/ dbp:http://dbpedia.org/ontology/"
        )
        assertEquals(
            mapOf(
                "foaf" to "http://xmlns.com/foaf/spec/",
                "dbp" to "http://dbpedia.org/ontology/"
            ),
            prefixes
        )
    }

    @Test
    fun `Multiple prefixes are rightly parsed`() {
        val prefixes = parsePrefixes(
            "foaf: http://xmlns.com/foaf/spec/ dbp: http://dbpedia.org/ontology/"
        )
        assertEquals(
            mapOf(
                "foaf" to "http://xmlns.com/foaf/spec/",
                "dbp" to "http://dbpedia.org/ontology/"
            ),
            prefixes
        )
    }

    @Test
    fun `Different prefixes can be separated by new lines`() {
        val prefixes = parsePrefixes(
            """foaf: http://xmlns.com/foaf/spec/
            dbp: http://dbpedia.org/ontology/"""
        )
        assertEquals(
            mapOf(
                "foaf" to "http://xmlns.com/foaf/spec/",
                "dbp" to "http://dbpedia.org/ontology/"
            ),
            prefixes
        )
    }

    @Test
    fun `Empty string is rightly handled`() {
        assertTrue(parsePrefixes("").isEmpty())
    }
}

class TestResolveProperty {
    @Test
    fun `Default vocabularies are used`() {
        assertEquals(
            "http://idpf.org/epub/vocab/package/item/#nav",
            resolveProperty("nav", PACKAGE_RESERVED_PREFIXES, DEFAULT_VOCAB.ITEM)
        )
    }

    @Test
    fun `The prefix map has highest priority`() {
        assertEquals(
            "http://www.idpf.org/epub/vocab/overlays/#narrator",
            resolveProperty("media:narrator", PACKAGE_RESERVED_PREFIXES, DEFAULT_VOCAB.META)
        )
    }
}

class ParsePropertiesTest {
    @Test
    fun `Various white spaces are accepted`() {
        val properties = """
            rendition:flow-auto        rendition:layout-pre-paginated             
                 rendition:orientation-auto
        """
        assertEquals(
            listOf(
                "rendition:flow-auto",
                "rendition:layout-pre-paginated",
                "rendition:orientation-auto"
            ),
            parseProperties(properties)
        )
    }

    @Test
    fun `Empty string is rightly handled`() {
        assertTrue(parseProperties("").isEmpty())
    }
}
