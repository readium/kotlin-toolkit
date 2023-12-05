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
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdeptFallbackContentProtectionTest {

    @Test
    fun `Sniff no content protection`() {
        assertFalse(supports(mediaType = MediaType.EPUB, resources = emptyMap()).checkSuccess())
    }

    @Test
    fun `Sniff EPUB with empty encryption xml`() {
        assertFalse(
            supports(
                mediaType = MediaType.EPUB,
                resources = mapOf(
                    "META-INF/encryption.xml" to """<?xml version='1.0' encoding='utf-8'?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#"></encryption>"""
                )
            ).checkSuccess()
        )
    }

    @Test
    fun `Sniff Adobe ADEPT`() {
        assertTrue(
            supports(
                mediaType = MediaType.EPUB,
                resources = mapOf(
                    "META-INF/encryption.xml" to """<?xml version="1.0"?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <EncryptedData xmlns="http://www.w3.org/2001/04/xmlenc#">
    <EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"></EncryptionMethod>
    <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#">
      <resource xmlns="http://ns.adobe.com/adept">urn:uuid:2c43729c-b985-4531-8e86-ae75ce5e5da9</resource>
    </KeyInfo>
    <CipherData>
      <CipherReference URI="OEBPS/stylesheet.css"></CipherReference>
    </CipherData>
  </EncryptedData>
  </encryption>"""
                )
            ).checkSuccess()
        )
    }

    @Test
    fun `Sniff Adobe ADEPT from rights xml`() {
        assertTrue(
            supports(
                mediaType = MediaType.EPUB,
                resources = mapOf(
                    "META-INF/encryption.xml" to """<?xml version='1.0' encoding='utf-8'?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#"></encryption>""",
                    "META-INF/rights.xml" to """<?xml version="1.0"?><adept:rights xmlns:adept="http://ns.adobe.com/adept"></adept:rights>"""
                )
            ).checkSuccess()
        )
    }

    private fun supports(mediaType: MediaType, resources: Map<String, String>): Try<Boolean, ReadError> = runBlocking {
        AdeptFallbackContentProtection().supports(
            ContainerAsset(
                mediaType = mediaType,
                container = TestContainer(resources.mapKeys { Url(it.key)!! })
            )
        )
    }
}
