/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.epub

import kotlin.test.Test
import kotlin.test.assertEquals
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.xml.XmlParser

class EncryptionParserTest {

    private val fixtures = Fixtures("publication")

    private fun parseEncryption(path: String): Map<Url, Encryption> {
        val bytes = fixtures.read(path).toByteArray()
        val document = XmlParser().parse(bytes)
        return EpubEncryptionParser.parse(document)
    }

    private fun assertContains(map: Map<Url, Encryption>, vararg entries: Pair<Url, Encryption>) {
        for ((key, value) in entries) {
            assertEquals(value, map[key], "unexpected encryption for $key")
        }
    }

    val lcpChap1 = Url("OEBPS/xhtml/chapter01.xhtml")!! to Encryption(
        algorithm = "http://www.w3.org/2001/04/xmlenc#aes256-cbc",
        compression = "deflate",
        originalLength = 13291,
        profile = null,
        scheme = "http://readium.org/2014/01/lcp"
    )

    val lcpChap2 = Url("OEBPS/xhtml/chapter02.xhtml")!! to Encryption(
        algorithm = "http://www.w3.org/2001/04/xmlenc#aes256-cbc",
        compression = "none",
        originalLength = 12914,
        profile = null,
        scheme = "http://readium.org/2014/01/lcp"
    )

    @Test
    fun `Check EncryptionParser with namespace prefixes`() {
        assertContains(
            parseEncryption("encryption/encryption-lcp-prefixes.xml"),
            lcpChap1,
            lcpChap2
        )
    }

    @Test
    fun `Check EncryptionParser with default namespaces`() {
        assertContains(
            parseEncryption("encryption/encryption-lcp-xmlns.xml"),
            lcpChap1,
            lcpChap2
        )
    }

    @Test
    fun `Check EncryptionParser with unknown retrieval method`() {
        assertContains(
            parseEncryption("encryption/encryption-unknown-method.xml"),
            Url("OEBPS/xhtml/chapter.xhtml")!! to Encryption(
                algorithm = "http://www.w3.org/2001/04/xmlenc#kw-aes128",
                compression = "deflate",
                originalLength = 12914,
                profile = null,
                scheme = null
            ),
            Url("OEBPS/images/image.jpeg")!! to Encryption(
                algorithm = "http://www.w3.org/2001/04/xmlenc#kw-aes128",
                compression = null,
                originalLength = null,
                profile = null,
                scheme = null
            )
        )
    }
}
