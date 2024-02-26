/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import java.io.File
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.file.DirectoryContainer
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.streamer.readBlocking
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpubDeobfuscatorTest {

    private val identifier = "urn:uuid:36d5078e-ff7d-468e-a5f3-f47c14b91f2f"

    private val deobfuscationDir = requireNotNull(
        EpubDeobfuscatorTest::class.java
            .getResource("deobfuscation")
            ?.path
            ?.let { File(it) }
    )

    private val container = runBlocking {
        DirectoryContainer(deobfuscationDir).checkSuccess()
    }

    private val font = requireNotNull(container[Url("cut-cut.woff")!!])
        .readBlocking()
        .checkSuccess()

    private fun deobfuscate(url: Url, resource: Resource, algorithm: String?): Resource {
        val encryptionData =
            if (algorithm != null) {
                mapOf(url to Encryption(algorithm = algorithm))
            } else {
                emptyMap()
            }

        val deobfuscator = EpubDeobfuscator(identifier, encryptionData)

        return deobfuscator.transform(url, resource)
    }

    @Test
    fun testIdpfDeobfuscation() {
        val url = Url("cut-cut.obf.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            "http://www.idpf.org/2008/embedding"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun testIdpfDeobfuscationWithRange() {
        runBlocking {
            val url = Url("cut-cut.obf.woff")!!
            val resource = assertNotNull(container[url])
            val deobfuscatedRes = deobfuscate(
                url,
                resource,
                "http://www.idpf.org/2008/embedding"
            ).read(20L until 40L).checkSuccess()
            assertThat(deobfuscatedRes).isEqualTo(font.copyOfRange(20, 40))
        }
    }

    @Test
    fun testAdobeDeobfuscation() {
        val url = Url("cut-cut.adb.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            "http://ns.adobe.com/pdf/enc#RC"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun `a resource is passed through when the link doesn't contain encryption data`() {
        val url = Url("cut-cut.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            null
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun `a resource is passed through when the algorithm is unknown`() {
        val url = Url("cut-cut.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            "unknown algorithm"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }
}
