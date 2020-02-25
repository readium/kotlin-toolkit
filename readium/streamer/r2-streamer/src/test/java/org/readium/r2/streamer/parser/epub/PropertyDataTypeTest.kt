/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test

class ParsePrefixesTest {
    @Test
    fun `A single prefix is rightly parsed`() {
        val prefixes = parsePrefixes("foaf: http://xmlns.com/foaf/spec/")
        assertThat(prefixes).contains(
            entry("foaf", "http://xmlns.com/foaf/spec/")
        )
        assertThat(prefixes).hasSize(1)
    }

    @Test
    fun `Space between prefixes and iris can be ommited`() {
        val prefixes = parsePrefixes("foaf: http://xmlns.com/foaf/spec/ dbp:http://dbpedia.org/ontology/")
        assertThat(prefixes).contains(
            entry("foaf", "http://xmlns.com/foaf/spec/"),
            entry("dbp", "http://dbpedia.org/ontology/")
        )
        assertThat(prefixes).hasSize(2)
    }

    @Test
    fun `Multiple prefixes are rightly parsed`() {
        val prefixes = parsePrefixes("foaf: http://xmlns.com/foaf/spec/ dbp: http://dbpedia.org/ontology/")
        assertThat(prefixes).contains(
            entry("foaf", "http://xmlns.com/foaf/spec/"),
            entry("dbp", "http://dbpedia.org/ontology/")
        )
        assertThat(prefixes).hasSize(2)
    }

    @Test
    fun `Different prefixes can be separated by new lines`() {
        @Test
        fun `Multiple prefixes are rightly parsed`() {
            val prefixes = parsePrefixes(
                """foaf: http://xmlns.com/foaf/spec/
                dbp: http://dbpedia.org/ontology/"""
            )
            assertThat(prefixes).contains(
                entry("foaf", "http://xmlns.com/foaf/spec/"),
                entry("dbp", "http://dbpedia.org/ontology/")
            )
            assertThat(prefixes).hasSize(2)
        }
    }

    @Test
    fun `Empty string is rightly handled`() {
        assertThat(parsePrefixes("")).isEmpty()
    }
}

class TestResolveProperty {
    @Test
    fun `Default vocabularies are used`() {
        assertThat(resolveProperty("nav", PACKAGE_RESERVED_PREFIXES, DEFAULT_VOCAB.ITEM))
            .isEqualTo("http://idpf.org/epub/vocab/package/item/#nav")
    }

    @Test
    fun `The prefix map has highest priority`() {
        assertThat(resolveProperty("media:narrator", PACKAGE_RESERVED_PREFIXES, DEFAULT_VOCAB.META))
            .isEqualTo("http://www.idpf.org/epub/vocab/overlays/#narrator")
    }

    @Test
    fun `Return null when the prefix is unknown`() {
        assertThat(resolveProperty("unknown:narrator", PACKAGE_RESERVED_PREFIXES, DEFAULT_VOCAB.META))
            .isNull()
    }

    @Test
    fun `Empty string is rightly handled`() {
        assertThat(resolveProperty("", mapOf())).isNull()
    }
}

class ParsePropertiesTest {
    @Test
    fun `Various white spaces are accepted`() {
        val properties = """
            rendition:flow-auto        rendition:layout-pre-paginated             
                 rendition:orientation-auto
        """
        assertThat(parseProperties(properties)).containsExactly(
            "rendition:flow-auto",
            "rendition:layout-pre-paginated",
            "rendition:orientation-auto"
        )
    }

    @Test
    fun `Empty string is rightly handled`() {
        assertThat(parseProperties("")).isEmpty()
    }
}