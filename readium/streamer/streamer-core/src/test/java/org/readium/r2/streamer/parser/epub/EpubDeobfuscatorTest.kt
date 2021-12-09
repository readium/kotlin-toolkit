/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.streamer.readBlocking
import java.io.File
import kotlin.test.assertNotNull

class EpubDeobfuscatorTest {

    private val identifier = "urn:uuid:36d5078e-ff7d-468e-a5f3-f47c14b91f2f"
    private val transformer = EpubDeobfuscator(identifier)
    private val fetcher: Fetcher
    private val font: ByteArray

    init {
        val deobfuscationDir = EpubDeobfuscatorTest::class.java
            .getResource("deobfuscation/cut-cut.woff")
            ?.path
            ?.let { File(it).parentFile }
        assertNotNull(deobfuscationDir)
        fetcher = FileFetcher("/deobfuscation", deobfuscationDir)

        val fontResult = fetcher.get(Link(href = "/deobfuscation/cut-cut.woff")).readBlocking()
        assert(fontResult.isSuccess)
        font = fontResult.getOrThrow()
    }

    private fun deobfuscate(href: String, algorithm: String?): Resource {
        val encryption = algorithm?.let {
            Encryption(
                algorithm = algorithm
            ).toJSON().toMap()
        }
        val properties = encryption?.let {
            mapOf( "encrypted" to it)
        }.orEmpty()

        val obfuscatedRes = fetcher.get(
            Link(
                href = href,
                properties = Properties(properties)
            )
        )
        return transformer.transform(obfuscatedRes)
    }

    @Test
    fun testIdpfDeobfuscation() {
        val deobfuscatedRes = deobfuscate(
            "/deobfuscation/cut-cut.obf.woff",
            "http://www.idpf.org/2008/embedding"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun testAdobeDeobfuscation() {
        val deobfuscatedRes = deobfuscate(
            "/deobfuscation/cut-cut.adb.woff",
            "http://ns.adobe.com/pdf/enc#RC"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun `a resource is passed through when the link doesn't contain encryption data`() {
        val deobfuscatedRes = deobfuscate(
            "/deobfuscation/cut-cut.woff",
            null
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun `a resource is passed through when the algorithm is unknown`() {
        val deobfuscatedRes = deobfuscate(
            "/deobfuscation/cut-cut.woff",
            "unknown algorithm"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

}