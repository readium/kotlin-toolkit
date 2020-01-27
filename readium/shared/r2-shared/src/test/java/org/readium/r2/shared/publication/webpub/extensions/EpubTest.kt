package org.readium.r2.shared.publication.webpub.extensions

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.webpub.link.Properties

class EpubTest {

    // EpubLayout

    @Test fun `parse layout`() {
        assertEquals(EpubLayout.FIXED, EpubLayout.from("fixed"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.from("reflowable"))
        assertNull(EpubLayout.from("foobar"))
        assertNull(EpubLayout.from(null))
    }

    @Test fun `parse layout from EPUB rendition property`() {
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("reflowable"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("pre-paginated"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("foobar"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("foobar", fallback = EpubLayout.FIXED))
    }


    // EpubEncryption

    @Test fun `parse minimal JSON`() {
        assertEquals(
            EpubEncryption(algorithm = "http://algo"),
            EpubEncryption.fromJSON(JSONObject("{'algorithm': 'http://algo'}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            EpubEncryption(
                algorithm = "http://algo",
                compression = "gzip",
                originalLength = 42099,
                profile = "http://profile",
                scheme = "http://scheme"
            ),
            EpubEncryption.fromJSON(JSONObject("""{
                "algorithm": "http://algo",
                "compression": "gzip",
                "original-length": 42099,
                "profile": "http://profile",
                "scheme": "http://scheme"
            }"""))
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(EpubEncryption.fromJSON(null))
    }

    @Test fun `parse JSON requires {algorithm}`() {
        assertNull(EpubEncryption.fromJSON(JSONObject("{'compression': 'gzip'}")))
    }

    @Test fun `get minimal JSON`() {
        assertEquals(
            JSONObject("{'algorithm': 'http://algo'}").toString(),
            EpubEncryption(algorithm = "http://algo").toJSON().toString()
        )
    }

    @Test fun `get full JSON`() {
        assertEquals(
            JSONObject("""{
                "algorithm": "http://algo",
                "compression": "gzip",
                "original-length": 42099,
                "profile": "http://profile",
                "scheme": "http://scheme"
            }""").toString(),
            EpubEncryption(
                algorithm = "http://algo",
                compression = "gzip",
                originalLength = 42099,
                profile = "http://profile",
                scheme = "http://scheme"
            ).toJSON().toString()
        )
    }


    // EPUB extensions for link [Properties].

    @Test fun `get Properties {contains} when available`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} removes duplicates`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} when missing`() {
        assertEquals(emptySet<String>(), Properties().contains)
    }

    @Test fun `get Properties {contains} skips duplicates`() {
        assertEquals(
            setOf("mathml"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "mathml"))).contains
        )
    }

    @Test fun `get Properties {layout} when available`() {
        assertEquals(
            EpubLayout.FIXED,
            Properties(otherProperties = mapOf("layout" to "fixed")).layout
        )
    }

    @Test fun `get Properties {layout} when missing`() {
        assertNull(Properties().layout)
    }

    @Test fun `get Properties {mediaOverlay} when available`() {
        assertEquals(
            "http://media-overlay",
            Properties(otherProperties = mapOf("mediaOverlay" to "http://media-overlay")).mediaOverlay
        )
    }

    @Test fun `get Properties {mediaOverlay} when missing`() {
        assertNull(Properties().mediaOverlay)
    }

    @Test fun `get Properties {encryption} when available`() {
        assertEquals(
            EpubEncryption(algorithm = "http://algo", compression = "gzip"),
            Properties(otherProperties = mapOf("encrypted" to mapOf(
                "algorithm" to "http://algo",
                "compression" to "gzip"
            ))).encryption
        )
    }

    @Test fun `get Properties {encryption} when missing`() {
        assertNull(Properties().encryption)
    }

    @Test fun `get Properties {encryption} when not valid`() {
        assertNull(Properties(otherProperties = mapOf("encrypted" to "invalid")).encryption)
    }

}