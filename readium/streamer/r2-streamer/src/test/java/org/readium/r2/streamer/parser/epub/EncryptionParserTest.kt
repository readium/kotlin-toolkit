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
import org.readium.r2.shared.Encryption
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.parser.xml.XmlParser

class EncryptionParserTest {
    fun parseEncryption(path: String) : Map<String, Encryption> {
        val res = EncryptionParserTest::class.java.getResourceAsStream(path)
        check(res != null)
        val document = XmlParser().parse(res)
        return EncryptionParser.parse(document, DRM(DRM.Brand.lcp))
    }

    val lcpChap1 = entry("/chapter01.xhtml", Encryption().apply {
        algorithm = "http://www.w3.org/2001/04/xmlenc#aes256-cbc"
        compression = "deflate"
        originalLength = 13291
        profile = null
        scheme = DRM.Scheme.lcp
    })


    val lcpChap2 =  entry("/dir/chapter02.xhtml", Encryption().apply {
        algorithm = "http://www.w3.org/2001/04/xmlenc#aes256-cbc"
        compression = "none"
        originalLength = 12914
        profile = null
        scheme = DRM.Scheme.lcp
    })

    @Test
    fun `Check EncryptionParser with namespace prefixes`() {
        assertThat(parseEncryption("encryption/encryption-lcp-prefixes.xml")).contains(lcpChap1, lcpChap2)
    }

    @Test
    fun `Check EncryptionParser with default namespaces`() {
        assertThat(parseEncryption("encryption/encryption-lcp-xmlns.xml")).contains(lcpChap1, lcpChap2)
    }

    @Test
    fun `Check EncryptionParser with unknown retrieval method`() {
        assertThat(parseEncryption("encryption/encryption-unknown-method.xml")).contains(
            entry("/html/chapter.html", Encryption().apply {
                algorithm = "http://www.w3.org/2001/04/xmlenc#kw-aes128"
                compression = "deflate"
                originalLength = 12914
                profile = null
                scheme = null }),
            entry("/images/image.jpeg", Encryption().apply {
                algorithm = "http://www.w3.org/2001/04/xmlenc#kw-aes128"
                compression = null
                originalLength = null
                profile = null
                scheme = null
            })
        )
    }
}