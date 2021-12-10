package org.readium.r2.streamer.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.ContentProtection.Scheme
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FallbackContentProtectionTest {

    @Test
    fun `Sniff no content protection`() {
        assertNull(sniff(mediaType = MediaType.EPUB, resources = emptyMap()))
    }

    @Test
    fun `Sniff EPUB with empty encryption xml`() {
        assertNull(sniff(mediaType = MediaType.EPUB, resources = mapOf(
            "/META-INF/encryption.xml" to """<?xml version='1.0' encoding='utf-8'?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#"></encryption>"""
        )))
    }

    @Test
    fun `Sniff LCP protected package`() {
        assertEquals(Scheme.Lcp, sniff(
            mediaType = MediaType.ZIP,
            resources = mapOf(
                "/license.lcpl" to "{}"
            )
        ))
    }

    @Test
    fun `Sniff LCP protected EPUB`() {
        assertEquals(Scheme.Lcp, sniff(
            mediaType = MediaType.EPUB,
            resources = mapOf(
                "/META-INF/license.lcpl" to "{}"
            )
        ))
    }

    @Test
    fun `Sniff LCP protected EPUB missing the license`() {
        assertEquals(Scheme.Lcp, sniff(
            mediaType = MediaType.EPUB,
            resources = mapOf(
                "/META-INF/encryption.xml" to """<?xml version="1.0" encoding="UTF-8"?>
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
        ))
    }

    @Test
    fun `Sniff Adobe ADEPT`() {
        assertEquals(Scheme.Adept, sniff(
            mediaType = MediaType.EPUB,
            resources = mapOf(
                "/META-INF/encryption.xml" to """<?xml version="1.0"?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
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
        ))
    }

    @Test
    fun `Sniff Adobe ADEPT from rights xml`() {
        assertEquals(Scheme.Adept, sniff(
            mediaType = MediaType.EPUB,
            resources = mapOf(
                "/META-INF/encryption.xml" to """<?xml version='1.0' encoding='utf-8'?><encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#"></encryption>""",
                "/META-INF/rights.xml" to """<?xml version="1.0"?><adept:rights xmlns:adept="http://ns.adobe.com/adept"></adept:rights>"""
            )
        ))
    }

    private fun sniff(mediaType: MediaType, resources: Map<String, String>): Scheme? = runBlocking {
        FallbackContentProtection().sniffScheme(
            fetcher = TestFetcher(resources),
            mediaType = mediaType
        )
    }
}

class TestFetcher(private val resources: Map<String, String> = emptyMap()) : Fetcher {

    override suspend fun links(): List<Link> = resources.map { Link(href = it.key) }

    override fun get(link: Link): Resource =
        resources[link.href]?.let { StringResource(link, it) }
            ?: FailureResource(link, Resource.Exception.NotFound())

    override suspend fun close() {}
}
