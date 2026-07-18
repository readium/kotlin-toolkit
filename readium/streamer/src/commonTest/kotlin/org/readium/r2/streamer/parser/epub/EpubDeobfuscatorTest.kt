/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.DirectoryContainer
import org.readium.r2.shared.util.fromFilePath
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.streamer.Fixtures

class EpubDeobfuscatorTest {

    private val identifier = "urn:uuid:36d5078e-ff7d-468e-a5f3-f47c14b91f2f"

    private val deobfuscationDir = Fixtures("epub").path("deobfuscation")

    private suspend fun container(): Container<Resource> =
        DirectoryContainer(
            checkNotNull(
                AbsoluteUrl.fromFilePath(deobfuscationDir.toString(), isDirectory = true)
            )
        ).checkSuccess()

    private suspend fun font(container: Container<Resource>): ByteArray =
        checkNotNull(container[Url("cut-cut.woff")!!])
            .read()
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
    fun testIdpfDeobfuscation() = runTest {
        val container = container()
        val url = Url("cut-cut.obf.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            "http://www.idpf.org/2008/embedding"
        ).read().getOrNull()
        assertContentEquals(font(container), deobfuscatedRes)
    }

    @Test
    fun testIdpfDeobfuscationWithRange() = runTest {
        val container = container()
        val url = Url("cut-cut.obf.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            "http://www.idpf.org/2008/embedding"
        ).read(20L until 40L).checkSuccess()
        assertContentEquals(font(container).copyOfRange(20, 40), deobfuscatedRes)
    }

    @Test
    fun testAdobeDeobfuscation() = runTest {
        val container = container()
        val url = Url("cut-cut.adb.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            "http://ns.adobe.com/pdf/enc#RC"
        ).read().getOrNull()
        assertContentEquals(font(container), deobfuscatedRes)
    }

    @Test
    fun `a resource is passed through when the link doesn't contain encryption data`() = runTest {
        val container = container()
        val url = Url("cut-cut.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            null
        ).read().getOrNull()
        assertContentEquals(font(container), deobfuscatedRes)
    }

    @Test
    fun `a resource is passed through when the algorithm is unknown`() = runTest {
        val container = container()
        val url = Url("cut-cut.woff")!!
        val resource = assertNotNull(container[url])
        val deobfuscatedRes = deobfuscate(
            url,
            resource,
            "unknown algorithm"
        ).read().getOrNull()
        assertContentEquals(font(container), deobfuscatedRes)
    }
}
