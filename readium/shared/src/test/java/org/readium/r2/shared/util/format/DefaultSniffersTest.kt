/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultSniffersTest {

    private val epubFormat = Format(
        specification = FormatSpecification(Specification.Zip, Specification.Epub),
        mediaType = MediaType.EPUB,
        fileExtension = FileExtension("epub")
    )

    @Test
    fun `EpubDrmSniffer doesn't recognize EPUB with empty encryption xml`() = runBlocking {
        assertEquals(
            epubFormat,
            EpubDrmSniffer.sniffContainer(
                format = epubFormat,
                container = TestContainer(
                    Url("META-INF/encryption.xml")!! to """<?xml version='1.0' encoding='utf-8'?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#"></encryption>"""
                )
            ).checkSuccess()
        )
    }

    @Test
    fun `Sniff Adobe ADEPT`() = runBlocking {
        assertEquals(
            epubFormat.copy(specification = epubFormat.specification + Specification.Adept),
            EpubDrmSniffer.sniffContainer(
                format = epubFormat,
                container = TestContainer(
                    Url("META-INF/encryption.xml")!! to """<?xml version="1.0"?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
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
    fun `Sniff Adobe ADEPT from rights xml`() = runBlocking {
        assertEquals(
            epubFormat.copy(specification = epubFormat.specification + Specification.Adept),
            EpubDrmSniffer.sniffContainer(
                format = epubFormat,
                container = TestContainer(
                    Url("META-INF/encryption.xml")!! to """<?xml version='1.0' encoding='utf-8'?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#"></encryption>""",
                    Url("META-INF/rights.xml")!! to """<?xml version="1.0"?><adept:rights xmlns:adept="http://ns.adobe.com/adept"></adept:rights>"""
                )
            ).checkSuccess()
        )
    }

    @Test
    fun `Sniff LCP protected EPUB`() = runBlocking {
        assertEquals(
            epubFormat.copy(specification = epubFormat.specification + Specification.Lcp),
            EpubDrmSniffer.sniffContainer(
                format = epubFormat,
                container = TestContainer(Url("META-INF/license.lcpl")!! to "{}")
            ).checkSuccess()
        )
    }

    @Test
    fun `Sniff LCP protected EPUB missing the license`() = runBlocking {
        assertEquals(
            epubFormat.copy(specification = epubFormat.specification + Specification.Lcp),
            EpubDrmSniffer.sniffContainer(
                format = epubFormat,
                container = TestContainer(
                    Url("META-INF/encryption.xml")!! to """<?xml version="1.0" encoding="UTF-8"?>
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
            ).checkSuccess()
        )
    }
}
