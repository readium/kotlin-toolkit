/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.error.getOrThrow
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.resource.DirectoryContainerFactory
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.flatMap
import org.readium.r2.shared.util.Url
import org.readium.r2.streamer.readBlocking
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpubDeobfuscatorTest {

    private val identifier = "urn:uuid:36d5078e-ff7d-468e-a5f3-f47c14b91f2f"

    private val deobfuscationDir = requireNotNull(
        EpubDeobfuscatorTest::class.java
            .getResource("deobfuscation")
            ?.let { Url(it) }
    )

    private val container = runBlocking {
        requireNotNull(
            DirectoryContainerFactory().create(deobfuscationDir).getOrNull()
        )
    }

    private val font = requireNotNull(
        container.get("/cut-cut.woff").readBlocking().getOrNull()
    )

    private fun deobfuscate(path: String, algorithm: String?): Resource {
        val resource = container.get(path)

        val deobfuscator = EpubDeobfuscator(identifier) {
            if (resource.source == it) {
                algorithm?.let {
                    Encryption(algorithm = algorithm)
                }
            } else {
                null
            }
        }

        return resource.flatMap(deobfuscator::transform)
    }

    @Test
    fun testIdpfDeobfuscation() {
        val deobfuscatedRes = deobfuscate(
            "/cut-cut.obf.woff",
            "http://www.idpf.org/2008/embedding"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun testIdpfDeobfuscationWithRange() {
        runBlocking {
            val deobfuscatedRes = deobfuscate(
                "/cut-cut.obf.woff",
                "http://www.idpf.org/2008/embedding"
            ).read(20L until 40L).getOrThrow()
            assertThat(deobfuscatedRes).isEqualTo(font.copyOfRange(20, 40))
        }
    }

    @Test
    fun testAdobeDeobfuscation() {
        val deobfuscatedRes = deobfuscate(
            "/cut-cut.adb.woff",
            "http://ns.adobe.com/pdf/enc#RC"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun `a resource is passed through when the link doesn't contain encryption data`() {
        val deobfuscatedRes = deobfuscate(
            "/cut-cut.woff",
            null
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }

    @Test
    fun `a resource is passed through when the algorithm is unknown`() {
        val deobfuscatedRes = deobfuscate(
            "/cut-cut.woff",
            "unknown algorithm"
        ).readBlocking().getOrNull()
        assertThat(deobfuscatedRes).isEqualTo(font)
    }
}
