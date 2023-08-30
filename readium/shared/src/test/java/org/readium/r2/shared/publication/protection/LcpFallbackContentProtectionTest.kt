/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LcpFallbackContentProtectionTest {

    @Test
    fun `Sniff no content protection`() {
        assertFalse(supports(mediaType = MediaType.EPUB, resources = emptyMap()))
    }

    @Test
    fun `Sniff EPUB with empty encryption xml`() {
        assertFalse(
            supports(
                mediaType = MediaType.EPUB,
                resources = mapOf(
                    "META-INF/encryption.xml" to """<?xml version='1.0' encoding='utf-8'?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#"></encryption>"""
                )
            )
        )
    }

    @Test
    fun `Sniff LCP protected Readium package`() {
        assertTrue(
            supports(
                mediaType = MediaType.READIUM_WEBPUB,
                resources = mapOf(
                    "license.lcpl" to "{}"
                )
            )
        )
    }

    @Test
    fun `Sniff LCP protected EPUB`() {
        assertTrue(
            supports(
                mediaType = MediaType.EPUB,
                resources = mapOf(
                    "META-INF/license.lcpl" to "{}"
                )
            )
        )
    }

    @Test
    fun `Sniff LCP protected EPUB missing the license`() {
        assertTrue(
            supports(
                mediaType = MediaType.EPUB,
                resources = mapOf(
                    "META-INF/encryption.xml" to """<?xml version="1.0" encoding="UTF-8"?>
<encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <EncryptedData xmlns="http://www.w3.org/2001/04/xmlenc#">
    <EncryptionMethod xmlns="http://www.w3.org/2001/04/xmlenc#" Algorithm="http://www.w3.org/2001/04/xmlenc#aes256-cbc"></EncryptionMethod>
    <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#">
      <RetrievalMethod xmlns="http://www.w3.org/2000/09/xmldsig#" URI="license.lcpl#/encryption/content_key" Type="http://readium.org/2014/01/lcp#EncryptedContentKey"></RetrievalMethod>
    </KeyInfo>
    <CipherData xmlns="http://www.w3.org/2001/04/xmlenc#">
      <CipherReference xmlns="http://www.w3.org/2001/04/xmlenc#" URI="OPS/chapter_001.xhtml"></CipherReference>
    </CipherData>
    <EncryptionProperties xmlns="http://www.w3.org/2001/04/xmlenc#">
      <EncryptionProperty xmlns="http://www.w3.org/2001/04/xmlenc#">
        <Compression xmlns="http://www.idpf.org/2016/encryption#compression" Method="8" OriginalLength="13877"></Compression>
      </EncryptionProperty>
    </EncryptionProperties>
  </EncryptedData>
</encryption>"""
                )
            )
        )
    }

    private fun supports(mediaType: MediaType, resources: Map<String, String>): Boolean = runBlocking {
        LcpFallbackContentProtection(MediaTypeRetriever()).supports(
            Asset.Container(
                mediaType = mediaType,
                exploded = false,
                container = TestContainer(resources)
            )
        )
    }
}
